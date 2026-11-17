package com.elantradiagnostic.pro

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class Obd2LiveDataParserTest {

    @Test
    fun decodesEngineRpmFromSpacedResponse() {
        assertEquals(1726.0, Obd2LiveDataParser.engineRpm("41 0C 1A F8")!!, 0.0)
    }

    @Test
    fun decodesQuarterRpmFromCompactElmResponse() {
        assertEquals(0.25, Obd2LiveDataParser.engineRpm("410C0001\r>")!!, 0.0)
    }

    @Test
    fun decodesCoolantTemperature() {
        assertEquals(60, Obd2LiveDataParser.coolantTemperatureC("0105\r41 05 64\r>"))
        assertEquals(-40, Obd2LiveDataParser.coolantTemperatureC("410500"))
    }

    @Test
    fun acceptsCanHeaderAndIgnoresAdapterText() {
        assertEquals(
            1726.0,
            Obd2LiveDataParser.engineRpm("SEARCHING...\r7E8 04 41 0C 1A F8\r>")!!,
            0.0
        )
    }

    @Test
    fun rejectsNoDataAndIncompleteResponses() {
        assertNull(Obd2LiveDataParser.engineRpm("NO DATA"))
        assertNull(Obd2LiveDataParser.engineRpm("41 0C 1A"))
        assertNull(Obd2LiveDataParser.coolantTemperatureC("41 05"))
        assertNull(Obd2LiveDataParser.coolantTemperatureC("ERROR"))
    }
}
