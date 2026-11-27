package dev.pocketpanel.system

/** Host used by the ping probe. */
const val PING_HOST: String = "8.8.8.8"

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
 * per-application attribution. A null rate or ping means the value is
 * not available; it is never estimated.
 */
data class NetworkInfo(
    val transport: NetworkTransport? = null,
    val receiveBytesPerSecond: Long? = null,
    val transmitBytesPerSecond: Long? = null,
    /** Round-trip latency to [PING_HOST] in milliseconds, or null. */
    val pingMillis: Int? = null,
)
