package org.pollyanna.nfckmp.security


interface AttestationCheckProvider {
    /**
     * Perform local environment checks (Root/Debug).
     * Should throw a specific SecurityException if a hard violation is found.
     */
    fun checkLocalSecurity(): Boolean

    /**
     * Request a hardware-backed assertion object from the OS.
     * Android: Google Play Integrity Token
     * iOS: App Attest Key Assertion / DeviceCheck Token
     */
    suspend fun fetchHardwareAssertion(): String
}