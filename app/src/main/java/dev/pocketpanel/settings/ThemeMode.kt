package dev.pocketpanel.settings

/**
 * User-selectable theme mode.
 *
 * The stored representation is [name]; [fromStorage] tolerates missing or
 * unknown values by falling back to [SYSTEM].
 */
enum class ThemeMode {
    SYSTEM,
    LIGHT,
    DARK;

    /** Resolves the effective dark-theme flag for a given system state. */
    fun resolveDarkTheme(systemInDarkTheme: Boolean): Boolean = when (this) {
        SYSTEM -> systemInDarkTheme
        LIGHT -> false
        DARK -> true
    }

    companion object {
        fun fromStorage(value: String?): ThemeMode =
            entries.firstOrNull { it.name == value } ?: SYSTEM
    }
}
