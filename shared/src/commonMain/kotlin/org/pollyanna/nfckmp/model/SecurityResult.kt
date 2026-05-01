package org.pollyanna.nfckmp.model

import kotlin.io.encoding.Base64

data class SecurePayload(
    val encryptedData: ByteArray,
    val signature: ByteArray
) {
    fun clear() {
        encryptedData.fill(0)
        signature.fill(0)
    }

    fun toBase64Payload(): Map<String, String> {
        return mapOf(
            "encryptedData" to Base64.encode(encryptedData),
            "signature" to Base64.encode(signature)
        )
    }
}

sealed interface SecurityResult {
    data class Success(val payload: SecurePayload) : SecurityResult
    data class Failure(val error: SecurityError) : SecurityResult
}

sealed class SecurityError {
    data class EncryptionFailed(val cause: Throwable) : SecurityError()
    data class SignatureFailed(val cause: Throwable) : SecurityError()
}