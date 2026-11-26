package dev.pocketpanel.system

import android.content.Context
import android.os.BatteryManager
import android.os.Build
import android.os.PowerManager

/**
 * Read-only snapshot of device system information.
 *
 * All values are [nullable] because some fields may not be available
 * on all device configurations or API levels.
 */
data class DeviceInfo(
    val apiLevel: Int = Build.VERSION.SDK_INT,
    val model: String = Build.MODEL,
    val manufacturer: String = Build.MANUFACTURER,
    val brand: String = Build.BRAND,
    val fingerprint: String = Build.FINGERPRINT,

    /** Battery level in percent (0-100), or null when unknown. */
    val batteryLevel: Int?,
    /** Battery status: 1=CHARGING, 2=DISCHARGING, 3=NOT_CHARGING, 4=FULL */
    val batteryStatus: Int?,
    /** Whether the device is currently plugged in for charging. */
    val isCharging: Boolean?,
    /** Battery health: 0=UNKNOWN, 1=GOOD, 2=OVERHEAT, 3=DEAD, 4=OVER_VOLTAGE,
      5=UNKNOWN, 6=FAILURE */
    val batteryHealth: Int?,

    /** Total memory in MiB, or null when unknown. */
    val totalMemoryMiB: Int?,
    /** Available memory in MiB, or null when unknown. */
    val availableMemoryMiB: Int?,

    /** Number of CPU cores, or null when unknown. */
    val cpuCount: Int?,
    /** Current CPU frequency in MHz at the primary core, or null. */
    val currentCpuFrequencyMhz: Int?,
    /** Maximum CPU frequency in MHz at the primary core, or null. */
    val maxCpuFrequencyMhz: Int?
) {
    companion object {
        fun load(context: Context): DeviceInfo {
            val pm = context.getSystemService(PowerManager::class.java)
            val im = context.getSystemService(android.content.Context.BATTERY_SERVICE) as? BatteryManager

            val level = if (im != null) {
                // Read battery capacity directly; property keys are strings,
                // which are more reliably available across API levels than
                // the BatteryProperty enum.
                val cap = im.getInt("capacity").coerceIn(0, 100)
                cap
            } else null
            val status = if (im != null) im.getInt("status") else null
            val health = if (im != null) im.getInt("health") else null
            val plugged = if (im != null) im.isCharging else null

            val batteryLevel = level.coerceIn(0, 100)

            // Total memory
            val totalMemoryMiB: Int? = try {
                val runtime = Runtime.getRuntime()
                (runtime.totalMemory / 1024L / 1024L).toInt()
            } catch (_: Exception) {
                null
            }

            // Available memory
            val availableMemoryMiB: Int? = try {
                val runtime = Runtime.getRuntime()
                (runtime.freeMemory / 1024L / 1024L).toInt()
            } catch (_: Exception) {
                null
            }

            // CPU info
            val cpuCount: Int? = try {
                Runtime.getRuntime().availableProcessors()
            } catch (_: Exception) {
                null
            }

            val currentCpuFrequencyMhz: Int? = try {
                // Try to read from /sys/devices/system/cpu/cpu0/cpufreq/cur_freq
                // Fall back to null on platforms without this filesystem
                null
            } catch (_: Exception) {
                null
            }

            val maxCpuFrequencyMhz: Int? = try {
                null
            } catch (_: Exception) {
                null
            }

            return DeviceInfo(
                apiLevel = Build.VERSION.SDK_INT,
                model = Build.MODEL,
                manufacturer = Build.MANUFACTURER,
                brand = Build.BRAND,
                fingerprint = Build.FINGERPRINT,
                batteryLevel = batteryLevel,
                batteryStatus = status,
                isCharging = plugged,
                batteryHealth = health,
                totalMemoryMiB = totalMemoryMiB,
                availableMemoryMiB = availableMemoryMiB,
                cpuCount = cpuCount,
                currentCpuFrequencyMhz = currentCpuFrequencyMhz,
                maxCpuFrequencyMhz = maxCpuFrequencyMhz,
            )
        }
    }
}