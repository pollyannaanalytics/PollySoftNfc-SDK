package org.pollyanna.nfckmp.security

import org.pollyanna.nfckmp.model.SecurityResult

interface DeviceSecurityRepository {
    suspend fun encrypt(rawData: ByteArray, publicKey: ByteArray): SecurityResult
    suspend fun getRegistrationCertificate(challenge: ByteArray): ByteArray?
}