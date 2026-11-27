package dev.pocketpanel

import android.app.Application
import dev.pocketpanel.settings.DataStoreSettingsRepository
import dev.pocketpanel.settings.SettingsRepository
import dev.pocketpanel.settings.settingsDataStore
import dev.pocketpanel.elevated.ElevatedHardwareAccess
import dev.pocketpanel.elevated.ShizukuHardwareAccess
import dev.pocketpanel.vpn.EncryptedVpnProfileStore
import dev.pocketpanel.vpn.VpnRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob

class PocketPanelApplication : Application() {

    private val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    val settingsRepository: SettingsRepository by lazy {
        DataStoreSettingsRepository(settingsDataStore)
    }

    val vpnRepository: VpnRepository by lazy {
        VpnRepository(
            context = this,
            profileStore = EncryptedVpnProfileStore(settingsDataStore),
            scope = applicationScope,
        )
    }

    /** Optional; standard monitoring works without Shizuku. */
    val elevatedHardwareAccess: ElevatedHardwareAccess by lazy {
        ShizukuHardwareAccess(this).also { it.initialize() }
    }
}
