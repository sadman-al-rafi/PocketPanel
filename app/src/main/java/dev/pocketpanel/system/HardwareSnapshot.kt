package dev.pocketpanel.system

import dev.pocketpanel.elevated.SocTemperature

/** All readings collected for one monitoring tick. */
data class HardwareSnapshot(
    val cpuUsage: CpuUsage? = null,
    val thermal: ThermalInfo = ThermalInfo(),
    val gpu: GpuInfo? = null,
    /** Elevated readings, present only when Shizuku access is granted. */
    val socTemperature: SocTemperature? = null,
    val gpuBusyPercent: Int? = null,
    val gpuClockKhz: Long? = null,
)
