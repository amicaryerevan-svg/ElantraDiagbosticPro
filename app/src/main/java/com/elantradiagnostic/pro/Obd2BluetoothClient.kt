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
 * It performs the ELM initialization sequence and the read-only 0100 supported-PIDs
 * query. It does not clear trouble codes or send actuator/control commands.
 */
internal class Obd2BluetoothClient : AutoCloseable {

    interface Listener {
        fun onConnected(deviceName: String, adapterInfo: String, ecuResponse: String, livePidResponses: Map<Int, String>, dtcResponse: String)
        fun onFailure(message: String)
    }

    private val executor = Executors.newSingleThreadExecutor()
    @Volatile private var socket: BluetoothSocket? = null
    @Volatile private var closed = false

    fun connect(device: BluetoothDevice, listener: Listener) {
        executor.execute {
            try {
                val connectedSocket = device.createRfcommSocketToServiceRecord(SPP_UUID)
                socket = connectedSocket
                connectedSocket.connect()

                val input = connectedSocket.inputStream
                val output = connectedSocket.outputStream

                val adapterInfo = exchange(input, output, "ATZ", 6_000L)
                if (adapterInfo.isBlank()) {
                    throw IOException("ELM327 не ответил на команду сброса ATZ.")
                }

                val setupCommands = listOf("ATE0", "ATL0", "ATS0", "ATH0", "ATSP0")
                for (command in setupCommands) {
                    val response = exchange(input, output, command, 3_000L)
                    if (response.contains("?") || response.contains("ERROR", ignoreCase = true)) {
                        throw IOException("Адаптер отклонил команду $command: $response")
                    }
                }

                // Mode 01 / PID 00 reads which standard live-data PIDs are supported.
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
                        livePidResponses[pid] = try {
                            exchange(input, output, command, 4_000L)
                        } catch (error: IOException) {
                            error.message ?: "Нет ответа"
                        }
                    }
                }

                // Mode 03 is read-only and returns stored emission-related powertrain DTCs.
                // Run it only after the ECU has answered the supported-PID handshake.
                val dtcResponse = if (supportedPids != null) {
                    try {
                        exchange(input, output, "03", 8_000L)
                    } catch (error: IOException) {
                        error.message ?: "Нет ответа"
                    }
                } else {
                    "Не запрашивался: корректный ответ PID 0100 не получен."
                }

                if (!closed) {
                    listener.onConnected(
                        device.name?.takeIf { it.isNotBlank() } ?: device.address,
                        adapterInfo,
                        ecuResponse,
                        livePidResponses,
                        dtcResponse
                    )
                }
            } catch (error: SecurityException) {
                closeSocket()
                if (!closed) {
                    listener.onFailure("Нет разрешения Android Nearby devices (Bluetooth Connect).")
                }
            } catch (error: Exception) {
                closeSocket()
                if (!closed) {
                    listener.onFailure(
                        error.message?.takeIf { it.isNotBlank() }
                            ?: "Не удалось подключиться к Bluetooth OBD2 адаптеру."
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

        val response = StringBuilder()
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

                for (index in 0 until count) {
                    val character = buffer[index].toInt().toChar()
                    if (character == '>') {
                        return response.toString().trim()
                    }
                    response.append(character)
                }
            } else {
                Thread.sleep(25L)
            }
        }

        val result = response.toString().trim()
        if (result.isEmpty()) {
            throw IOException("Нет ответа от адаптера на команду $command.")
        }
        return result
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
