package dev.pocketpanel.settings

/**
 * Screen edge the floating handle rests against.
 *
 * Stored as [name]; unknown values fall back to [RIGHT].
 */
enum class HandleEdge {
    LEFT,
    RIGHT;

    companion object {
        fun fromStorage(value: String?): HandleEdge =
            entries.firstOrNull { it.name == value } ?: RIGHT
    }
}
