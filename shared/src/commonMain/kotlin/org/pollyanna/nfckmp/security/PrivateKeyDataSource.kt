package org.pollyanna.nfckmp.security

import org.pollyanna.nfckmp.model.AttestationResult

interface PrivateKeyDataSource {
    fun getLocalKey(): Any?
    fun getCertificateChain(): AttestationResult
    fun generateRsaKeyPair(challenge: ByteArray?)
    fun exists(): Boolean
}