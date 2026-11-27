package dev.pocketpanel.elevated

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/** Parsing of real sysfs/proc values; no Android APIs are exercised. */
class ElevatedParsersTest {

    @Test
    fun percentBusyValuesAreAcceptedOnlyInRange() {
        assertEquals(0, ElevatedParsers.busyPercent("0", ElevatedParsers.BusyKind.PERCENT))
        assertEquals(42, ElevatedParsers.busyPercent(" 42 ", ElevatedParsers.BusyKind.PERCENT))
        assertEquals(100, ElevatedParsers.busyPercent("100", ElevatedParsers.BusyKind.PERCENT))
        assertNull(ElevatedParsers.busyPercent("-1", ElevatedParsers.BusyKind.PERCENT))
        assertNull(ElevatedParsers.busyPercent("101", ElevatedParsers.BusyKind.PERCENT))
        assertNull(ElevatedParsers.busyPercent("nope", ElevatedParsers.BusyKind.PERCENT))
    }

    @Test
    fun kgslBusyRatioIsConverted() {
        assertEquals(33, ElevatedParsers.busyPercent("43 128", ElevatedParsers.BusyKind.KGSL_RATIO))
        assertEquals(100, ElevatedParsers.busyPercent("10 10", ElevatedParsers.BusyKind.KGSL_RATIO))
        assertNull(ElevatedParsers.busyPercent("10 0", ElevatedParsers.BusyKind.KGSL_RATIO))
        assertNull(ElevatedParsers.busyPercent("20 10", ElevatedParsers.BusyKind.KGSL_RATIO))
        assertNull(ElevatedParsers.busyPercent("10", ElevatedParsers.BusyKind.KGSL_RATIO))
    }

    @Test
    fun millidegreeTemperaturesAreConvertedAndClamped() {
        assertEquals(45f, ElevatedParsers.thermalCelsius("45000"))
        assertEquals(0f, ElevatedParsers.thermalCelsius("0"))
        assertNull(ElevatedParsers.thermalCelsius("-1000"))
        assertNull(ElevatedParsers.thermalCelsius("200000"))
        assertNull(ElevatedParsers.thermalCelsius("garbage"))
        assertNull(ElevatedParsers.thermalCelsius(null))
    }

    @Test
    fun onlyPositiveClockValuesAreAccepted() {
        assertEquals(300_000L, ElevatedParsers.gpuClockKhz("300000"))
        assertNull(ElevatedParsers.gpuClockKhz("0"))
        assertNull(ElevatedParsers.gpuClockKhz("-1"))
        assertNull(ElevatedParsers.gpuClockKhz(""))
    }
}
