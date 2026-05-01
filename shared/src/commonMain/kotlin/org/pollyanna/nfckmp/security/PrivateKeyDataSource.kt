package org.pollyanna.nfckmp.security

interface PrivateKeyDataSource {
    fun getLocalKey(): Any?
    fun getCertificateChain(): List<String>?
    fun generateRsaKeyPair(challenge: ByteArray?)
    fun exists(): Boolean
}