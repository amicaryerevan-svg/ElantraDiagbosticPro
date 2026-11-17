package com.elantradiagnostic.pro

/**
 * Decodes selected SAE J1979 Mode 01 live-data responses.
 *
 * The parser accepts compact or whitespace-separated ELM327 output and ignores
 * unrelated adapter text. It never infers a value from an incomplete response.
 */
internal object Obd2LiveDataParser {

    private val rpmPattern = Regex(
        "(?i)(?<![0-9A-F])41\\s*0C\\s*([0-9A-F]{2})\\s*([0-9A-F]{2})(?![0-9A-F])"
    )
    private val coolantPattern = Regex(
        "(?i)(?<![0-9A-F])41\\s*05\\s*([0-9A-F]{2})(?![0-9A-F])"
    )

    /** Returns engine speed in rpm, including quarter-rpm resolution, or null. */
    fun engineRpm(response: String): Double? {
        val match = rpmPattern.find(response) ?: return null
        val a = match.groupValues[1].toInt(16)
        val b = match.groupValues[2].toInt(16)
        return (a * 256 + b) / 4.0
    }

    /** Returns engine coolant temperature in °C, or null for an invalid response. */
    fun coolantTemperatureC(response: String): Int? {
        val match = coolantPattern.find(response) ?: return null
        return match.groupValues[1].toInt(16) - 40
    }
}
