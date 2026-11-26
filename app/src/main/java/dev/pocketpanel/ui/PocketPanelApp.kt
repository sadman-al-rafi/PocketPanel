package dev.pocketpanel.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import dev.pocketpanel.settings.ThemeMode
import dev.pocketpanel.system.SystemViewModel
import dev.pocketpanel.system.monitoringEnabled
import dev.pocketpanel.system.onMonitoringToggle
import dev.pocketpanel.ui.main.MainScreen
import dev.pocketpanel.ui.settings.SettingsScreen

@Composable
fun PocketPanelApp(
    uiState: PocketPanelUiState,
    overlayPermissionGranted: Boolean,
    notificationPermissionGranted: Boolean,
    onOpenSettings: () -> Unit,
    onNavigateBack: () -> Unit,
    onThemeModeSelected: (ThemeMode) -> Unit,
    onDynamicColorChange: (Boolean) -> Unit,
    onOverlayEnabledChange: (Boolean) -> Unit,
    onRequestOverlayPermission: () -> Unit,
) {
    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background,
    ) {
        when (uiState.screen) {
            AppScreen.Main -> MainScreen(
                preferences = uiState.preferences,
                overlayPermissionGranted = overlayPermissionGranted,
                notificationPermissionGranted = notificationPermissionGranted,
                onOverlayEnabledChange = onOverlayEnabledChange,
                onRequestOverlayPermission = onRequestOverlayPermission,
                onOpenSettings = onOpenSettings,
                isMonitoringEnabled = uiState.preferences.overlayEnabled,
                onMonitoringToggle = {
                    isMonitoringEnabled = !isMonitoringEnabled
                    if (isMonitoringEnabled) {
                        onOverlayEnabledChange(true)
                    } else {
                        onOverlayEnabledChange(false)
                    }
                },
            )

            AppScreen.Settings -> {
                BackHandler(onBack = onNavigateBack)
                SettingsScreen(
                    preferences = uiState.preferences,
                    onNavigateBack = onNavigateBack,
                    onThemeModeSelected = onThemeModeSelected,
                    onDynamicColorChange = onDynamicColorChange,
                )
            }
        }
    }
}
