package dev.pocketpanel.settings

import android.content.Context
import android.util.Log
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.floatPreferencesKey
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
            val defaults = UserPreferences()
            UserPreferences(
                themeMode = ThemeMode.fromStorage(stored[Keys.THEME_MODE]),
                dynamicColor = stored[Keys.DYNAMIC_COLOR] ?: defaults.dynamicColor,
                overlayEnabled = stored[Keys.OVERLAY_ENABLED] ?: defaults.overlayEnabled,
                handleEdge = HandleEdge.fromStorage(stored[Keys.HANDLE_EDGE]),
                handleVerticalFraction = (stored[Keys.HANDLE_VERTICAL_FRACTION]
                    ?: defaults.handleVerticalFraction).coerceIn(0f, 1f),
            )
        }

    override suspend fun setThemeMode(mode: ThemeMode) {
        dataStore.edit { stored -> stored[Keys.THEME_MODE] = mode.name }
    }

    override suspend fun setDynamicColor(enabled: Boolean) {
        dataStore.edit { stored -> stored[Keys.DYNAMIC_COLOR] = enabled }
    }

    override suspend fun setOverlayEnabled(enabled: Boolean) {
        dataStore.edit { stored -> stored[Keys.OVERLAY_ENABLED] = enabled }
    }

    override suspend fun setHandlePosition(edge: HandleEdge, verticalFraction: Float) {
        dataStore.edit { stored ->
            stored[Keys.HANDLE_EDGE] = edge.name
            stored[Keys.HANDLE_VERTICAL_FRACTION] = verticalFraction.coerceIn(0f, 1f)
        }
    }

    private object Keys {
        val THEME_MODE = stringPreferencesKey("theme_mode")
        val DYNAMIC_COLOR = booleanPreferencesKey("dynamic_color")
        val OVERLAY_ENABLED = booleanPreferencesKey("overlay_enabled")
        val HANDLE_EDGE = stringPreferencesKey("handle_edge")
        val HANDLE_VERTICAL_FRACTION = floatPreferencesKey("handle_vertical_fraction")
    }

    private companion object {
        const val TAG = "SettingsRepository"
    }
}
