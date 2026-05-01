package org.pollyanna.nfckmp.security

interface TransactionIdentifier {
    suspend fun getAttestationCertificate(challenge: ByteArray?): List<String>
    fun signTransaction(data: ByteArray): ByteArray
}