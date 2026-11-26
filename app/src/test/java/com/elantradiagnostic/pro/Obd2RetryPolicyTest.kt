package com.elantradiagnostic.pro

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class Obd2RetryPolicyTest {

    private val address = "AA:BB:CC:DD:EE:FF"

    @Test
    fun allowsRetryForPreviouslySelectedPairedAdapter() {
        assertTrue(Obd2RetryPolicy.isAvailable(address, true, listOf(address)))
    }

    @Test
    fun matchesBluetoothAddressWithoutCaseSensitivity() {
        assertTrue(
            Obd2RetryPolicy.isAvailable(
                address.lowercase(),
                true,
                listOf(address.lowercase())
            )
        )
    }

    @Test
    fun rejectsRetryWhenBluetoothIsDisabled() {
        assertFalse(Obd2RetryPolicy.isAvailable(address, false, listOf(address)))
    }

    @Test
    fun rejectsAdapterThatIsNoLongerPaired() {
        assertFalse(
            Obd2RetryPolicy.isAvailable(
                address,
                true,
                listOf("11:22:33:44:55:66")
            )
        )
    }

    @Test
    fun rejectsMissingOrMalformedAddress() {
        assertFalse(Obd2RetryPolicy.isAvailable(null, true, listOf(address)))
        assertFalse(Obd2RetryPolicy.isAvailable("not-a-mac", true, listOf(address)))
    }
}
