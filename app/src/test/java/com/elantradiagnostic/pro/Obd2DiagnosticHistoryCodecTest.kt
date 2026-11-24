package com.elantradiagnostic.pro

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class Obd2DiagnosticHistoryCodecTest {

    @Test
    fun roundTripsNewlinesSeparatorsAndUnicode() {
        val entry = Obd2DiagnosticHistoryEntry(
            timestampMillis = 1_800_000_000_000L,
            adapterName = "ELM | Bluetooth\n327",
            summary = "ЭБУ ответил\nP0301 | сохранён",
            status = Obd2DiagnosticStatus.INCOMPLETE_RESPONSE
        )

        assertEquals(listOf(entry), Obd2DiagnosticHistoryCodec.decode(
            Obd2DiagnosticHistoryCodec.encode(listOf(entry))
        ))
    }

    @Test
    fun readsLegacyThreeFieldRecordsAsCompleted() {
        val legacy = "123|ELM327|ЭБУ+ответил"
        assertEquals(
            listOf(Obd2DiagnosticHistoryEntry(123L, "ELM327", "ЭБУ ответил")),
            Obd2DiagnosticHistoryCodec.decode(legacy)
        )
    }

    @Test
    fun skipsRecordsWithUnknownStatus() {
        val decoded = Obd2DiagnosticHistoryCodec.decode("123|ELM327|Ошибка|FUTURE_STATUS")
        assertTrue(decoded.isEmpty())
    }

    @Test
    fun keepsOnlyTheMostRecentTwentyEntries() {
        // The app prepends each new session, so entries are ordered newest first.
        val entries = (25L downTo 1L).map {
            Obd2DiagnosticHistoryEntry(it, "ELM327", "Сеанс $it")
        }

        val decoded = Obd2DiagnosticHistoryCodec.decode(Obd2DiagnosticHistoryCodec.encode(entries))

        assertEquals(Obd2DiagnosticHistoryCodec.MAX_ENTRIES, decoded.size)
        assertEquals(25L, decoded.first().timestampMillis)
        assertEquals(6L, decoded.last().timestampMillis)
    }

    @Test
    fun skipsMalformedRecordsWithoutLosingValidOnes() {
        val valid = Obd2DiagnosticHistoryCodec.encode(
            listOf(Obd2DiagnosticHistoryEntry(123L, "ELM327", "OK"))
        )

        val decoded = Obd2DiagnosticHistoryCodec.decode(
            "not-a-record\n0|bad|timestamp\n$valid\n124|bad%ZZ|record"
        )

        assertEquals(listOf(Obd2DiagnosticHistoryEntry(123L, "ELM327", "OK")), decoded)
    }

    @Test
    fun returnsEmptyListForBlankOrOversizedStorage() {
        assertTrue(Obd2DiagnosticHistoryCodec.decode("  ").isEmpty())
        assertTrue(Obd2DiagnosticHistoryCodec.decode("x".repeat(500_001)).isEmpty())
    }
}
