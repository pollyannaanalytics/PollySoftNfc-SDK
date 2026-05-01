package org.pollyanna.nfckmp.security

import org.pollyanna.nfckmp.model.SecurePayload
import org.pollyanna.nfckmp.model.SecurityError
import org.pollyanna.nfckmp.model.SecurityResult

class AndroidDeviceSecurityRepository(
    private val cryptoDataSource: CryptoDataSource,
    private val transactionIdentifyRepository: TransactionIdentifyRepository
) : DeviceSecurityRepository {

    override suspend fun encrypt(
        rawData: ByteArray,
        publicKey: ByteArray
    ): SecurityResult {
        var encryptedData: ByteArray? = null

        return try {
            encryptedData = cryptoDataSource.encrypt(rawData, publicKey)
            val signature = transactionIdentifyRepository.signTransaction(encryptedData)
            SecurityResult.Success(
                SecurePayload(
                    encryptedData = encryptedData,
                    signature = signature
                )
            )
        } catch (e: Exception) {
            encryptedData?.fill(0)
            val error = when (e) {
                is SecurityException -> SecurityError.SignatureFailed(e)
                else -> SecurityError.EncryptionFailed(e)
            }
            SecurityResult.Failure(error)
        } finally {
            publicKey.fill(0.toByte())
        }
    }

    override suspend fun getRegistrationCertificate(challenge: ByteArray): ByteArray? {
        return transactionIdentifyRepository.getAttestationCertificate(challenge)
    }
}