package dev.pocketpanel.vpn

import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import android.util.Log
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.stringPreferencesKey
import java.io.IOException
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

/** Storage for the single imported WireGuard profile. */
interface VpnProfileStore {

    /** Non-secret summary, or null when no profile is stored. */
    val profile: Flow<VpnProfileSummary?>

    suspend fun save(summary: VpnProfileSummary, configText: String)

    /** Decrypts the stored configuration, or returns null when unreadable. */
    suspend fun readConfigText(): String?

    suspend fun clear()
}

/**
 * Encrypts the configuration with an AES/GCM key that never leaves the
 * Android Keystore. Only the ciphertext, the IV and the non-secret
 * summary are written to DataStore.
 */
class EncryptedVpnProfileStore(
    private val dataStore: DataStore<Preferences>,
) : VpnProfileStore {

    override val profile: Flow<VpnProfileSummary?> = dataStore.data
        .catch { throwable ->
            if (throwable is IOException) {
                Log.w(TAG, "Unable to read the stored VPN profile")
                emit(emptyPreferences())
            } else {
                throw throwable
            }
        }
        .map { stored ->
            val name = stored[Keys.NAME] ?: return@map null
            VpnProfileSummary(
                name = name,
                addressSummary = stored[Keys.ADDRESS]?.takeIf { it.isNotEmpty() },
                endpointSummary = stored[Keys.ENDPOINT]?.takeIf { it.isNotEmpty() },
            )
        }

    override suspend fun save(summary: VpnProfileSummary, configText: String) {
        val encrypted = encrypt(configText)
        dataStore.edit { stored ->
            stored[Keys.NAME] = summary.name
            stored[Keys.ADDRESS] = summary.addressSummary.orEmpty()
            stored[Keys.ENDPOINT] = summary.endpointSummary.orEmpty()
            stored[Keys.CIPHERTEXT] = encrypted.ciphertext
            stored[Keys.IV] = encrypted.iv
        }
    }

    override suspend fun readConfigText(): String? {
        val stored = try {
            dataStore.data.first()
        } catch (error: IOException) {
            Log.w(TAG, "Unable to read the stored VPN configuration")
            return null
        }
        val ciphertext = stored[Keys.CIPHERTEXT] ?: return null
        val iv = stored[Keys.IV] ?: return null
        return try {
            decrypt(ciphertext, iv)
        } catch (error: Exception) {
            // A rotated or invalidated Keystore key makes the ciphertext
            // unreadable. The configuration must be imported again.
            Log.w(TAG, "Unable to decrypt the stored VPN configuration")
            null
        }
    }

    override suspend fun clear() {
        dataStore.edit { stored ->
            stored.remove(Keys.NAME)
            stored.remove(Keys.ADDRESS)
            stored.remove(Keys.ENDPOINT)
            stored.remove(Keys.CIPHERTEXT)
            stored.remove(Keys.IV)
        }
    }

    private fun encrypt(plaintext: String): EncryptedConfig {
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.ENCRYPT_MODE, secretKey())
        return EncryptedConfig(
            ciphertext = Base64.encodeToString(
                cipher.doFinal(plaintext.toByteArray(Charsets.UTF_8)),
                Base64.NO_WRAP,
            ),
            iv = Base64.encodeToString(cipher.iv, Base64.NO_WRAP),
        )
    }

    private fun decrypt(ciphertext: String, iv: String): String {
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(
            Cipher.DECRYPT_MODE,
            secretKey(),
            GCMParameterSpec(GCM_TAG_BITS, Base64.decode(iv, Base64.NO_WRAP)),
        )
        return String(
            cipher.doFinal(Base64.decode(ciphertext, Base64.NO_WRAP)),
            Charsets.UTF_8,
        )
    }

    @Synchronized
    private fun secretKey(): SecretKey {
        val keyStore = KeyStore.getInstance(ANDROID_KEYSTORE).apply { load(null) }
        (keyStore.getEntry(KEY_ALIAS, null) as? KeyStore.SecretKeyEntry)
            ?.let { return it.secretKey }

        val generator = KeyGenerator.getInstance(
            KeyProperties.KEY_ALGORITHM_AES,
            ANDROID_KEYSTORE,
        )
        generator.init(
            KeyGenParameterSpec.Builder(
                KEY_ALIAS,
                KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT,
            )
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .setKeySize(KEY_SIZE_BITS)
                .build(),
        )
        return generator.generateKey()
    }

    private data class EncryptedConfig(val ciphertext: String, val iv: String)

    private object Keys {
        val NAME = stringPreferencesKey("vpn_profile_name")
        val ADDRESS = stringPreferencesKey("vpn_profile_address")
        val ENDPOINT = stringPreferencesKey("vpn_profile_endpoint")
        val CIPHERTEXT = stringPreferencesKey("vpn_config_ciphertext")
        val IV = stringPreferencesKey("vpn_config_iv")
    }

    private companion object {
        const val TAG = "VpnProfileStore"
        const val ANDROID_KEYSTORE = "AndroidKeyStore"
        const val KEY_ALIAS = "pocketpanel_vpn_config"
        const val TRANSFORMATION = "AES/GCM/NoPadding"
        const val GCM_TAG_BITS = 128
        const val KEY_SIZE_BITS = 256
    }
}
