package dev.pocketpanel.system

/**
 * Static GPU identity read from the OpenGL ES driver.
 *
 * This is identity information only. Android has no public API for GPU
 * utilization or frames per second, so neither is reported.
 */
data class GpuInfo(
    val renderer: String?,
    val vendor: String?,
    val glVersion: String?,
)
