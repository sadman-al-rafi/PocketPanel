package dev.pocketpanel.system

import java.io.IOException
import java.util.concurrent.TimeUnit
import kotlin.math.roundToInt

/**
 * Measures ICMP round-trip latency with the platform `ping` command.
 *
 * The command is not an SDK contract and is blocked on some devices or
 * ROMs. Any failure returns null, and the UI shows "Not available";
 * no latency is estimated.
 */
internal class PingProbe(private val host: String = PING_HOST) {

    fun pingOnce(timeoutMillis: Int = DEFAULT_TIMEOUT_MILLIS): Int? {
        val timeoutSeconds = ((timeoutMillis + 999) / 1000).coerceAtLeast(1).toString()
        val primary = listOf("/system/bin/ping", "-c", "1", "-W", timeoutSeconds, host)
        val fallback = listOf("ping", "-c", "1", "-W", timeoutSeconds, host)
        return try {
            runPing(primary, timeoutMillis)
        } catch (_: IOException) {
            // The absolute path could not be executed; try PATH once.
            try {
                runPing(fallback, timeoutMillis)
            } catch (_: IOException) {
                null
            }
        }
    }

    private fun runPing(command: List<String>, timeoutMillis: Int): Int? {
        var process: Process? = null
        return try {
            process = ProcessBuilder(command)
                .redirectErrorStream(true)
                .start()
            if (!process.waitFor(timeoutMillis.toLong(), TimeUnit.MILLISECONDS)) {
                process.destroyForcibly()
                return null
            }
            val output = process.inputStream.bufferedReader().use { it.readText() }
            if (process.exitValue() != 0) return null
            parsePingLatency(output)
        } catch (_: InterruptedException) {
            Thread.currentThread().interrupt()
            null
        } finally {
            process?.destroy()
        }
    }

    private companion object {
        const val DEFAULT_TIMEOUT_MILLIS = 2_000
    }
}

/**
 * Parses the round-trip time from `ping` output.
 *
 * Handles both `time=23.4 ms` and `time<1 ms`. Returns null when the
 * output contains no latency.
 */
internal fun parsePingLatency(output: String): Int? {
    val match = PING_TIME_PATTERN.find(output) ?: return null
    return match.groupValues[1].toFloatOrNull()?.roundToInt()
}

private val PING_TIME_PATTERN = Regex("""time[=<]\s*([0-9]+(?:\.[0-9]+)?)\s*ms""")
