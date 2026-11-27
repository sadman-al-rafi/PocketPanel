package dev.pocketpanel.system

import android.os.PowerManager
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/** Mapping of the documented PowerManager thermal status constants. */
class ThermalInfoTest {

    @Test
    fun everyPlatformStatusHasALabel() {
        assertEquals(ThermalLevel.NONE, ThermalLevel.from(PowerManager.THERMAL_STATUS_NONE))
        assertEquals(ThermalLevel.LIGHT, ThermalLevel.from(PowerManager.THERMAL_STATUS_LIGHT))
        assertEquals(
            ThermalLevel.MODERATE,
            ThermalLevel.from(PowerManager.THERMAL_STATUS_MODERATE),
        )
        assertEquals(ThermalLevel.SEVERE, ThermalLevel.from(PowerManager.THERMAL_STATUS_SEVERE))
        assertEquals(
            ThermalLevel.CRITICAL,
            ThermalLevel.from(PowerManager.THERMAL_STATUS_CRITICAL),
        )
        assertEquals(
            ThermalLevel.EMERGENCY,
            ThermalLevel.from(PowerManager.THERMAL_STATUS_EMERGENCY),
        )
        assertEquals(
            ThermalLevel.SHUTDOWN,
            ThermalLevel.from(PowerManager.THERMAL_STATUS_SHUTDOWN),
        )
    }

    @Test
    fun unknownStatusDoesNotBecomeAReading() {
        assertEquals(ThermalLevel.UNKNOWN, ThermalLevel.from(-1))
        assertEquals(ThermalLevel.UNKNOWN, ThermalLevel.from(99))
    }

    @Test
    fun emptyThermalInfoHasNoReadings() {
        val info = ThermalInfo()
        assertNull(info.status)
        assertNull(info.headroom)
    }
}
