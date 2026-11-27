package dev.pocketpanel.system

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class PingLatencyTest {

    @Test
    fun parsesDecimalLatency() {
        assertEquals(
            23,
            parsePingLatency(
                "64 bytes from 8.8.8.8: icmp_seq=1 ttl=117 time=23.4 ms",
            ) ?: -1,
        )
    }

    @Test
    fun parsesWholeNumberLatency() {
        assertEquals(
            12,
            parsePingLatency(
                "64 bytes from 8.8.8.8: icmp_seq=1 ttl=117 time=12 ms",
            ) ?: -1,
        )
    }

    @Test
    fun parsesLessThanOneMillisecond() {
        assertEquals(
            1,
            parsePingLatency(
                "64 bytes from 8.8.8.8: icmp_seq=1 ttl=117 time<1 ms",
            ) ?: -1,
        )
    }

    @Test
    fun timeoutOutputReturnsNull() {
        assertNull(parsePingLatency("Request timeout for icmp_seq 0"))
    }

    @Test
    fun emptyOutputReturnsNull() {
        assertNull(parsePingLatency(""))
    }
}
