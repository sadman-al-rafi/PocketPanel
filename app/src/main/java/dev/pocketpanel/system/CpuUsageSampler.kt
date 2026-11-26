package dev.pocketpanel.system

import java.io.File

/**
 * Best-effort total CPU usage from `/proc/stat`.
 *
 * `/proc/stat` is not an SDK contract, and access is denied on some
 * devices. When the file cannot be read, or the counters are unusable,
 * [sample] returns null and the UI reports the value as unavailable.
 * The first call only establishes a baseline and also returns null.
 */
class CpuUsageSampler {

    private var previousTotal: Long = -1L
    private var previousIdle: Long = -1L

    fun sample(): Int? {
        val counters = readCounters() ?: return null
        val previousTotalValue = previousTotal
        val previousIdleValue = previousIdle
        previousTotal = counters.total
        previousIdle = counters.idle

        if (previousTotalValue < 0L || previousIdleValue < 0L) return null

        val totalDelta = counters.total - previousTotalValue
        val idleDelta = counters.idle - previousIdleValue
        if (totalDelta <= 0L || idleDelta < 0L || idleDelta > totalDelta) return null

        val busyDelta = totalDelta - idleDelta
        return (busyDelta * 100L / totalDelta).toInt().coerceIn(0, 100)
    }

    private fun readCounters(): Counters? {
        val line = try {
            File(PROC_STAT).useLines { lines ->
                lines.firstOrNull { it.startsWith("cpu ") }
            }
        } catch (_: Exception) {
            null
        } ?: return null

        val parts = line.trim().split(WHITESPACE)
        if (parts.size < 5) return null

        val values = parts.drop(1).mapNotNull { it.toLongOrNull() }
        // user, nice, system, idle are the first four fields. iowait is
        // optional; guest time is already included in user time.
        if (values.size < 4) return null

        val idle = values[3] + (values.getOrNull(4) ?: 0L)
        val total = values.sum()
        return Counters(total = total, idle = idle)
    }

    private data class Counters(val total: Long, val idle: Long)

    private companion object {
        const val PROC_STAT = "/proc/stat"
        val WHITESPACE = Regex("\\s+")
    }
}
