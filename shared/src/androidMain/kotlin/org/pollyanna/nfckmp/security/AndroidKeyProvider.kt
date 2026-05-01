package org.pollyanna.nfckmp.security

import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import java.security.KeyPairGenerator
import java.security.KeyStore

class AndroidKeyProvider(private val alias: String): KeyProvider {
    private val provider = "AndroidKeyStore"
    private val keyStore = KeyStore.getInstance(provider).apply { load(null) }


    override fun getLocalKey(): String? {
        return keyStore.getKey(alias, null)?.toString()
    }

    override fun getCertificateChain(): List<String>? {
        return keyStore.getCertificateChain(alias)?.toList()?.map { it.toString() }
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