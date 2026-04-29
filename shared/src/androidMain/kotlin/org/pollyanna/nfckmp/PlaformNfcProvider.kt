package org.pollyanna.nfckmp

actual class PlatformNfcProvider(private val isoDep: android.nfc.tech.IsoDep) {
    actual suspend fun transceive(data: ByteArray): ByteArray {
        if (!isoDep.isConnected) isoDep.connect()
        return isoDep.transceive(data)
    }
}