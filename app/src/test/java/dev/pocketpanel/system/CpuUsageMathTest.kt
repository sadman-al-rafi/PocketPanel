package dev.pocketpanel.system

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/** Pure CPU calculations; no Android APIs are exercised. */
class CpuUsageMathTest {

    @Test
    fun counterDeltasProduceBusyPercentage() {
        assertEquals(20, CpuUsageMath.fromCounterDeltas(100L, 80L))
        assertEquals(100, CpuUsageMath.fromCounterDeltas(100L, 0L))
        assertEquals(0, CpuUsageMath.fromCounterDeltas(100L, 100L))
        assertEquals(50, CpuUsageMath.fromCounterDeltas(200L, 100L))
    }

    @Test
    fun unusableCounterDeltasReturnNull() {
        assertNull(CpuUsageMath.fromCounterDeltas(0L, 0L))
        assertNull(CpuUsageMath.fromCounterDeltas(-1L, 0L))
        assertNull(CpuUsageMath.fromCounterDeltas(100L, -1L))
        assertNull(CpuUsageMath.fromCounterDeltas(100L, 101L))
    }

    @Test
    fun frequencyEstimateUsesMinimumAndMaximum() {
        assertEquals(0, CpuUsageMath.fromFrequencies(listOf(1000L), listOf(1000L), listOf(4000L)))
        assertEquals(100, CpuUsageMath.fromFrequencies(listOf(4000L), listOf(1000L), listOf(4000L)))
        assertEquals(50, CpuUsageMath.fromFrequencies(listOf(2500L), listOf(1000L), listOf(4000L)))
    }

    @Test
    fun frequencyEstimateSumsAcrossCoresWithoutOverflow() {
        val current = listOf(3_000_000_000L, 3_000_000_000L)
        val min = listOf(1_000_000L, 1_000_000L)
        val max = listOf(3_000_000_000L, 3_000_000_000L)
        assertEquals(100, CpuUsageMath.fromFrequencies(current, min, max))
    }

    @Test
    fun invalidFrequencyInputsReturnNull() {
        assertNull(CpuUsageMath.fromFrequencies(emptyList(), emptyList(), emptyList()))
        assertNull(
            CpuUsageMath.fromFrequencies(
                listOf(1000L),
                listOf(1000L, 1000L),
                listOf(4000L),
            ),
        )
        assertNull(CpuUsageMath.fromFrequencies(listOf(1000L), listOf(1000L), listOf(1000L)))
        assertNull(CpuUsageMath.fromFrequencies(listOf(-1L), listOf(1000L), listOf(4000L)))
    }

    @Test
    fun procStatLineIsParsedWithoutDoubleCountingGuestTime() {
        val line = "cpu  123 4 56 789 10 1 2 3 4 5"
        assertEquals(988L to 799L, CpuUsageMath.parseProcStatLine(line))
    }

    @Test
    fun nonCpuOrMalformedLinesAreRejected() {
        assertNull(CpuUsageMath.parseProcStatLine("cpu0 1 2 3 4 5 6 7"))
        assertNull(CpuUsageMath.parseProcStatLine("cpu 1 2 3"))
        assertNull(CpuUsageMath.parseProcStatLine("cpu 1 x 3 4 5 6 7"))
    }
}
