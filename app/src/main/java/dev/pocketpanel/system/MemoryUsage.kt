package dev.pocketpanel.system

/** Derives used-memory percentage from ActivityManager snapshots. */
object MemoryUsage {

    /** Returns the used fraction in percent, or null when unusable. */
    fun usedPercent(totalBytes: Long?, availableBytes: Long?): Int? {
        if (totalBytes == null || availableBytes == null || totalBytes <= 0L) {
            return null
        }
        val used = totalBytes - availableBytes
        if (used < 0L || used > totalBytes) return null
        return (used * 100L / totalBytes).toInt().coerceIn(0, 100)
    }
}
