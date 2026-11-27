package dev.pocketpanel.settings

import org.junit.Assert.assertEquals
import org.junit.Test

/** Stored preference values must degrade safely to their defaults. */
class SettingsEnumsTest {

    @Test
    fun handleSizeFallsBackToNormal() {
        assertEquals(HandleSize.COMPACT, HandleSize.fromStorage("COMPACT"))
        assertEquals(HandleSize.NORMAL, HandleSize.fromStorage("NORMAL"))
        assertEquals(HandleSize.NORMAL, HandleSize.fromStorage(null))
        assertEquals(HandleSize.NORMAL, HandleSize.fromStorage("unknown"))
    }

    @Test
    fun mainCardLayoutFallsBackToStandard() {
        assertEquals(MainCardLayout.FOCUS, MainCardLayout.fromStorage("FOCUS"))
        assertEquals(MainCardLayout.MINIMAL, MainCardLayout.fromStorage("MINIMAL"))
        assertEquals(MainCardLayout.STANDARD, MainCardLayout.fromStorage(null))
        assertEquals(MainCardLayout.STANDARD, MainCardLayout.fromStorage("unknown"))
    }
}
