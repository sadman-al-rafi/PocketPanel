package dev.pocketpanel

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import dev.pocketpanel.overlay.OverlayPermission
import dev.pocketpanel.overlay.OverlayService
import dev.pocketpanel.settings.SettingsRepository
import dev.pocketpanel.system.SystemInfoRepository
import dev.pocketpanel.ui.PocketPanelApp
import dev.pocketpanel.ui.PocketPanelViewModel
import dev.pocketpanel.ui.theme.PocketPanelTheme
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    private lateinit var settingsRepository: SettingsRepository
    private lateinit var systemInfoRepository: SystemInfoRepository

    private val activityScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    private var overlayPermissionGranted by mutableStateOf(false)
    private var notificationPermissionGranted by mutableStateOf(false)
    private var pendingOverlayEnable = false

    private val notificationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { granted ->
        notificationPermissionGranted = granted
        // A denied notification permission only hides the service
        // notification. The overlay itself keeps working.
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        settingsRepository = (application as PocketPanelApplication).settingsRepository
        systemInfoRepository = SystemInfoRepository(applicationContext)
        overlayPermissionGranted = OverlayPermission.isGranted(this)
        notificationPermissionGranted = areNotificationsGranted()

        setContent {
            val viewModel: PocketPanelViewModel = viewModel(
                factory = PocketPanelViewModel.factory(
                    settingsRepository = settingsRepository,
                    systemInfoRepository = systemInfoRepository,
                ),
            )
            val uiState by viewModel.uiState.collectAsStateWithLifecycle()
            val systemInDarkTheme = isSystemInDarkTheme()
            val darkTheme = uiState.preferences.themeMode
                .resolveDarkTheme(systemInDarkTheme)

            PocketPanelTheme(
                darkTheme = darkTheme,
                dynamicColor = uiState.preferences.dynamicColor,
            ) {
                PocketPanelApp(
                    uiState = uiState,
                    overlayPermissionGranted = overlayPermissionGranted,
                    notificationPermissionGranted = notificationPermissionGranted,
                    onOpenSettings = viewModel::openSettings,
                    onNavigateBack = viewModel::navigateBack,
                    onThemeModeSelected = viewModel::setThemeMode,
                    onDynamicColorChange = viewModel::setDynamicColor,
                    onOverlayEnabledChange = ::setOverlayEnabled,
                    onRequestOverlayPermission = ::requestOverlayPermission,
                    onMonitoringEnabledChange = viewModel::setMonitoringEnabled,
                    onMonitoringIntervalChange = viewModel::setMonitoringIntervalSeconds,
                )
            }
        }
    }

    override fun onResume() {
        super.onResume()
        overlayPermissionGranted = OverlayPermission.isGranted(this)
        notificationPermissionGranted = areNotificationsGranted()

        activityScope.launch {
            val preferences = try {
                settingsRepository.preferences.first()
            } catch (error: CancellationException) {
                throw error
            } catch (error: Exception) {
                Log.e(TAG, "Unable to read the overlay setting", error)
                return@launch
            }
            when {
                pendingOverlayEnable -> {
                    pendingOverlayEnable = false
                    if (overlayPermissionGranted) {
                        startOverlay(enablePreference = true)
                    } else {
                        // The user came back without granting access. Keep
                        // the saved setting consistent with reality.
                        persistOverlayEnabled(false)
                        stopOverlayService()
                    }
                }

                preferences.overlayEnabled && overlayPermissionGranted -> {
                    // Restore the overlay after process death or a reboot
                    // while the setting is still on.
                    if (!OverlayService.isRunning) {
                        startOverlay(enablePreference = false)
                    }
                }

                preferences.overlayEnabled -> {
                    persistOverlayEnabled(false)
                    stopOverlayService()
                }
            }
        }
    }

    override fun onDestroy() {
        activityScope.cancel()
        super.onDestroy()
    }

    private fun setOverlayEnabled(enabled: Boolean) {
        if (enabled) {
            startOverlay(enablePreference = true)
        } else {
            stopOverlayService()
            activityScope.launch { persistOverlayEnabled(false) }
        }
    }

    private fun requestOverlayPermission() {
        pendingOverlayEnable = true
        if (!OverlayPermission.openSettings(this)) {
            pendingOverlayEnable = false
            Toast.makeText(
                this,
                R.string.overlay_permission_unavailable,
                Toast.LENGTH_LONG,
            ).show()
        }
    }

    private fun startOverlay(enablePreference: Boolean) {
        if (!OverlayPermission.isGranted(this)) {
            requestOverlayPermission()
            return
        }

        if (enablePreference) {
            requestNotificationPermissionIfNeeded()
        }

        val started = runCatching {
            ContextCompat.startForegroundService(this, OverlayService.startIntent(this))
        }.onFailure { error ->
            Log.e(TAG, "Unable to start the overlay service", error)
        }.isSuccess

        if (started && enablePreference) {
            activityScope.launch { persistOverlayEnabled(true) }
        } else if (!started) {
            activityScope.launch { persistOverlayEnabled(false) }
        }
    }

    private suspend fun persistOverlayEnabled(enabled: Boolean) {
        try {
            settingsRepository.setOverlayEnabled(enabled)
        } catch (error: CancellationException) {
            throw error
        } catch (error: Exception) {
            Log.e(TAG, "Unable to save the overlay setting", error)
        }
    }

    private fun stopOverlayService() {
        stopService(Intent(this, OverlayService::class.java))
    }

    private fun requestNotificationPermissionIfNeeded() {
        if (!areNotificationsGranted()) {
            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    private fun areNotificationsGranted(): Boolean =
        Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
            ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.POST_NOTIFICATIONS,
            ) == PackageManager.PERMISSION_GRANTED

    private companion object {
        const val TAG = "MainActivity"
    }
}
