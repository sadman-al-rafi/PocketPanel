package dev.pocketpanel.system

import android.app.ActivityManager
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import android.os.Build
import androidx.core.content.ContextCompat

/**
 * Immutable snapshot of the device and the values the monitoring screen
 * can display.
 *
 * Every optional field is null when the platform does not expose the
 * value. A null field is shown as "Not available"; it is never replaced
 * with an estimate or a fabricated reading.
 */
data class DeviceInfo(
    val apiLevel: Int = Build.VERSION.SDK_INT,
    val model: String = Build.MODEL,
    val manufacturer: String = Build.MANUFACTURER,
    /** Total CPU usage in percent, or null when no source is readable. */
    val cpuUsagePercent: Int? = null,
    /** True when the percentage is estimated from core clock frequencies. */
    val cpuUsageIsEstimate: Boolean = false,
    /** Number of processors reported by the runtime, or null when unknown. */
    val cpuCount: Int? = null,
    /** Physical memory in bytes, or null when unavailable. */
    val totalMemoryBytes: Long? = null,
    /** Currently available memory in bytes, or null when unavailable. */
    val availableMemoryBytes: Long? = null,
    /** Battery charge in percent (0-100), or null when unavailable. */
    val batteryLevelPercent: Int? = null,
    /** Battery temperature in degrees Celsius, or null when unavailable. */
    val batteryTemperatureCelsius: Float? = null,
    /** Whether the battery is charging, or null when the status is unknown. */
    val isCharging: Boolean? = null,
    /** Raw [BatteryManager] health constant, or null when unavailable. */
    val batteryHealth: Int? = null,
) {
    companion object {

        /**
         * Reads a fresh snapshot.
         *
         * @param cpuUsage the latest sample from [CpuUsageSampler], or null
         *   when no CPU source is available on this device.
         */
        fun load(context: Context, cpuUsage: CpuUsage? = null): DeviceInfo {
            val appContext = context.applicationContext
            val memory = readMemory(appContext)
            val battery = readBattery(appContext)

            return DeviceInfo(
                apiLevel = Build.VERSION.SDK_INT,
                model = Build.MODEL,
                manufacturer = Build.MANUFACTURER,
                cpuUsagePercent = cpuUsage?.percent,
                cpuUsageIsEstimate = cpuUsage?.estimated ?: false,
                cpuCount = Runtime.getRuntime().availableProcessors().takeIf { it > 0 },
                totalMemoryBytes = memory.totalBytes,
                availableMemoryBytes = memory.availableBytes,
                batteryLevelPercent = battery.levelPercent,
                batteryTemperatureCelsius = battery.temperatureCelsius,
                isCharging = battery.isCharging,
                batteryHealth = battery.health,
            )
        }

        private fun readMemory(context: Context): MemorySnapshot {
            val activityManager =
                context.getSystemService(ActivityManager::class.java) ?: return MemorySnapshot()
            val memoryInfo = ActivityManager.MemoryInfo()
            return try {
                activityManager.getMemoryInfo(memoryInfo)
                MemorySnapshot(
                    totalBytes = memoryInfo.totalMem.takeIf { it > 0 },
                    availableBytes = memoryInfo.availMem.takeIf { it >= 0 },
                )
            } catch (_: RuntimeException) {
                MemorySnapshot()
            }
        }

        /**
         * Reads the sticky [Intent.ACTION_BATTERY_CHANGED] broadcast. This is
         * the documented way to obtain battery health, which
         * [BatteryManager] does not expose as a property.
         */
        private fun readBattery(context: Context): BatterySnapshot {
            val intent = try {
                ContextCompat.registerReceiver(
                    context,
                    null,
                    IntentFilter(Intent.ACTION_BATTERY_CHANGED),
                    ContextCompat.RECEIVER_NOT_EXPORTED,
                )
            } catch (_: RuntimeException) {
                null
            } ?: return BatterySnapshot()

            val level = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1)
            val scale = intent.getIntExtra(BatteryManager.EXTRA_SCALE, -1)
            val levelPercent = if (level >= 0 && scale > 0) {
                (level * 100 / scale).coerceIn(0, 100)
            } else {
                null
            }

            val temperatureTenths =
                intent.getIntExtra(BatteryManager.EXTRA_TEMPERATURE, Int.MIN_VALUE)
            val temperatureCelsius = if (temperatureTenths != Int.MIN_VALUE) {
                temperatureTenths / 10f
            } else {
                null
            }

            val status = intent.getIntExtra(
                BatteryManager.EXTRA_STATUS,
                BatteryManager.BATTERY_STATUS_UNKNOWN,
            )
            val isCharging = when (status) {
                BatteryManager.BATTERY_STATUS_CHARGING,
                BatteryManager.BATTERY_STATUS_FULL -> true

                BatteryManager.BATTERY_STATUS_DISCHARGING,
                BatteryManager.BATTERY_STATUS_NOT_CHARGING -> false

                else -> null
            }

            val health = intent.getIntExtra(
                BatteryManager.EXTRA_HEALTH,
                BatteryManager.BATTERY_HEALTH_UNKNOWN,
            )

            return BatterySnapshot(
                levelPercent = levelPercent,
                temperatureCelsius = temperatureCelsius,
                isCharging = isCharging,
                health = health,
            )
        }
    }
}

private data class MemorySnapshot(
    val totalBytes: Long? = null,
    val availableBytes: Long? = null,
)

private data class BatterySnapshot(
    val levelPercent: Int? = null,
    val temperatureCelsius: Float? = null,
    val isCharging: Boolean? = null,
    val health: Int? = null,
)
