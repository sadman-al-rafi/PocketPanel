package dev.pocketpanel.settings

import kotlinx.coroutines.flow.Flow

/**
 * Single source of truth for persisted user settings.
 *
 * Screens and view models never read preference keys directly.
 */
interface SettingsRepository {

    val preferences: Flow<UserPreferences>

    suspend fun setThemeMode(mode: ThemeMode)

    suspend fun setDynamicColor(enabled: Boolean)

    suspend fun setOverlayEnabled(enabled: Boolean)

    /** Persists the resting edge and vertical position of the handle atomically. */
    suspend fun setHandlePosition(edge: HandleEdge, verticalFraction: Float)
}
