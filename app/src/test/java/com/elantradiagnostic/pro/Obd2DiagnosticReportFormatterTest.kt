package com.elantradiagnostic.pro

import org.junit.Assert.assertTrue
import org.junit.Test

class Obd2DiagnosticReportFormatterTest {

    @Test
    fun reportContainsVehicleAdapterTimestampAndOriginalSummary() {
        val entry = Obd2DiagnosticHistoryEntry(
            timestampMillis = 1_800_000_000_000L,
            adapterName = "ELM327 Bluetooth",
            summary = "ЭБУ ответил\nP0301 — обнаружены пропуски воспламенения."
        )

        val report = Obd2DiagnosticReportFormatter.format(entry)

        assertTrue(report.contains("Elantra Diagnostic Pro"))
        assertTrue(report.contains("Hyundai Elantra 2021 SEL 2.0L"))
        assertTrue(report.contains("Дата и время:"))
        assertTrue(report.contains("Bluetooth-адаптер: ELM327 Bluetooth"))
        assertTrue(report.contains(entry.summary))
        assertTrue(report.contains("непрерывная телеметрия"))
    }

    @Test
    fun suggestedFileNameUsesSafeTxtName() {
        val entry = Obd2DiagnosticHistoryEntry(
            timestampMillis = 1_800_000_000_000L,
            adapterName = "ELM327",
            summary = "OK"
        )

        assertTrue(
            Obd2DiagnosticReportFormatter.suggestedFileName(entry)
                .matches(Regex("Elantra_OBD2_\\d{8}_\\d{6}\\.txt"))
        )
    }
}
