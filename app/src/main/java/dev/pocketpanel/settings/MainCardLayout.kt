package dev.pocketpanel.settings

/**
 * Preset for which cards appear on the main screen.
 *
 * Stored as [name]; unknown values fall back to [STANDARD].
 */
enum class MainCardLayout {
    STANDARD,
    FOCUS,
    MINIMAL;

    companion object {
        fun fromStorage(value: String?): MainCardLayout =
            entries.firstOrNull { it.name == value } ?: STANDARD
    }
}
