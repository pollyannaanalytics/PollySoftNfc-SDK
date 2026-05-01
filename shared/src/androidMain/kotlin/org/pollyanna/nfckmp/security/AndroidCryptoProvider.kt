package org.pollyanna.nfckmp.security

import android.content.Context
import android.util.Base64
import java.security.KeyFactory
import java.security.spec.MGF1ParameterSpec
import java.security.spec.X509EncodedKeySpec
import javax.crypto.Cipher
import javax.crypto.spec.OAEPParameterSpec
import javax.crypto.spec.PSource

class AndroidCryptoProvider(private val context: Context): CryptoProvider {
    override fun encrypt(rawData: ByteArray, backendPublicKey: ByteArray): ByteArray {
        val keySpec = X509EncodedKeySpec(backendPublicKey)
        val publicKey = KeyFactory.getInstance("RSA").generatePublic(keySpec)
        val cipher = Cipher.getInstance("RSA/ECB/OAEPWithSHA-256AndMGF1Padding")
        val oaepSpec = OAEPParameterSpec(
            "SHA-256", "MGF1", MGF1ParameterSpec.SHA256, PSource.PSpecified.DEFAULT
        )
        cipher.init(Cipher.ENCRYPT_MODE, publicKey, oaepSpec)
        val encryptedData = cipher.doFinal(rawData)
        return Base64.encode(encryptedData, Base64.NO_WRAP)
    }


}