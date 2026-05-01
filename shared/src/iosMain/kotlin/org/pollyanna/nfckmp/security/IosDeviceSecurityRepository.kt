package org.pollyanna.nfckmp.security

import org.pollyanna.nfckmp.model.SecurityError
import org.pollyanna.nfckmp.model.SecurityResult

class IosDeviceSecurityRepository : DeviceSecurityRepository {

    override suspend fun getRegistrationCertificate(challenge: ByteArray): ByteArray? {
        // TODO: implement via DeviceCheck / App Attest
        return null
    }

    override suspend fun encrypt(rawData: ByteArray, publicKey: ByteArray): SecurityResult {
        // TODO: implement via iOS Security framework
        return SecurityResult.Failure(
            SecurityError.EncryptionFailed(NotImplementedError("iOS encryption not yet implemented"))
        )
    }
}
