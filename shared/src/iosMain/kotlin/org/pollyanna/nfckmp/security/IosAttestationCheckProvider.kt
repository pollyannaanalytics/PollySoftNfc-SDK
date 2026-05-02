package org.pollyanna.nfckmp.security

class IosAttestationCheckProvider : AttestationCheckProvider {
    override fun checkLocalSecurity(): Boolean = true

    // TODO: implement via App Attest / DeviceCheck
    override suspend fun fetchHardwareAssertion(): String = ""
}
