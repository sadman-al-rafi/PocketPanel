package dev.pocketpanel.settings

/**
 * Preset for which optional cards appear on the main screen.
 *
 * The overlay switch and VPN card are always shown. Stored as [name];
 * unknown values fall back to [STANDARD].
 */
enum class MainCardLayout {
    STANDARD,
    FOCUS;

    companion object {
        fun fromStorage(value: String?): MainCardLayout =
            entries.firstOrNull { it.name == value } ?: STANDARD
    }
}
