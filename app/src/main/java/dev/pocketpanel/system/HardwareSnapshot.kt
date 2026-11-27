package dev.pocketpanel.system

/** Hardware readings collected for one monitoring tick. */
data class HardwareSnapshot(
    val cpuUsage: CpuUsage? = null,
    val gpu: GpuInfo? = null,
)
