package dev.pocketpanel

import android.app.Application
import dev.pocketpanel.settings.DataStoreSettingsRepository
import dev.pocketpanel.settings.SettingsRepository
import dev.pocketpanel.settings.settingsDataStore

class PocketPanelApplication : Application() {

    val settingsRepository: SettingsRepository by lazy {
        DataStoreSettingsRepository(settingsDataStore)
    }
}
