package org.pollyanna.nfckmp.security

interface CryptoProvider {
    fun encrypt(rawData: ByteArray, backendPublicKeyString: String): String
}