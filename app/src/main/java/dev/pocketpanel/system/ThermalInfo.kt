package dev.pocketpanel.system

import android.content.Context
import android.os.Build
import android.os.PowerManager

/** Coarse platform thermal state and current headroom, both real API readings. */
data class ThermalInfo(
    val status: ThermalLevel? = null,
    /** Fraction of the thermal envelope in use; 1.0 means severe throttling. */
    val headroom: Float? = null,
)

/** Friendly mapping of the PowerManager thermal status constants. */
enum class ThermalLevel {
    NONE,
    LIGHT,
    MODERATE,
    SEVERE,
    CRITICAL,
    EMERGENCY,
    SHUTDOWN,
    UNKNOWN;

    companion object {
        fun from(status: Int): ThermalLevel = when (status) {
            PowerManager.THERMAL_STATUS_NONE -> NONE
            PowerManager.THERMAL_STATUS_LIGHT -> LIGHT
            PowerManager.THERMAL_STATUS_MODERATE -> MODERATE
            PowerManager.THERMAL_STATUS_SEVERE -> SEVERE
            PowerManager.THERMAL_STATUS_CRITICAL -> CRITICAL
            PowerManager.THERMAL_STATUS_EMERGENCY -> EMERGENCY
            PowerManager.THERMAL_STATUS_SHUTDOWN -> SHUTDOWN
            else -> UNKNOWN
        }
    }
}

/** Reads thermal status (API 29+) and headroom (API 30+) from PowerManager. */
class ThermalProvider(context: Context) {

    private val powerManager =
        context.applicationContext.getSystemService(PowerManager::class.java)
    private val statusAvailable = Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q
    private val headroomAvailable = Build.VERSION.SDK_INT >= Build.VERSION_CODES.R

    fun snapshot(): ThermalInfo {
        val status = if (statusAvailable && powerManager != null) {
            try {
                ThermalLevel.from(powerManager.currentThermalStatus)
            } catch (_: Exception) {
                null
            }
        } else {
            null
        }

        // A forecast of zero reads the current value. The platform warns
        // that more frequent calls than about once per second are useless.
        val headroom = if (headroomAvailable && powerManager != null) {
            try {
                powerManager.getThermalHeadroom(0)
                    .takeIf { value -> !value.isNaN() && value >= 0f }
            } catch (_: Exception) {
                null
            }
        } else {
            null
        }

        return ThermalInfo(status = status, headroom = headroom)
    }
}
