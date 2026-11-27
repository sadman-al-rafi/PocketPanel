package dev.pocketpanel.system

/**
 * Converts two cumulative byte counters into a bytes-per-second rate.
 *
 * Returns null when either counter is unsupported (negative), when the
 * elapsed time is not positive, or when the counters moved backwards.
 * A null rate is displayed as "Not available"; it is never estimated.
 */
internal fun bytesPerSecond(
    previous: Long,
    current: Long,
    elapsedMillis: Long,
): Long? {
    if (previous < 0L || current < 0L || elapsedMillis <= 0L) return null
    val delta = current - previous
    if (delta < 0L) return null
    return delta * 1000L / elapsedMillis
}
