package org.pollyanna.nfckmp.security

interface CryptoProvider {
    fun encrypt(rawData: ByteArray, backendPublicKey: ByteArray): ByteArray
}