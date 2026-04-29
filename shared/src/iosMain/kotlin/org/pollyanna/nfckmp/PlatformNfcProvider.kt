package org.pollyanna.nfckmp

actual class PlatformNfcProvider {
    actual suspend fun transceive(data: ByteArray): ByteArray {
        // todo
        return byteArrayOf()
    }
}