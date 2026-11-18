package com.elantradiagnostic.pro

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class Obd2DtcParserTest {

    @Test
    fun decodesSingleCodeFromSpacedResponse() {
        assertEquals(listOf("P0133"), Obd2DtcParser.troubleCodes("03\r43 01 33\r>"))
    }

    @Test
    fun decodesCompactResponseAndStopsAtZeroPair() {
        assertEquals(
            listOf("P0133"),
            Obd2DtcParser.troubleCodes("4301330000\r>")
        )
    }

    @Test
    fun decodesMultipleCodesInOneResponse() {
        assertEquals(
            listOf("P0133", "P0210"),
            Obd2DtcParser.troubleCodes("43 01 33 02 10")
        )
    }

    @Test
    fun decodesPowertrainChassisBodyAndNetworkPrefixes() {
        assertEquals(
            listOf("P0001", "C0001", "B0002", "U0003"),
            Obd2DtcParser.troubleCodes("43 00 01 40 01 80 02 C0 03")
        )
    }

    @Test
    fun acceptsOptionalCanHeaderAndMultipleEcuLines() {
        assertEquals(
            listOf("P0133", "P0210"),
            Obd2DtcParser.troubleCodes("7E8 03 43 01 33\r7E9 03 43 02 10")
        )
    }

    @Test
    fun returnsEmptyListWhenNoStoredCodesAreReported() {
        assertEquals(emptyList<String>(), Obd2DtcParser.troubleCodes("43 00 00"))
    }

    @Test
    fun rejectsMissingAndIncompleteResponses() {
        assertNull(Obd2DtcParser.troubleCodes("NO DATA"))
        assertNull(Obd2DtcParser.troubleCodes(""))
        assertNull(Obd2DtcParser.troubleCodes("43"))
        assertNull(Obd2DtcParser.troubleCodes("43 01"))
        assertNull(Obd2DtcParser.troubleCodes("43 01 33 02"))
        assertNull(Obd2DtcParser.troubleCodes("CAN ERROR"))
    }
}
