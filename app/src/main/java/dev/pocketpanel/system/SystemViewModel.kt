package dev.pocketpanel.system

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.pocketpanel.settings.SettingsRepository

/** ViewModel for the system monitoring feature. */
class SystemViewModel(
    private val settingsRepository: SettingsRepository,
) : ViewModel() {

    private val _monitoringEnabled = MutableStateFlow(false)
    val monitoringEnabled: StateFlow<Boolean> = _monitoringEnabled.asStateFlow()

    init {
        // Restore the saved setting on creation
        _monitoringEnabled.value = settingsRepository.preferences.value.overlayEnabled
    }

    /** Toggle monitoring on/off and persist the new setting. */
    fun setMonitoringEnabled(enabled: Boolean) {
        _monitoringEnabled.value = enabled
        viewModelScope.launch {
            settingsRepository.setOverlayEnabled(enabled)
        }
    }
}