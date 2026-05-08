package org.pollyanna.nfckmp.security

import org.pollyanna.nfckmp.model.SecurityResult

/**
 * Abstraction over the platform cryptography layer.
 *
 * Android implementation uses Android Keystore with hardware-backed RSA keys.
 * All sensitive key material is stored in the Secure Element and is never exported to
 * the application process.
 */
interface DeviceSecurityRepository {

    /**
     * Encrypts [rawData] with the backend's RSA public key and signs the ciphertext with the
     * device's hardware-backed private key.
     *
     * - Encryption: RSA-OAEP with SHA-256 / MGF1-SHA-256.
     * - Signing: SHA256withRSA via Android Keystore.
     *
     * The implementation zeros [publicKey] before returning, regardless of success or failure.
     *
     * @param rawData Raw card data bytes from the NFC reader. Callers must zero this array
     *   after the call returns.
     * @param publicKey DER-encoded X.509 SubjectPublicKeyInfo of the backend's RSA key.
     * @return [SecurityResult.Success] with a [org.pollyanna.nfckmp.model.SecurePayload], or
     *   [SecurityResult.Failure] describing the crypto error.
     */
    suspend fun encrypt(rawData: ByteArray, publicKey: ByteArray): SecurityResult

    /**
     * Generates (or retrieves) the device's hardware-backed RSA key pair and returns the
     * full attestation certificate chain.
     *
     * If the key pair does not yet exist it is created inside the Android Keystore bound to
     * [challenge], which lets the backend verify the key was generated for this specific
     * registration session.
     *
     * @param challenge Nonce received from [org.pollyanna.nfckmp.network.BackendService.getRegistrationChallenge].
     * @return Concatenated DER-encoded X.509 certs (leaf first), or `null` if key generation
     *   or attestation is not available on this device.
     */
    suspend fun getRegistrationCertificate(challenge: ByteArray): ByteArray?
}
