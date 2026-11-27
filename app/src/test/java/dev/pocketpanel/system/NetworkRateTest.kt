package dev.pocketpanel.system

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class NetworkRateTest {

    @Test
    fun computesRateFromCounterDelta() {
        assertEquals(
            1500L,
            bytesPerSecond(previous = 1_000L, current = 4_000L, elapsedMillis = 2_000L),
        )
    }

    @Test
    fun zeroDeltaIsZeroRate() {
        assertEquals(
            0L,
            bytesPerSecond(previous = 500L, current = 500L, elapsedMillis = 1_000L),
        )
    }

    @Test
    fun unsupportedCountersReturnNull() {
        assertNull(bytesPerSecond(previous = -1L, current = 100L, elapsedMillis = 1_000L))
        assertNull(bytesPerSecond(previous = 100L, current = -1L, elapsedMillis = 1_000L))
    }

    @Test
    fun countersMovingBackwardsReturnNull() {
        assertNull(bytesPerSecond(previous = 2_000L, current = 1_000L, elapsedMillis = 1_000L))
    }

    @Test
    fun nonPositiveElapsedTimeReturnsNull() {
        assertNull(bytesPerSecond(previous = 0L, current = 1_000L, elapsedMillis = 0L))
        assertNull(bytesPerSecond(previous = 0L, current = 1_000L, elapsedMillis = -5L))
    }
}
