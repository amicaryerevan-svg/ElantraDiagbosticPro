package com.elantradiagnostic.pro

/**
 * Collects one ELM327 reply until the adapter's '>' prompt is received.
 *
 * The prompt is the command boundary. A timeout without it must not be treated as a
 * complete reply because late bytes could otherwise be attributed to the next command.
 */
internal class Elm327ResponseAccumulator {

    private val buffer = StringBuilder()
    var isComplete: Boolean = false
        private set

    fun append(bytes: ByteArray, count: Int): Boolean {
        require(count in 0..bytes.size) { "Invalid byte count." }
        if (isComplete) return true

        for (index in 0 until count) {
            val character = bytes[index].toInt().toChar()
            if (character == '>') {
                isComplete = true
                break
            }
            check(buffer.length < MAX_RESPONSE_CHARS) {
                "Ответ ELM327 превышает допустимый размер."
            }
            buffer.append(character)
        }
        return isComplete
    }

    fun content(): String = buffer.toString().trim()

    private companion object {
        const val MAX_RESPONSE_CHARS = 8_192
    }
}
