package dev.pocketpanel.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import dev.pocketpanel.settings.SettingsRepository
import dev.pocketpanel.settings.ThemeMode
import dev.pocketpanel.settings.UserPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
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
)

class PocketPanelViewModel(
    private val settingsRepository: SettingsRepository,
) : ViewModel() {

    private val screen = MutableStateFlow<AppScreen>(AppScreen.Main)

    val uiState: StateFlow<PocketPanelUiState> = combine(
        settingsRepository.preferences,
        screen,
    ) { preferences, currentScreen ->
        PocketPanelUiState(preferences = preferences, screen = currentScreen)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
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

    companion object {
        fun factory(settingsRepository: SettingsRepository): ViewModelProvider.Factory =
            viewModelFactory {
                initializer { PocketPanelViewModel(settingsRepository) }
            }
    }
}
