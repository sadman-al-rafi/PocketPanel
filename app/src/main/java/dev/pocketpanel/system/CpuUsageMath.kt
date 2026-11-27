package dev.pocketpanel.system

/** Pure CPU calculations, separated from file access so they can be tested on the JVM. */
object CpuUsageMath {

    /** Busy percentage from two cumulative counter samples, or null when unusable. */
    fun fromCounterDeltas(totalDelta: Long, idleDelta: Long): Int? {
        if (totalDelta <= 0L || idleDelta < 0L || idleDelta > totalDelta) return null
        return ((totalDelta - idleDelta) * 100L / totalDelta).toInt().coerceIn(0, 100)
    }

    /**
     * Approximate usage from per-core clock frequencies in kHz.
     *
     * This is an estimate, not a CPU counter measurement: a core running
     * near its maximum frequency is assumed to be busy. It is only used
     * when `/proc/stat` is unavailable.
     */
    fun fromFrequencies(current: List<Long>, min: List<Long>, max: List<Long>): Int? {
        if (current.isEmpty() || current.size != min.size || current.size != max.size) {
            return null
        }
        var currentSum = 0L
        var minSum = 0L
        var maxSum = 0L
        for (index in current.indices) {
            val currentFrequency = current[index]
            val minimum = min[index]
            val maximum = max[index]
            if (currentFrequency < 0L || minimum < 0L || maximum <= minimum) return null
            currentSum += currentFrequency
            minSum += minimum
            maxSum += maximum
        }
        if (maxSum <= minSum) return null
        return ((currentSum - minSum) * 100L / (maxSum - minSum))
            .coerceIn(0L, 100L)
            .toInt()
    }

    /** Extracts total and idle counters from the aggregate `cpu` line of /proc/stat. */
    fun parseProcStatLine(line: String): Pair<Long, Long>? {
        if (!line.startsWith("cpu ")) return null
        val values = line.trim().split(WHITESPACE)
            .drop(1)
            .map { it.toLongOrNull() ?: return null }
        if (values.size < 4) return null
        // guest and guest_nice are already included in user and nice.
        val total = values.take(8).sum()
        val idle = values[3] + (values.getOrNull(4) ?: 0L)
        return total to idle
    }

    private val WHITESPACE = Regex("\\s+")
}
