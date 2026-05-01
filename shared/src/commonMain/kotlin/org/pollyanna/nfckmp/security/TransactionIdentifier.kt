package org.pollyanna.nfckmp.security

interface TransactionIdentifier {
    suspend fun getAttestationCertificate(challenge: ByteArray?): ByteArray?
    fun signTransaction(data: ByteArray): ByteArray
}