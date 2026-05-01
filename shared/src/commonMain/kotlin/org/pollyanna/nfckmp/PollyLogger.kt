package org.pollyanna.nfckmp

interface PollyLogger {
    fun log(tag: String, message: String)

    companion object {
        val Default: PollyLogger = object : PollyLogger {
            override fun log(tag: String, message: String) {
                println("[Polly][$tag] $message")
            }
        }

        val Silent: PollyLogger = object : PollyLogger {
            override fun log(tag: String, message: String) = Unit
        }
    }
}
