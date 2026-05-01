package org.pollyanna.nfckmp.security

interface CryptoDataSource {
    fun encrypt(rawData: ByteArray, backendPublicKey: ByteArray): ByteArray
}