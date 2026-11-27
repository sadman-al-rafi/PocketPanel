package dev.pocketpanel.elevated

import dev.pocketpanel.system.CpuUsage
import kotlinx.coroutines.flow.StateFlow

/** Real temperature from a kernel thermal zone. */
data class SocTemperature(
    val celsius: Float,
    val zoneType: String,
)

/** Readings that are only available with optional elevated access. */
data class ElevatedSnapshot(
    val cpuUsage: CpuUsage? = null,
    val socTemperature: SocTemperature? = null,
    val gpuBusyPercent: Int? = null,
    val gpuClockKhz: Long? = null,
)

/**
 * Boundary between monitoring/UI and the optional elevated provider.
 *
 * The standard implementation is simply "no snapshot"; M8 adds the
 * Shizuku implementation behind the same surface.
 */
interface ElevatedHardwareAccess {
    val status: StateFlow<ShizukuStatus>
    fun initialize()
    fun requestPermission()
    fun uid(): Int?
    suspend fun snapshot(): ElevatedSnapshot?
    fun close()
}
