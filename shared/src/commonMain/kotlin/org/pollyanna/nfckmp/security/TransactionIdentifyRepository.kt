package org.pollyanna.nfckmp.security

interface TransactionIdentifyRepository {
    suspend fun getAttestationCertificate(challenge: ByteArray?): ByteArray?
    fun signTransaction(data: ByteArray): ByteArray
}