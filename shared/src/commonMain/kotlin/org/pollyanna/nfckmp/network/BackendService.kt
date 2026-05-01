package org.pollyanna.nfckmp.network

import org.pollyanna.nfckmp.model.SecurePayload


interface BackendService {
    suspend fun getRegistrationChallenge(): ByteArray
    suspend fun submitDeviceBinding(payload: SecurePayload)
    suspend fun getPublicKey(): ByteArray
}

