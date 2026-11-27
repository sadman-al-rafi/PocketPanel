package dev.pocketpanel.ui

import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import dev.pocketpanel.settings.HandleEdge
import dev.pocketpanel.settings.HandleSize
import dev.pocketpanel.settings.MainCardLayout
import dev.pocketpanel.settings.ThemeMode
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
    onMonitoringEnabledChange: (Boolean) -> Unit,
    onMonitoringIntervalChange: (Int) -> Unit,
    onVpnImportConfig: (Uri) -> Unit,
    onVpnConnect: () -> Unit,
    onVpnDisconnect: () -> Unit,
    onVpnRemove: () -> Unit,
    onVpnDismissMessage: () -> Unit,
    onHandleSizeChange: (HandleSize) -> Unit,
    onHandleEdgeChange: (HandleEdge) -> Unit,
    onMainCardLayoutChange: (MainCardLayout) -> Unit,
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
                deviceInfo = uiState.deviceInfo,
                networkInfo = uiState.networkInfo,
                onMonitoringEnabledChange = onMonitoringEnabledChange,
                onMonitoringIntervalChange = onMonitoringIntervalChange,
                vpn = uiState.vpn,
                onVpnImportConfig = onVpnImportConfig,
                onVpnConnect = onVpnConnect,
                onVpnDisconnect = onVpnDisconnect,
                onVpnRemove = onVpnRemove,
                onVpnDismissMessage = onVpnDismissMessage,
            )

            AppScreen.Settings -> {
                BackHandler(onBack = onNavigateBack)
                SettingsScreen(
                    preferences = uiState.preferences,
                    onNavigateBack = onNavigateBack,
                    onThemeModeSelected = onThemeModeSelected,
                    onDynamicColorChange = onDynamicColorChange,
                    onMonitoringEnabledChange = onMonitoringEnabledChange,
                    onMonitoringIntervalChange = onMonitoringIntervalChange,
                    onHandleSizeChange = onHandleSizeChange,
                    onHandleEdgeChange = onHandleEdgeChange,
                    onMainCardLayoutChange = onMainCardLayoutChange,
                )
            }
        }
    }
}
