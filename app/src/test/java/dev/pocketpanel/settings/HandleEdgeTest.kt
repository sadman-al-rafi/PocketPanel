package dev.pocketpanel.settings

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test

class HandleEdgeTest {

    @Test
    fun fromStorageReturnsTheStoredEdge() {
        HandleEdge.entries.forEach { edge ->
            assertEquals(edge, HandleEdge.fromStorage(edge.name))
        }
    }

    @Test
    fun fromStorageFallsBackToRightForMissingOrUnknownValues() {
        assertEquals(HandleEdge.RIGHT, HandleEdge.fromStorage(null))
        assertEquals(HandleEdge.RIGHT, HandleEdge.fromStorage(""))
        assertEquals(HandleEdge.RIGHT, HandleEdge.fromStorage("CENTER"))
        assertEquals(HandleEdge.RIGHT, HandleEdge.fromStorage("left"))
    }

    @Test
    fun defaultPreferencesKeepTheOverlayOffOnTheRightEdge() {
        val defaults = UserPreferences()

        assertFalse(defaults.overlayEnabled)
        assertEquals(HandleEdge.RIGHT, defaults.handleEdge)
        assertEquals(0.5f, defaults.handleVerticalFraction, 0.0001f)
    }
}
