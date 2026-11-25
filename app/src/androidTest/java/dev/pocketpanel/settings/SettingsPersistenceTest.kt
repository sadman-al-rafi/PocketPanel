package dev.pocketpanel.settings

import android.content.Context
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.preferencesDataStoreFile
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import java.io.File
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Proves the settings are written to disk: the first DataStore is shut down
 * after writing, and a second DataStore instance reads the same file back.
 */
@RunWith(AndroidJUnit4::class)
class SettingsPersistenceTest {

    private val context: Context =
        InstrumentationRegistry.getInstrumentation().targetContext

    @Test
    fun themeModeSurvivesStoreRecreation() = runBlocking {
        val file = context.preferencesDataStoreFile("settings-test-${System.nanoTime()}")
        try {
            withStore(file) { repository ->
                repository.setThemeMode(ThemeMode.DARK)
                assertEquals(
                    ThemeMode.DARK,
                    repository.preferences.first().themeMode,
                )
            }

            withStore(file) { repository ->
                assertEquals(
                    ThemeMode.DARK,
                    repository.preferences.first().themeMode,
                )
            }
        } finally {
            file.delete()
        }
    }

    @Test
    fun defaultsApplyToAFreshStore() = runBlocking {
        val file = context.preferencesDataStoreFile("settings-test-${System.nanoTime()}")
        try {
            withStore(file) { repository ->
                val preferences = repository.preferences.first()
                assertEquals(ThemeMode.SYSTEM, preferences.themeMode)
                assertTrue(preferences.dynamicColor)
            }
        } finally {
            file.delete()
        }
    }

    private suspend fun <T> withStore(
        file: File,
        block: suspend (DataStoreSettingsRepository) -> T,
    ): T {
        val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
        try {
            val store = PreferenceDataStoreFactory.create(
                scope = scope,
                produceFile = { file },
            )
            return block(DataStoreSettingsRepository(store))
        } finally {
            scope.cancel()
            scope.coroutineContext[Job]?.join()
        }
    }
}
