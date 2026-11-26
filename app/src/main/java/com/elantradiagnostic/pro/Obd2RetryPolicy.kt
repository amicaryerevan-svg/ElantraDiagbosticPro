package com.elantradiagnostic.pro

/** Validates whether the last user-selected Bluetooth adapter can be retried safely. */
internal object Obd2RetryPolicy {
    private val bluetoothAddressPattern =
        Regex("^[0-9A-F]{2}(:[0-9A-F]{2}){5}$")

    fun normalizeAddress(address: String?): String? {
        val normalized = address?.trim()?.uppercase(java.util.Locale.US) ?: return null
        return normalized.takeIf { bluetoothAddressPattern.matches(it) }
    }

    fun isAvailable(
        lastAddress: String?,
        bluetoothEnabled: Boolean,
        pairedAddresses: Collection<String>
    ): Boolean {
        if (!bluetoothEnabled) return false
        val target = normalizeAddress(lastAddress) ?: return false
        return pairedAddresses.any { normalizeAddress(it) == target }
    }
}
