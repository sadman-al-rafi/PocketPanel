package dev.pocketpanel.system

/** All M7 readings collected for one monitoring tick. */
data class HardwareSnapshot(
    val cpuUsage: CpuUsage? = null,
    val thermal: ThermalInfo = ThermalInfo(),
    val gpu: GpuInfo? = null,
)
