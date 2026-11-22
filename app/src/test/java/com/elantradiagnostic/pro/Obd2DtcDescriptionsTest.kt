package com.elantradiagnostic.pro

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class Obd2DtcDescriptionsTest {

    @Test
    fun describesCommonGenericElantraRelevantCodes() {
        assertEquals(
            "Обнаружены пропуски воспламенения в цилиндре 1.",
            Obd2DtcDescriptions.descriptionFor("P0301")
        )
        assertEquals(
            "Система слишком бедная, Bank 1.",
            Obd2DtcDescriptions.descriptionFor("P0171")
        )
        assertEquals(
            "Эффективность каталитической системы ниже порогового значения, Bank 1.",
            Obd2DtcDescriptions.descriptionFor("P0420")
        )
    }

    @Test
    fun normalizesWhitespaceAndCase() {
        assertEquals(
            Obd2DtcDescriptions.descriptionFor("P0455"),
            Obd2DtcDescriptions.descriptionFor(" p0455 ")
        )
    }

    @Test
    fun leavesUnknownAndManufacturerSpecificCodesUnresolved() {
        assertNull(Obd2DtcDescriptions.descriptionFor("P1000"))
        assertNull(Obd2DtcDescriptions.descriptionFor("P9999"))
        assertNull(Obd2DtcDescriptions.descriptionFor("U0100"))
        assertNull(Obd2DtcDescriptions.descriptionFor("C0035"))
        assertNull(Obd2DtcDescriptions.descriptionFor("P030"))
    }
}
