package org.pollyanna.nfckmp.network

import org.pollyanna.nfckmp.model.SecurePayload


interface BackendService {
    suspend fun getRegistrationChallenge(): ByteArray
    suspend fun registerDevice(certificateChain: ByteArray)
    suspend fun getPublicKey(): ByteArray
    suspend fun submitDeviceBinding(payload: SecurePayload)
}

