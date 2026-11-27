package com.elantradiagnostic.pro

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class Elm327AdapterInfoParserTest {

    @Test
    fun prefersAtiIdentifierAndKeepsReportedProtocol() {
        val details = Elm327AdapterInfoParser.parse(
            resetBanner = "ELM327 v1.5",
            atiResponse = "ELM327 v2.1",
            protocolResponse = "AUTO, ISO 15765-4 (CAN 11/500)"
        )

        assertEquals("ELM327 v2.1", details.reportedIdentifier)
        assertEquals("ATI", details.identifierSource)
        assertEquals("AUTO, ISO 15765-4 (CAN 11/500)", details.protocolDescription)
    }

    @Test
    fun fallsBackToResetBannerWhenAtiIsUnsupported() {
        val details = Elm327AdapterInfoParser.parse(
            resetBanner = "ELM327 v1.5",
            atiResponse = "?",
            protocolResponse = "?"
        )

        assertEquals("ELM327 v1.5", details.reportedIdentifier)
        assertEquals("ATZ", details.identifierSource)
        assertNull(details.protocolDescription)
    }

    @Test
    fun ignoresCommandEchoesAndErrorLines() {
        val details = Elm327AdapterInfoParser.parse(
            resetBanner = "ATZ\r\nELM327 v1.4",
            atiResponse = "ATI\r\nERROR",
            protocolResponse = "ATDP\r\nNO DATA"
        )

        assertEquals("ELM327 v1.4", details.reportedIdentifier)
        assertNull(details.protocolDescription)
    }

    @Test
    fun formatsMissingValuesWithoutClaimingAuthenticity() {
        val summary = Elm327AdapterInfoParser.format(null, "?", null)

        assertTrue(summary.contains("не сообщён адаптером"))
        assertTrue(summary.contains("не подтверждают подлинность"))
        assertFalse(summary.contains("оригинальный ELM327"))
    }
}
