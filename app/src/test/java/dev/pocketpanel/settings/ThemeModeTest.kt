package dev.pocketpanel.settings

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ThemeModeTest {

    @Test
    fun fromStorageReturnsTheStoredMode() {
        ThemeMode.entries.forEach { mode ->
            assertEquals(mode, ThemeMode.fromStorage(mode.name))
        }
    }

    @Test
    fun fromStorageFallsBackToSystemForMissingOrUnknownValues() {
        assertEquals(ThemeMode.SYSTEM, ThemeMode.fromStorage(null))
        assertEquals(ThemeMode.SYSTEM, ThemeMode.fromStorage(""))
        assertEquals(ThemeMode.SYSTEM, ThemeMode.fromStorage("SEPIA"))
        assertEquals(ThemeMode.SYSTEM, ThemeMode.fromStorage("dark"))
    }

    @Test
    fun systemModeFollowsTheSystemDarkTheme() {
        assertTrue(ThemeMode.SYSTEM.resolveDarkTheme(systemInDarkTheme = true))
        assertFalse(ThemeMode.SYSTEM.resolveDarkTheme(systemInDarkTheme = false))
    }

    @Test
    fun explicitModesOverrideTheSystemDarkTheme() {
        assertFalse(ThemeMode.LIGHT.resolveDarkTheme(systemInDarkTheme = true))
        assertFalse(ThemeMode.LIGHT.resolveDarkTheme(systemInDarkTheme = false))
        assertTrue(ThemeMode.DARK.resolveDarkTheme(systemInDarkTheme = true))
        assertTrue(ThemeMode.DARK.resolveDarkTheme(systemInDarkTheme = false))
    }

    @Test
    fun defaultPreferencesUseSystemThemeAndDynamicColor() {
        val defaults = UserPreferences()
        assertEquals(ThemeMode.SYSTEM, defaults.themeMode)
        assertTrue(defaults.dynamicColor)
    }
}
