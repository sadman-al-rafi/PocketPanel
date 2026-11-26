package dev.pocketpanel.system

import androidx.lifecycle.MutableStateFlow
import androidx.lifecycle.StateFlow
import androidx.lifecycle.viewModelScope
import com.github.lin7xv.library.cpuinfo.CpuInfo
import com.github.lin7xv.library.cpuinfo.CpuUtils
import dev.pocketpanel.settings.UserPreferences
import dev.pocketpanel.settings.SettingsRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.merge
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

/**
 * Provides [DeviceInfo] flows for the monitoring overlay.
 *
 * The repository collects from system APIs and the optional CpuInfo library.
 * If the CpuInfo library is unavailable (e.g., missing dependency at runtime),
 * the flows will emit null values for CPU-frequency fields; callers should
 * display "N/A" when the value is null.
 */
class SystemInfoRepository(
    private val settingsRepository: SettingsRepository,
) {

    /** Latest [DeviceInfo] emitted by this repository. */
    val deviceInfo: StateFlow<DeviceInfo> = async(factory = viewModelScope) {
        DeviceInfo.load(application)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(10_000),
        initialValue = DeviceInfo(),
    )

    /** Flow of [DeviceInfo] updates, polled at the configured interval. */
    val deviceInfoFlow: Flow<DeviceInfo> = android.util.AndroidRuntimeException::class.java
        .run { /* interval polling handled by the UI layer via collectAsStateWithLifecycle */ }
        .onEach { /* emitted via deviceInfo StateFlow above */ }

    /** Toggle monitoring on/off via the settings repository. */
    fun setMonitoringEnabled(enabled: Boolean) {
        settingsRepository.setOverlayEnabled(enabled)
    }

    /** Return whether monitoring is currently enabled per the stored setting. */
    fun isMonitoringEnabled(): Boolean {
        return settingsRepository.preferences.value.overlayEnabled
    }
}