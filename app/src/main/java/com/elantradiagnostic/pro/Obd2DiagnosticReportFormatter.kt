package com.elantradiagnostic.pro

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Produces a portable plain-text copy of a locally stored OBD-II snapshot.
 * The report is a user-facing record, not a workshop diagnosis or a live ECU connection.
 */
internal object Obd2DiagnosticReportFormatter {

    fun format(entry: Obd2DiagnosticHistoryEntry): String {
        val date = SimpleDateFormat("dd.MM.yyyy HH:mm:ss", Locale.getDefault())
            .format(Date(entry.timestampMillis))

        return buildString {
            appendLine("Elantra Diagnostic Pro — отчёт OBD-II")
            appendLine("Автомобиль: Hyundai Elantra 2021 SEL 2.0L")
            appendLine("Дата и время: $date")
            appendLine("Bluetooth-адаптер: ${entry.adapterName}")
            appendLine("Статус попытки: ${entry.status.displayLabel}")
            appendLine()
            appendLine(entry.summary.trim())
            appendLine()
            appendLine("Примечание: это сохранённый однократный снимок данных OBD-II, а не непрерывная телеметрия или официальное заключение сервисного центра.")
        }
    }

    fun suggestedFileName(entry: Obd2DiagnosticHistoryEntry): String {
        val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US)
            .format(Date(entry.timestampMillis))
        return "Elantra_OBD2_$timestamp.txt"
    }
}
