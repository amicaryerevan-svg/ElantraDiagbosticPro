package com.elantradiagnostic.pro

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class Obd2ResponseParserTest {

    @Test
    fun parsesCompactPidBitmapAndHighlightsUsefulPids() {
        val supported = Obd2ResponseParser.supportedPids("4100BE3EA813")

        assertNotNull(supported)
        assertTrue(5 in supported!!)
        assertTrue(12 in supported)
        assertFalse(2 in supported)
        assertTrue(Obd2ResponseParser.describeSupportedPids(supported).contains("010C: поддерживается"))
    }

    @Test
    fun parsesSpacedElmResponseAndEmptyBitmap() {
        val supported = Obd2ResponseParser.supportedPids("\r\n41 00 00 00 00 00\r\n>")

        assertNotNull(supported)
        assertTrue(supported!!.isEmpty())
    }

    @Test
    fun rejectsNoDataAndIncompleteBitmap() {
        assertNull(Obd2ResponseParser.supportedPids("NO DATA"))
        assertNull(Obd2ResponseParser.supportedPids("41 00 BE 3E"))
    }

    @Test
    fun recognizesAllThirtyTwoPidsWhenEveryBitIsSet() {
        assertEquals((1..32).toSet(), Obd2ResponseParser.supportedPids("4100FFFFFFFF"))
    }
}
