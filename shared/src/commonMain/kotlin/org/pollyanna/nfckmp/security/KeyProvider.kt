package org.pollyanna.nfckmp.security

interface KeyProvider {
    fun getLocalKey(): String?
    fun getCertificateChain(): List<String>?

    fun generateRsaKeyPair(challenge: ByteArray?)

    fun exists(): Boolean
}