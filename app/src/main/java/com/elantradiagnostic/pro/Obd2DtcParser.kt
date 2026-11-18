package com.elantradiagnostic.pro

/**
 * Decodes SAE J1979 Mode 03 positive responses into standard five-character DTCs.
 *
 * The parser accepts compact or whitespace-separated ELM327 output, including
 * optional CAN headers. It returns null for missing or incomplete responses and
 * an empty list for a valid response that contains no stored DTCs.
 */
internal object Obd2DtcParser {

    private val responseMarker = Regex("(?i)(?<![0-9A-F])43")

    fun troubleCodes(response: String): List<String>? {
        val responseLines = response.lineSequence()
            .filter { responseMarker.containsMatchIn(it) }
            .toList()
        if (responseLines.isEmpty()) return null

        val codes = linkedSetOf<String>()
        for (line in responseLines) {
            val marker = responseMarker.find(line) ?: continue
            val payload = buildString {
                for (character in line.substring(marker.range.last + 1)) {
                    if (character.isWhitespace()) continue
                    if (character.digitToIntOrNull(16) != null) {
                        append(character.uppercaseChar())
                    } else {
                        break
                    }
                }
            }

            // Each DTC is exactly two data bytes (four hexadecimal characters).
            // Reject odd or truncated payloads rather than inventing a partial code.
            if (payload.isEmpty() || payload.length % 4 != 0) return null

            for (offset in payload.indices step 4) {
                val firstByte = payload.substring(offset, offset + 2).toInt(16)
                val secondByte = payload.substring(offset + 2, offset + 4).toInt(16)

                // 00 00 is the standard unused/no-more-DTC pair.
                if (firstByte == 0 && secondByte == 0) break
                codes += decodeCode(firstByte, secondByte)
            }
        }
        return codes.toList()
    }

    private fun decodeCode(firstByte: Int, secondByte: Int): String {
        val system = when (firstByte ushr 6) {
            0 -> 'P'
            1 -> 'C'
            2 -> 'B'
            else -> 'U'
        }
        val subsystem = (firstByte ushr 4) and 0x03
        return buildString {
            append(system)
            append(subsystem)
            appendHexDigit(firstByte and 0x0F)
            appendHexDigit(secondByte ushr 4)
            appendHexDigit(secondByte and 0x0F)
        }
    }

    private fun StringBuilder.appendHexDigit(value: Int) {
        append(value.toString(16).uppercase())
    }
}
