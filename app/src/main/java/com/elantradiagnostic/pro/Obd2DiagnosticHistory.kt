package com.elantradiagnostic.pro

import java.net.URLDecoder
import java.net.URLEncoder

internal data class Obd2DiagnosticHistoryEntry(
    val timestampMillis: Long,
    val adapterName: String,
    val summary: String
)

/**
 * Small, versioned-by-key text codec for the last few local diagnostic reports.
 * Each field is URL-encoded so newlines and separators in adapter output stay safe.
 */
internal object Obd2DiagnosticHistoryCodec {
    const val MAX_ENTRIES = 20

    private const val MAX_ADAPTER_NAME_LENGTH = 120
    private const val MAX_SUMMARY_LENGTH = 1_600
    private const val MAX_LINE_LENGTH = 22_000
    private const val MAX_STORAGE_LENGTH = 500_000

    fun encode(entries: List<Obd2DiagnosticHistoryEntry>): String =
        entries.take(MAX_ENTRIES).joinToString("\n") { entry ->
            listOf(
                entry.timestampMillis.coerceAtLeast(0L).toString(),
                encodeField(entry.adapterName.take(MAX_ADAPTER_NAME_LENGTH)),
                encodeField(entry.summary.take(MAX_SUMMARY_LENGTH))
            ).joinToString("|")
        }

    fun decode(raw: String): List<Obd2DiagnosticHistoryEntry> {
        if (raw.isBlank() || raw.length > MAX_STORAGE_LENGTH) return emptyList()

        return raw.lineSequence()
            .take(MAX_ENTRIES)
            .mapNotNull { line ->
                if (line.isBlank() || line.length > MAX_LINE_LENGTH) return@mapNotNull null
                val fields = line.split('|', limit = 3)
                if (fields.size != 3) return@mapNotNull null

                val timestamp = fields[0].toLongOrNull()
                    ?.takeIf { it > 0L }
                    ?: return@mapNotNull null
                val adapterName = decodeField(fields[1])
                    ?: return@mapNotNull null
                val summary = decodeField(fields[2])
                    ?: return@mapNotNull null

                Obd2DiagnosticHistoryEntry(timestamp, adapterName, summary)
            }
            .toList()
    }

    private fun encodeField(value: String): String = URLEncoder.encode(value, "UTF-8")

    private fun decodeField(value: String): String? =
        try {
            URLDecoder.decode(value, "UTF-8")
        } catch (_: IllegalArgumentException) {
            null
        }
}
