package dev.pocketpanel

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import dev.pocketpanel.ui.PocketPanelApp
import dev.pocketpanel.ui.PocketPanelViewModel
import dev.pocketpanel.ui.theme.PocketPanelTheme

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val settingsRepository = (application as PocketPanelApplication).settingsRepository

        setContent {
            val viewModel: PocketPanelViewModel = viewModel(
                factory = PocketPanelViewModel.factory(settingsRepository),
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
                    onOpenSettings = viewModel::openSettings,
                    onNavigateBack = viewModel::navigateBack,
                    onThemeModeSelected = viewModel::setThemeMode,
                    onDynamicColorChange = viewModel::setDynamicColor,
                )
            }
        }
    }
}
