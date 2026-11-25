package dev.pocketpanel.settings

/**
 * Immutable snapshot of the user-visible settings.
 *
 * Defaults are used before the first value is read from disk and when a
 * stored value is missing.
 */
data class UserPreferences(
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val dynamicColor: Boolean = true,
)
