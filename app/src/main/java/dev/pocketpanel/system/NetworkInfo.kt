package dev.pocketpanel.system

/** Transport of the active network. */
enum class NetworkTransport {
    WIFI,
    CELLULAR,
    ETHERNET,
    VPN,
    OTHER,
    NONE,
}

/**
 * Immutable snapshot of the active network.
 *
 * Throughput is device-wide because `TrafficStats` does not expose
 * per-application attribution. A null rate means the value is not
 * available; it is never estimated.
 */
data class NetworkInfo(
    val transport: NetworkTransport? = null,
    val receiveBytesPerSecond: Long? = null,
    val transmitBytesPerSecond: Long? = null,
)
