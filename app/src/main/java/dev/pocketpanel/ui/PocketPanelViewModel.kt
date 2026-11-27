package dev.pocketpanel.ui

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import dev.pocketpanel.settings.HandleEdge
import dev.pocketpanel.settings.HandleSize
import dev.pocketpanel.settings.MainCardLayout
import dev.pocketpanel.settings.SettingsRepository
import dev.pocketpanel.settings.ThemeMode
import dev.pocketpanel.settings.UserPreferences
import dev.pocketpanel.system.DeviceInfo
import dev.pocketpanel.system.NetworkInfo
import dev.pocketpanel.system.NetworkMonitor
import dev.pocketpanel.system.SystemInfoRepository
import dev.pocketpanel.vpn.VpnConnectionState
import dev.pocketpanel.vpn.VpnImportResult
import dev.pocketpanel.vpn.VpnProfileSummary
import dev.pocketpanel.vpn.VpnRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/** Top-level destination. Two screens do not justify a navigation library. */
sealed interface AppScreen {
    data object Main : AppScreen
    data object Settings : AppScreen
}

data class PocketPanelUiState(
    val preferences: UserPreferences = UserPreferences(),
    val screen: AppScreen = AppScreen.Main,
    /** Latest monitoring sample, or null when monitoring is off or not ready. */
    val deviceInfo: DeviceInfo? = null,
    /** Latest network sample, or null when monitoring is off or not ready. */
    val networkInfo: NetworkInfo? = null,
    val vpn: VpnUiState = VpnUiState(),
)

/** VPN profile, tunnel state and the last import message. */
data class VpnUiState(
    val profile: VpnProfileSummary? = null,
    val connection: VpnConnectionState = VpnConnectionState.Disconnected,
    val importResult: VpnImportResult? = null,
)

private data class MonitoringSample(
    val deviceInfo: DeviceInfo,
    val networkInfo: NetworkInfo,
)

@OptIn(ExperimentalCoroutinesApi::class)
class PocketPanelViewModel(
    private val settingsRepository: SettingsRepository,
    private val systemInfoRepository: SystemInfoRepository,
    private val networkMonitor: NetworkMonitor,
    private val vpnRepository: VpnRepository,
) : ViewModel() {

    private val screen = MutableStateFlow<AppScreen>(AppScreen.Main)
    private val importResult = MutableStateFlow<VpnImportResult?>(null)

    /**
     * Polling only runs while the UI observes the state. Changing the
     * interval or stopping monitoring restarts the sampling flow.
     */
    private val monitoring: StateFlow<MonitoringSample?> = settingsRepository.preferences
        .map { preferences ->
            preferences.monitoringEnabled to preferences.monitoringIntervalSeconds
        }
        .distinctUntilChanged()
        .flatMapLatest { (enabled, intervalSeconds) ->
            if (!enabled) {
                flowOf(null)
            } else {
                val intervalMillis = intervalSeconds * 1_000L
                combine(
                    systemInfoRepository.deviceInfoFlow(intervalMillis),
                    networkMonitor.networkInfoFlow(intervalMillis),
                ) { deviceInfo, networkInfo ->
                    MonitoringSample(
                        deviceInfo = deviceInfo,
                        networkInfo = networkInfo,
                    )
                }
            }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
            initialValue = null,
        )

    private val vpn: StateFlow<VpnUiState> = combine(
        vpnRepository.profile,
        vpnRepository.state,
        importResult,
    ) { profile, connection, result ->
        VpnUiState(
            profile = profile,
            connection = connection,
            importResult = result,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
        initialValue = VpnUiState(),
    )

    val uiState: StateFlow<PocketPanelUiState> = combine(
        settingsRepository.preferences,
        screen,
        monitoring,
        vpn,
    ) { preferences, currentScreen, sample, vpnState ->
        PocketPanelUiState(
            preferences = preferences,
            screen = currentScreen,
            deviceInfo = sample?.deviceInfo,
            networkInfo = sample?.networkInfo,
            vpn = vpnState,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
        initialValue = PocketPanelUiState(),
    )

    fun openSettings() {
        screen.value = AppScreen.Settings
    }

    fun navigateBack() {
        screen.value = AppScreen.Main
    }

    fun setThemeMode(mode: ThemeMode) {
        viewModelScope.launch { settingsRepository.setThemeMode(mode) }
    }

    fun setDynamicColor(enabled: Boolean) {
        viewModelScope.launch { settingsRepository.setDynamicColor(enabled) }
    }

    fun setMonitoringEnabled(enabled: Boolean) {
        viewModelScope.launch { settingsRepository.setMonitoringEnabled(enabled) }
    }

    fun setMonitoringIntervalSeconds(seconds: Int) {
        viewModelScope.launch { settingsRepository.setMonitoringIntervalSeconds(seconds) }
    }

    fun setHandleSize(size: HandleSize) {
        viewModelScope.launch { settingsRepository.setHandleSize(size) }
    }

    fun setHandleEdge(edge: HandleEdge) {
        viewModelScope.launch { settingsRepository.setHandleEdge(edge) }
    }

    fun setMainCardLayout(layout: MainCardLayout) {
        viewModelScope.launch { settingsRepository.setMainCardLayout(layout) }
    }

    fun disconnectVpn() {
        vpnRepository.disconnect()
    }

    fun importVpnConfig(uri: Uri) {
        viewModelScope.launch {
            importResult.value = vpnRepository.importFromUri(uri)
        }
    }

    fun removeVpnProfile() {
        viewModelScope.launch {
            importResult.value = vpnRepository.removeProfile()
        }
    }

    fun dismissVpnMessage() {
        importResult.value = null
    }

    companion object {
        private const val STOP_TIMEOUT_MILLIS = 5_000L

        fun factory(
            settingsRepository: SettingsRepository,
            systemInfoRepository: SystemInfoRepository,
            networkMonitor: NetworkMonitor,
            vpnRepository: VpnRepository,
        ): ViewModelProvider.Factory = viewModelFactory {
            initializer {
                PocketPanelViewModel(
                    settingsRepository = settingsRepository,
                    systemInfoRepository = systemInfoRepository,
                    networkMonitor = networkMonitor,
                    vpnRepository = vpnRepository,
                )
            }
        }
    }
}
