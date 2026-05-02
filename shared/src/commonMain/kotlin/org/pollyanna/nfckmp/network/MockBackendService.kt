package org.pollyanna.nfckmp.network

import kotlinx.coroutines.delay
import org.pollyanna.nfckmp.model.SecurePayload

class MockBackendService : BackendService {

    private var accessToken: String? = null

    override suspend fun getRegistrationChallenge(): ByteArray {
        delay(300)
        refreshTokenIfNeeded()
        return "mock-registration-challenge".encodeToByteArray()
    }

    override suspend fun registerDevice(certificateChain: ByteArray) {
        delay(400)
        refreshTokenIfNeeded()
        println("[MockBackendService] Device registered with token=$accessToken")
        println("[MockBackendService]   certificate size=${certificateChain.size} bytes")
    }

    override suspend fun getPublicKey(): ByteArray {
        delay(300)
        refreshTokenIfNeeded()
        return MOCK_PUBLIC_KEY.encodeToByteArray()
    }

    override suspend fun submitDeviceBinding(payload: SecurePayload, integrityToken: String) {
        delay(500)
        refreshTokenIfNeeded()
        println("[MockBackendService] Device binding submitted with token=$accessToken")
        println("[MockBackendService]   encryptedData size=${payload.encryptedData.size} bytes")
        println("[MockBackendService]   signature size=${payload.signature.size} bytes")
        println("[MockBackendService]   integrityToken=${integrityToken.take(20)}...")
    }

    private fun refreshTokenIfNeeded() {
        if (accessToken == null) {
            accessToken = produceFakeAccessToken()
            println("[MockBackendService] Access token issued: $accessToken")
        }
    }

    private fun produceFakeAccessToken(): String {
        val header = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9"
        val payload = "eyJzdWIiOiJtb2NrLXRlcm1pbmFsIiwiaWF0IjoxNjAwMDAwMDAwfQ"
        val signature = "mock-sig"
        return "$header.$payload.$signature"
    }

    private companion object {
        const val MOCK_PUBLIC_KEY = "mock-rsa-public-key-bytes"
    }
}
