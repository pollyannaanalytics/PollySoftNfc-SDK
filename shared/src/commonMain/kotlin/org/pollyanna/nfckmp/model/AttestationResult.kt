package org.pollyanna.nfckmp.model

import kotlin.io.encoding.Base64

data class AttestationResult(
    val leafCertificate: ByteArray,
    val intermediateCertificates: List<ByteArray>
) {
    fun toBase64List(): List<String> {
        return (listOf(leafCertificate) + intermediateCertificates).map { Base64.encode(it) }
    }
}
