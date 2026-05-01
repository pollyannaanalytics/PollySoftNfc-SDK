package org.pollyanna.nfckmp.security

import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import org.pollyanna.nfckmp.model.AttestationResult
import java.security.KeyPairGenerator
import java.security.KeyStore
import java.security.PrivateKey

class AndroidPrivateKeyDataSource(private val alias: String): PrivateKeyDataSource {
    private val provider = "AndroidKeyStore"
    private val keyStore = KeyStore.getInstance(provider).apply { load(null) }

    override fun getLocalKey(): PrivateKey? = keyStore.getKey(alias, null) as? PrivateKey

    override fun getCertificateChain(): AttestationResult {
        val chain = keyStore.getCertificateChain(alias)

        val bytesChain = chain.map { it.encoded }
        return AttestationResult(
            leafCertificate = bytesChain.first(),
            intermediateCertificates = bytesChain.drop(1)
        )
    }

    override fun generateRsaKeyPair(challenge: ByteArray?) {
        val kpg = KeyPairGenerator.getInstance(KeyProperties.KEY_ALGORITHM_RSA, provider)
        val builder = KeyGenParameterSpec.Builder(
            alias,
            KeyProperties.PURPOSE_SIGN or KeyProperties.PURPOSE_VERIFY
        )
            .setDigests(KeyProperties.DIGEST_SHA256)
            .setSignaturePaddings(KeyProperties.SIGNATURE_PADDING_RSA_PKCS1)

        challenge?.let { builder.setAttestationChallenge(it) }

        kpg.initialize(builder.build())
        kpg.generateKeyPair()
    }

    override fun exists(): Boolean = keyStore.containsAlias(alias)
}