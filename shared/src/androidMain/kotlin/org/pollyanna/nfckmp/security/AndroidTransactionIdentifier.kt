package org.pollyanna.nfckmp.security

import android.util.Base64
import java.security.PrivateKey
import java.security.Signature

class AndroidTransactionIdentifier(private val privateKeyDataSource: PrivateKeyDataSource): TransactionIdentifier {
    override suspend fun getAttestationCertificate(challenge: ByteArray?): ByteArray? {
        if (!privateKeyDataSource.exists()) {
            privateKeyDataSource.generateRsaKeyPair(challenge)
        }

        val chain = privateKeyDataSource.getCertificateChain()

        return chain?.fold(byteArrayOf()) { acc, cert ->
            acc + cert.toCertification().encoded
        }

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

    fun String.toCertification(): java.security.cert.X509Certificate {
        val factory = java.security.cert.CertificateFactory.getInstance("X.509")
        val decodedBytes = Base64.decode(this, Base64.NO_WRAP)

        return decodedBytes.inputStream().use {
            factory.generateCertificate(it) as java.security.cert.X509Certificate
        }
    }

    companion object {
        private const val ALGORITHM = "SHA256withRSA"
        private const val EXCEPTION_KEYSTORE_NOT_EXISTED = "Key not initialized. Call initialize() first."
        private const val EXCEPTION_NO_LOCAL_KEY = "Hardware-backed identity key not found or invalid."
        private const val EXCEPTION_TRANSACTION_ERROR = "Failed to sign transaction data due to a hardware execution error."
    }
}