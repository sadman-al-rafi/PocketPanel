package dev.pocketpanel

import android.graphics.Color
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.graphics.toDrawable
import dev.pocketpanel.vpn.VpnConnectionState
import dev.pocketpanel.vpn.VpnRepository

/**
 * Transparent Activity used by the overlay to request Android's VPN
 * consent and then connect. It has no UI of its own.
 */
class VpnToggleActivity : ComponentActivity() {

    private val repository: VpnRepository
        get() = (application as PocketPanelApplication).vpnRepository

    private val consentLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult(),
    ) { result ->
        if (result.resultCode == RESULT_OK) {
            repository.connect()
        } else {
            repository.onConsentDenied()
        }
        finish()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        overridePendingTransition(0, 0)
        window.setBackgroundDrawable(Color.TRANSPARENT.toDrawable())
        window.setDimAmount(0f)

        // Disconnecting never needs consent again.
        if (repository.state.value == VpnConnectionState.Connected) {
            repository.disconnect()
            finish()
            return
        }

        val consentIntent = try {
            repository.prepareIntent()
        } catch (error: Exception) {
            null
        }
        if (consentIntent == null) {
            repository.connect()
            finish()
        } else {
            consentLauncher.launch(consentIntent)
        }
    }
}
