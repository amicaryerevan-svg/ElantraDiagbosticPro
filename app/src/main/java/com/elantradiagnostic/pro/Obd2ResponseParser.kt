package com.elantradiagnostic.pro

/**
 * Decodes the standard OBD-II Mode 01 PID 00 support bitmap.
 *
 * The response payload is 41 00 followed by four bytes. Each set bit announces
 * support for one PID in the range 01..20 (hex), in network bit order.
 */
internal object Obd2ResponseParser {

    private val pidBitmapPattern = Regex("4100([0-9A-F]{8})")

    /**
     * Returns supported decimal PID numbers (1..32), or null if no valid 41 00
     * response with a complete four-byte bitmap is present.
     */
    fun supportedPids(response: String): Set<Int>? {
        val compact = response.uppercase().filter { it in '0'..'9' || it in 'A'..'F' }
        val bitmap = pidBitmapPattern.find(compact)?.groupValues?.get(1) ?: return null
        val bytes = IntArray(4) { index ->
            bitmap.substring(index * 2, index * 2 + 2).toInt(16)
        }

        return (1..32).filterTo(linkedSetOf()) { pid ->
            val byteIndex = (pid - 1) / 8
            val bitIndex = 7 - ((pid - 1) % 8)
            (bytes[byteIndex] and (1 shl bitIndex)) != 0
        }
    }

    fun describeSupportedPids(supportedPids: Set<Int>): String {
        val pidList = if (supportedPids.isEmpty()) {
            "не перечислены"
        } else {
            supportedPids.sorted().joinToString(", ") {
                it.toString(16).uppercase().padStart(2, '0')
            }
        }
        val rpm = if (12 in supportedPids) "поддерживается" else "не заявлен"
        val coolant = if (5 in supportedPids) "поддерживается" else "не заявлен"

        return "Поддерживаемые PID 01–20: $pidList\n" +
            "Обороты двигателя (010C): $rpm\n" +
            "Температура охлаждающей жидкости (0105): $coolant"
    }
}
