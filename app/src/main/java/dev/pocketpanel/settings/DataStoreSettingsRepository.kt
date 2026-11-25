package dev.pocketpanel.settings

import android.content.Context
import android.util.Log
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import java.io.IOException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map

/** Process-wide DataStore used by the application. */
val Context.settingsDataStore: DataStore<Preferences> by preferencesDataStore(
    name = "settings",
)

class DataStoreSettingsRepository(
    private val dataStore: DataStore<Preferences>,
) : SettingsRepository {

    override val preferences: Flow<UserPreferences> = dataStore.data
        .catch { throwable ->
            if (throwable is IOException) {
                // A corrupt or unreadable file must not crash the app; the
                // user sees defaults instead and the next write repairs it.
                Log.w(TAG, "Unable to read settings; using defaults", throwable)
                emit(emptyPreferences())
            } else {
                throw throwable
            }
        }
        .map { stored ->
            UserPreferences(
                themeMode = ThemeMode.fromStorage(stored[Keys.THEME_MODE]),
                dynamicColor = stored[Keys.DYNAMIC_COLOR] ?: UserPreferences().dynamicColor,
            )
        }

    override suspend fun setThemeMode(mode: ThemeMode) {
        dataStore.edit { stored -> stored[Keys.THEME_MODE] = mode.name }
    }

    override suspend fun setDynamicColor(enabled: Boolean) {
        dataStore.edit { stored -> stored[Keys.DYNAMIC_COLOR] = enabled }
    }

    private object Keys {
        val THEME_MODE = stringPreferencesKey("theme_mode")
        val DYNAMIC_COLOR = booleanPreferencesKey("dynamic_color")
    }

    private companion object {
        const val TAG = "SettingsRepository"
    }
}
