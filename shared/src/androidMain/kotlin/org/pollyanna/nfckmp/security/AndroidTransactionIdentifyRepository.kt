package org.pollyanna.nfckmp.security

import android.util.Base64
import java.security.PrivateKey
import java.security.Signature

class AndroidTransactionIdentifyRepository(private val privateKeyDataSource: PrivateKeyDataSource): TransactionIdentifyRepository {
    override suspend fun getAttestationCertificate(challenge: ByteArray?): ByteArray {
        if (!privateKeyDataSource.exists()) {
            privateKeyDataSource.generateRsaKeyPair(challenge)
        }

        val attestation = privateKeyDataSource.getCertificateChain()

        return (listOf(attestation.leafCertificate) + attestation.intermediateCertificates)
            .fold(byteArrayOf()) { acc, bytes -> acc + bytes }
    }

    override fun signTransaction(data: ByteArray): ByteArray {
        if (!privateKeyDataSource.exists()) throw IllegalStateException(EXCEPTION_KEYSTORE_NOT_EXISTED)
        val privateKey = privateKeyDataSource.getLocalKey() as? PrivateKey
            ?: throw SecurityException(EXCEPTION_NO_LOCAL_KEY)
        return try {
            Signature.getInstance(ALGORITHM).run {
                initSign(privateKey)
                update(data)
                sign()
            }
        } catch (e: Exception) {
            throw SecurityException(EXCEPTION_TRANSACTION_ERROR, e)
        }
    }

    companion object {
        private const val ALGORITHM = "SHA256withRSA"
        private const val EXCEPTION_KEYSTORE_NOT_EXISTED = "Key not initialized. Call initialize() first."
        private const val EXCEPTION_NO_LOCAL_KEY = "Hardware-backed identity key not found or invalid."
        private const val EXCEPTION_TRANSACTION_ERROR = "Failed to sign transaction data due to a hardware execution error."
    }
}