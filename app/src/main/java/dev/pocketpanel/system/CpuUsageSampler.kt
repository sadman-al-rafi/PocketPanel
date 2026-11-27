package dev.pocketpanel.system

import java.io.File

/**
 * Total CPU usage for Android 8 and later.
 *
 * `/proc/stat` is not readable by apps on API 26+, so counter deltas are
 * tried first and, when unavailable, an estimate is derived from the
 * kernel cpufreq sysfs interface. The estimate is labelled so the UI can
 * show that it is not a counter measurement. When neither source is
 * readable, [sample] returns null and the UI reports "Not available".
 */
class CpuUsageSampler {

    private var previousTotal = -1L
    private var previousIdle = -1L
    private var sysfsUnavailable = false
    private val minFrequencyByCore = mutableMapOf<String, Long>()
    private val maxFrequencyByCore = mutableMapOf<String, Long>()

    fun sample(): CpuUsage? {
        val counters = readProcStat()
        if (counters != null) {
            val previousTotalValue = previousTotal
            val previousIdleValue = previousIdle
            previousTotal = counters.first
            previousIdle = counters.second
            CpuUsageMath.fromCounterDeltas(
                totalDelta = counters.first - previousTotalValue,
                idleDelta = counters.second - previousIdleValue,
            )?.let { return CpuUsage(it, estimated = false) }
        }
        return estimateFromFrequencies()
    }

    private fun readProcStat(): Pair<Long, Long>? = try {
        File(PROC_STAT).useLines { lines ->
            lines.firstOrNull { it.startsWith("cpu ") }
                ?.let(CpuUsageMath::parseProcStatLine)
        }
    } catch (_: Exception) {
        null
    }

    private fun estimateFromFrequencies(): CpuUsage? {
        if (sysfsUnavailable) return null

        val cores = File(SYS_CPU).listFiles { file -> CORE_NAME.matches(file.name) }
        if (cores.isNullOrEmpty()) {
            sysfsUnavailable = true
            return null
        }

        val current = mutableListOf<Long>()
        val min = mutableListOf<Long>()
        val max = mutableListOf<Long>()
        for (core in cores.sortedBy { it.name }) {
            val key = core.absolutePath
            val currentFrequency = readFrequency(File(core, CURRENT_FREQUENCY))
            // Minimum and maximum rarely change, so read them once per core.
            val minFrequency = minFrequencyByCore[key]
                ?: readFrequency(File(core, MIN_FREQUENCY))?.also { minFrequencyByCore[key] = it }
            val maxFrequency = maxFrequencyByCore[key]
                ?: readFrequency(File(core, MAX_FREQUENCY))?.also { maxFrequencyByCore[key] = it }

            // An offline or restricted core may not expose these files.
            if (currentFrequency == null || minFrequency == null || maxFrequency == null) {
                continue
            }
            current.add(currentFrequency)
            min.add(minFrequency)
            max.add(maxFrequency)
        }

        if (current.isEmpty()) {
            // Avoid re-reading unreadable sysfs files on every tick.
            sysfsUnavailable = true
            return null
        }
        val percent = CpuUsageMath.fromFrequencies(current, min, max) ?: return null
        return CpuUsage(percent, estimated = true)
    }

    private fun readFrequency(file: File): Long? = try {
        file.readText().trim().toLongOrNull()?.takeIf { it > 0L }
    } catch (_: Exception) {
        null
    }

    private companion object {
        const val PROC_STAT = "/proc/stat"
        const val SYS_CPU = "/sys/devices/system/cpu"
        const val CURRENT_FREQUENCY = "cpufreq/scaling_cur_freq"
        const val MIN_FREQUENCY = "cpufreq/cpuinfo_min_freq"
        const val MAX_FREQUENCY = "cpufreq/cpuinfo_max_freq"
        val CORE_NAME = Regex("cpu[0-9]+")
    }
}
