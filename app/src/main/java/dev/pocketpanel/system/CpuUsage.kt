package dev.pocketpanel.system

/**
 * A total CPU usage sample.
 *
 * [estimated] is true when the value is derived from core clock
 * frequencies instead of CPU counter deltas. The UI must label such a
 * value as an estimate.
 */
data class CpuUsage(
    val percent: Int,
    val estimated: Boolean,
)
