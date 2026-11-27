package com.elantradiagnostic.pro

import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothSocket
import android.os.SystemClock
import java.io.EOFException
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import java.nio.charset.StandardCharsets
import java.util.UUID
import java.util.concurrent.Executors

/**
 * Minimal read-only ELM327 client for already-paired Bluetooth Classic SPP adapters.
 *
 * It performs the ELM initialization sequence, reads supported standard PIDs, takes
 * one-time RPM/coolant snapshots, and retrieves stored Mode 03 DTCs. It does not
 * clear trouble codes or send actuator/control commands.
 */
internal class Obd2BluetoothClient : AutoCloseable {

    interface Listener {
        fun onConnected(deviceName: String, adapterInfo: String, ecuResponse: String, livePidResponses: Map<Int, String>, dtcResponse: String)
        fun onFailure(status: Obd2DiagnosticStatus, message: String)
    }

    private val executor = Executors.newSingleThreadExecutor()
    @Volatile private var socket: BluetoothSocket? = null
    @Volatile private var closed = false

    fun connect(device: BluetoothDevice, listener: Listener) {
        executor.execute {
            var stage = Obd2OperationStage.CONNECTION
            try {
                val connectedSocket = device.createRfcommSocketToServiceRecord(SPP_UUID)
                socket = connectedSocket
                connectedSocket.connect()
                stage = Obd2OperationStage.ADAPTER_INITIALIZATION

                val input = connectedSocket.inputStream
                val output = connectedSocket.outputStream

                val resetBanner = exchange(input, output, "ATZ", 6_000L)
                if (resetBanner.isBlank()) {
                    throw Elm327NoResponseException("ATZ")
                }

                val setupCommands = listOf("ATE0", "ATL0", "ATS0", "ATH0", "ATSP0")
                for (command in setupCommands) {
                    val response = exchange(input, output, command, 3_000L)
                    if (response.contains("?") || response.contains("ERROR", ignoreCase = true)) {
                        throw IOException("Адаптер отклонил команду $command: $response")
                    }
                }

                // Mode 01 / PID 00 reads which standard live-data PIDs are supported.
                stage = Obd2OperationStage.ECU_REQUEST
                val ecuResponse = exchange(input, output, "0100", 12_000L)
                val livePidResponses = linkedMapOf<Int, String>()
                val supportedPids = Obd2ResponseParser.supportedPids(ecuResponse)

                // Read only two standard, non-actuating Mode 01 PIDs, and only when
                // the ECU explicitly reports support for them. This is a one-time
                // snapshot, not a polling loop or a driver-facing live display.
                if (supportedPids != null) {
                    for (pid in listOf(0x0C, 0x05)) {
                        if (pid !in supportedPids) continue
                        val command = "01" + pid.toString(16).uppercase().padStart(2, '0')
                        // A command must finish with the ELM327 prompt. If it times out,
                        // abort the transaction rather than risk assigning late bytes to
                        // the next PID response.
                        livePidResponses[pid] = exchange(input, output, command, 4_000L)
                    }
                }

                // Mode 03 is read-only and returns stored emission-related powertrain DTCs.
                // Run it only after the ECU has answered the supported-PID handshake.
                val dtcResponse = if (supportedPids != null) {
                    // Keep the command stream synchronized: an incomplete reply aborts
                    // the connection instead of allowing stale bytes into another query.
                    exchange(input, output, "03", 8_000L)
                } else {
                    "Не запрашивался: корректный ответ PID 0100 не получен."
                }

                // Adapter metadata is optional and collected only after all required
                // diagnostic requests. If a clone does not answer, the completed scan
                // remains valid; no further command is sent after an incomplete reply.
                val atiResponse = exchangeOptionalMetadata(input, output, "ATI", 2_000L)
                val protocolResponse = if (atiResponse != null) {
                    exchangeOptionalMetadata(input, output, "ATDP", 2_000L)
                } else {
                    null
                }
                val adapterInfo = Elm327AdapterInfoParser.format(
                    resetBanner,
                    atiResponse,
                    protocolResponse
                )

                if (!closed) {
                    val resultDeviceName =
                        device.name?.takeIf { it.isNotBlank() } ?: device.address

                    // This client currently performs a one-time diagnostic snapshot, not
                    // continuous telemetry. Release the adapter before reporting the result
                    // so dismissing the dialog cannot leave the RFCOMM channel occupied.
                    closeSocket()
                    executor.shutdown()

                    if (!closed) {
                        listener.onConnected(
                            resultDeviceName,
                            adapterInfo,
                            ecuResponse,
                            livePidResponses,
                            dtcResponse
                        )
                    }
                }
            } catch (error: Exception) {
                closeSocket()
                // A failed attempt is terminal too. Do not leave the executor's worker
                // alive and idle after the Activity drops its reference to this client.
                executor.shutdown()
                if (!closed) {
                    val fallback = when (stage) {
                        Obd2OperationStage.CONNECTION -> "Не удалось подключиться к Bluetooth OBD2 адаптеру."
                        Obd2OperationStage.ADAPTER_INITIALIZATION -> "Не удалось выполнить инициализацию ELM327."
                        Obd2OperationStage.ECU_REQUEST -> "Не удалось получить корректный ответ ЭБУ."
                    }
                    listener.onFailure(
                        Obd2FailureClassifier.classify(stage, error),
                        error.message?.takeIf { it.isNotBlank() } ?: fallback
                    )
                }
            }
        }
    }

    private fun exchange(
        input: InputStream,
        output: OutputStream,
        command: String,
        timeoutMs: Long
    ): String {
        // Discard any bytes left by a previous command before sending the next one.
        while (input.available() > 0) {
            input.read()
        }

        output.write((command + "\r").toByteArray(StandardCharsets.US_ASCII))
        output.flush()

        val response = Elm327ResponseAccumulator()
        val deadline = SystemClock.elapsedRealtime() + timeoutMs

        while (SystemClock.elapsedRealtime() < deadline) {
            if (closed || Thread.currentThread().isInterrupted) {
                throw IOException("Подключение отменено.")
            }

            val available = input.available()
            if (available > 0) {
                val buffer = ByteArray(minOf(available, 256))
                val count = input.read(buffer)
                if (count < 0) throw EOFException("OBD2 адаптер закрыл соединение.")
                if (response.append(buffer, count)) {
                    return response.content()
                }
            } else {
                Thread.sleep(25L)
            }
        }

        val partial = response.content()
        if (partial.isEmpty()) {
            throw Elm327NoResponseException(command)
        }
        throw Elm327IncompleteResponseException(command, partial)
    }

    /**
     * Reads optional adapter metadata after the required OBD transaction. A timeout or
     * unsupported command only omits metadata; it must not turn a completed scan into
     * a failed scan. No further command is sent after an incomplete optional response.
     */
    private fun exchangeOptionalMetadata(
        input: InputStream,
        output: OutputStream,
        command: String,
        timeoutMs: Long
    ): String? {
        return try {
            exchange(input, output, command, timeoutMs)
        } catch (error: IOException) {
            if (closed || Thread.currentThread().isInterrupted) throw error
            null
        }
    }

    private fun closeSocket() {
        try {
            socket?.close()
        } catch (_: Exception) {
            // The socket may already be closed after a failed connection.
        } finally {
            socket = null
        }
    }

    override fun close() {
        closed = true
        closeSocket()
        executor.shutdownNow()
    }

    companion object {
        private val SPP_UUID: UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB")
    }
}
