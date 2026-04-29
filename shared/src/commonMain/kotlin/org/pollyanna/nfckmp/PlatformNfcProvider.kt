package org.pollyanna.nfckmp

expect class PlatformNfcProvider {
    suspend fun transceive(data: ByteArray): ByteArray
}