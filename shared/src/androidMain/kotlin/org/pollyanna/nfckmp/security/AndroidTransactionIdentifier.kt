package org.pollyanna.nfckmp.security

class AndroidTransactionIdentityChecker: TransactionIdentityChecker {
    override suspend fun getAttestationCertificate(): List<String> {
        TODO("Not yet implemented")
    }

    override fun signTransaction(data: ByteArray): ByteArray {
        TODO("Not yet implemented")
    }
}