package dev.pocketpanel.vpn

import com.wireguard.android.backend.Tunnel

/**
 * Stable tunnel handle for the WireGuard backend.
 *
 * GoBackend compares tunnels by identity, so the repository creates one
 * instance and reuses it for every connect and disconnect.
 */
class VpnTunnel(
    override val name: String,
    private val onState: (Tunnel.State) -> Unit,
) : Tunnel {

    override fun getName(): String = name

    override fun onStateChange(newState: Tunnel.State) {
        onState(newState)
    }
}
