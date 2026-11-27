package dev.pocketpanel.system

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class MemoryUsageTest {

    @Test
    fun usedPercentIsDerivedFromTotalAndAvailable() {
        assertEquals(50, MemoryUsage.usedPercent(200L, 100L))
        assertEquals(0, MemoryUsage.usedPercent(200L, 200L))
        assertEquals(100, MemoryUsage.usedPercent(200L, 0L))
    }

    @Test
    fun unusableSnapshotsReturnNull() {
        assertNull(MemoryUsage.usedPercent(null, 100L))
        assertNull(MemoryUsage.usedPercent(200L, null))
        assertNull(MemoryUsage.usedPercent(0L, 0L))
        assertNull(MemoryUsage.usedPercent(-1L, 0L))
        assertNull(MemoryUsage.usedPercent(100L, 200L))
    }
}
