package dev.pocketpanel.overlay

import dev.pocketpanel.system.DeviceInfo
import dev.pocketpanel.system.NetworkInfo

/** Latest monitoring sample shown in the overlay panel. */
data class OverlayMonitoring(
    val deviceInfo: DeviceInfo,
    val networkInfo: NetworkInfo,
)
