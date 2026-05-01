package org.pollyanna.nfckmp.security

interface TransactionIdentityChecker {
    suspend fun getAttestationCertificate(): List<String>
    fun signTransaction(data: ByteArray): ByteArray
}