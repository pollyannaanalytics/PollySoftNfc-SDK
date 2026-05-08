package org.pollyanna.nfckmp

/**
 * Logging sink for the SDK's internal diagnostic output.
 *
 * Pass an instance to [PollyNfc.createEngine]. Use [Default] during development and
 * [Silent] in production (or route to your own logger).
 */
interface PollyLogger {
    fun log(tag: String, message: String)

    companion object {
        /** Prints to stdout as `[Polly][<tag>] <message>`. */
        val Default: PollyLogger = object : PollyLogger {
            override fun log(tag: String, message: String) {
                println("[Polly][$tag] $message")
            }
        }

        /** Discards all log output. Use in production builds. */
        val Silent: PollyLogger = object : PollyLogger {
            override fun log(tag: String, message: String) = Unit
        }
    }
}
