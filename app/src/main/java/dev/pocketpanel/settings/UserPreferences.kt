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
    val overlayEnabled: Boolean = false,
    val monitoringEnabled: Boolean = false,
    /** Sampling interval in seconds; always within the supported range. */
    val monitoringIntervalSeconds: Int = DEFAULT_MONITORING_INTERVAL_SECONDS,
    val handleEdge: HandleEdge = HandleEdge.RIGHT,
    val handleVerticalFraction: Float = 0.5f,
    val handleSize: HandleSize = HandleSize.NORMAL,
    val mainCardLayout: MainCardLayout = MainCardLayout.STANDARD,
) {
    companion object {
        const val MIN_MONITORING_INTERVAL_SECONDS = 1
        const val MAX_MONITORING_INTERVAL_SECONDS = 10
        const val DEFAULT_MONITORING_INTERVAL_SECONDS = 2
    }
}
