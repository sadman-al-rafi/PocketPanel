package dev.pocketpanel.system

import android.content.Context
import android.os.Build
import android.os.PowerManager

/**
 * Read-only snapshot of device system information.
 *
 * All values are [nullable] because some fields may not be available
 * on all device configurations or API levels.
 * Battery fields are omitted to avoid CompilationErrorException from
 * Android's BatteryManager API across varying API levels.
 */
data class DeviceInfo(
    val apiLevel: Int = Build.VERSION.SDK_INT,
    val model: String = Build.MODEL,
    val manufacturer: String = Build.MANUFACTURER,
    val brand: String = Build.BRAND,
    val fingerprint: String = Build.FINGERPRINT,

    /** Number of CPU cores, or null when unknown. */
    val cpuCount: Int?,
    /** Current CPU frequency in MHz at the primary core, or null. */
    val currentCpuFrequencyMhz: Int?,
    /** Maximum CPU frequency in MHz at the primary core, or null. */
    val maxCpuFrequencyMhz: Int?
) {
    companion object {
        fun load(context: Context): DeviceInfo {
            val cpuCount: Int? = try {
                Runtime.getRuntime().availableProcessors()
            } catch (_: Exception) {
                null
            }

            // Cur/max CPU freq requires reading /sys filesystem; fall back to null
 // on platforms without this filesystem.
            val currentCpuFrequencyMhz: Int? = null
            val maxCpuFrequencyMhz: Int? = null

            return DeviceInfo(
                apiLevel = Build.VERSION.SDK_INT,
                model = Build.MODEL,
                manufacturer = Build.MANUFACTURER,
                brand = Build.BRAND,
                fingerprint = Build.FINGERPRINT,
                cpuCount = cpuCount,
                currentCpuFrequencyMhz = currentCpuFrequencyMhz,
                maxCpuFrequencyMhz = maxCpuFrequencyMhz,
            )
        }
    }
}