package dev.pocketpanel.settings

/** Handle size preset. Stored as [name]; unknown values fall back to [NORMAL]. */
enum class HandleSize {
    COMPACT,
    NORMAL;

    companion object {
        fun fromStorage(value: String?): HandleSize =
            entries.firstOrNull { it.name == value } ?: NORMAL
    }
}
