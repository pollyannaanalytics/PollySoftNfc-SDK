package org.pollyanna.nfckmp.security

import android.content.Context
import android.util.Base64
import java.security.KeyFactory
import java.security.PublicKey
import java.security.spec.MGF1ParameterSpec
import java.security.spec.X509EncodedKeySpec
import javax.crypto.Cipher
import javax.crypto.spec.OAEPParameterSpec
import javax.crypto.spec.PSource

class AndroidCryptoProvider(private val context: Context): CryptoProvider {
    override fun encrypt(rawData: ByteArray, backendPublicKeyString: String): String {
        val backendPublicKey = backendPublicKeyString.toPublicKey()
        val cipher = Cipher.getInstance("RSA/ECB/OAEPWithSHA-256AndMGF1Padding")
        val oaepSpec = OAEPParameterSpec(
            "SHA-256", "MGF1", MGF1ParameterSpec.SHA256, PSource.PSpecified.DEFAULT
        )
        cipher.init(Cipher.ENCRYPT_MODE, backendPublicKey, oaepSpec)
        val encryptedData = cipher.doFinal(rawData)
        return Base64.encodeToString(encryptedData, Base64.NO_WRAP)
    }

    private fun String.toPublicKey(): PublicKey {
        val keyBytes = Base64.decode(this, Base64.DEFAULT)
        val spec = X509EncodedKeySpec(keyBytes)
        val keyFactory = KeyFactory.getInstance("RSA")
        return keyFactory.generatePublic(spec)
    }


}