package dev.pocketpanel.vpn

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.net.VpnService
import android.provider.OpenableColumns
import android.util.Log
import com.wireguard.android.backend.BackendException
import com.wireguard.android.backend.GoBackend
import com.wireguard.android.backend.Tunnel
import com.wireguard.config.BadConfigException
import com.wireguard.config.Config
import java.io.ByteArrayOutputStream
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

/**
 * Single source of truth for the WireGuard profile and tunnel.
 *
 * The app UI consumes this repository, and the overlay side panel will
 * reuse the same surface later. Consent is requested by the caller with
 * [prepareIntent] before [connect], because the consent dialog must be
 * launched from an Activity.
 */
class VpnRepository(
    context: Context,
    private val profileStore: VpnProfileStore,
    private val scope: CoroutineScope,
) {

    val profile: StateFlow<VpnProfileSummary?> = profileStore.profile
        .stateIn(scope, SharingStarted.Eagerly, null)

    private val _state = MutableStateFlow<VpnConnectionState>(VpnConnectionState.Disconnected)
    val state: StateFlow<VpnConnectionState> = _state.asStateFlow()

    private val appContext = context.applicationContext
    private val backend: GoBackend by lazy { GoBackend(appContext) }
    private val operationMutex = Mutex()
    private var tunnel: VpnTunnel? = null

    /**
     * Returns the system consent intent, or null when consent was already
     * granted. The caller must launch a non-null intent for result.
     */
    fun prepareIntent(): Intent? = VpnService.prepare(appContext)

    /** Connects after consent has been granted. */
    fun connect() {
        scope.launch {
            operationMutex.withLock {
                if (_state.value == VpnConnectionState.Connected) return@withLock
                _state.value = VpnConnectionState.Connecting
                when (val loaded = loadConfig()) {
                    LoadedConfig.Unreadable -> {
                        _state.value = VpnConnectionState.Failed(VpnError.PROFILE_UNREADABLE)
                    }

                    LoadedConfig.Invalid -> {
                        _state.value = VpnConnectionState.Failed(VpnError.INVALID_PROFILE)
                    }

                    is LoadedConfig.Success -> startTunnel(loaded.config)
                }
            }
        }
    }

    /** Stops the tunnel. Safe to call when already disconnected. */
    fun disconnect() {
        scope.launch {
            operationMutex.withLock { stopTunnel() }
        }
    }

    /** Records that the user declined the system consent dialog. */
    fun onConsentDenied() {
        _state.value = VpnConnectionState.Failed(VpnError.NOT_AUTHORIZED)
    }

    suspend fun importFromUri(uri: Uri): VpnImportResult {
        val text = when (val read = withContext(Dispatchers.IO) { readConfigFile(uri) }) {
            ReadResult.Unreadable -> return VpnImportResult.UnreadableFile
            ReadResult.TooLarge -> return VpnImportResult.Invalid(ConfigError.TOO_LARGE)
            is ReadResult.Text -> read.value
        }
        val name = withContext(Dispatchers.IO) { displayName(uri) }
        return when (val parsed = WireGuardConfigParser.parse(text, name)) {
            is ConfigParseResult.Invalid -> VpnImportResult.Invalid(parsed.reason)
            is ConfigParseResult.Valid -> operationMutex.withLock {
                if (_state.value == VpnConnectionState.Connected) {
                    stopTunnel()
                }
                try {
                    profileStore.save(parsed.summary, text)
                    _state.value = VpnConnectionState.Disconnected
                    VpnImportResult.Success
                } catch (error: CancellationException) {
                    throw error
                } catch (error: Exception) {
                    Log.e(TAG, "Unable to store the VPN configuration")
                    VpnImportResult.StorageFailed
                }
            }
        }
    }

    suspend fun removeProfile(): VpnImportResult = operationMutex.withLock {
        if (_state.value == VpnConnectionState.Connected) {
            stopTunnel()
        }
        try {
            profileStore.clear()
            _state.value = VpnConnectionState.Disconnected
            VpnImportResult.Success
        } catch (error: CancellationException) {
            throw error
        } catch (error: Exception) {
            Log.e(TAG, "Unable to remove the VPN configuration")
            VpnImportResult.StorageFailed
        }
    }

    private suspend fun startTunnel(config: Config) {
        val activeTunnel = tunnel
            ?: VpnTunnel(TUNNEL_NAME, ::onTunnelState).also { tunnel = it }
        try {
            val result = withContext(Dispatchers.IO) {
                backend.setState(activeTunnel, Tunnel.State.UP, config)
            }
            _state.value = if (result == Tunnel.State.UP) {
                VpnConnectionState.Connected
            } else {
                VpnConnectionState.Failed(VpnError.ACTIVATION_FAILED)
            }
        } catch (error: BackendException) {
            _state.value = VpnConnectionState.Failed(mapBackendError(error.reason))
        } catch (error: CancellationException) {
            throw error
        } catch (error: LinkageError) {
            Log.e(TAG, "The WireGuard native library is unavailable")
            _state.value = VpnConnectionState.Failed(VpnError.ACTIVATION_FAILED)
        } catch (error: Exception) {
            Log.e(TAG, "Unable to connect the VPN (${error.javaClass.simpleName})")
            _state.value = VpnConnectionState.Failed(VpnError.ACTIVATION_FAILED)
        }
    }

    private suspend fun stopTunnel() {
        val activeTunnel = tunnel
        if (activeTunnel != null && _state.value != VpnConnectionState.Disconnected) {
            try {
                withContext(Dispatchers.IO) {
                    backend.setState(activeTunnel, Tunnel.State.DOWN, null)
                }
            } catch (error: CancellationException) {
                throw error
            } catch (error: LinkageError) {
                Log.e(TAG, "The WireGuard native library is unavailable")
            } catch (error: Exception) {
                Log.e(TAG, "Unable to disconnect the VPN (${error.javaClass.simpleName})")
            }
        }
        _state.value = VpnConnectionState.Disconnected
    }

    private fun onTunnelState(newState: Tunnel.State) {
        when (newState) {
            Tunnel.State.UP -> _state.value = VpnConnectionState.Connected
            Tunnel.State.DOWN -> _state.value = VpnConnectionState.Disconnected
            Tunnel.State.TOGGLE -> Unit
        }
    }

    private suspend fun loadConfig(): LoadedConfig {
        val text = try {
            profileStore.readConfigText()
        } catch (error: CancellationException) {
            throw error
        } catch (error: Exception) {
            Log.e(TAG, "Unable to read the stored VPN configuration")
            null
        } ?: return LoadedConfig.Unreadable

        return try {
            LoadedConfig.Success(Config.parse(text.reader().buffered()))
        } catch (error: BadConfigException) {
            LoadedConfig.Invalid
        } catch (error: Exception) {
            LoadedConfig.Invalid
        }
    }

    private fun readConfigFile(uri: Uri): ReadResult = try {
        val stream = appContext.contentResolver.openInputStream(uri)
            ?: return ReadResult.Unreadable
        stream.use { input ->
            val output = ByteArrayOutputStream()
            val buffer = ByteArray(READ_BUFFER_BYTES)
            var total = 0
            while (true) {
                val read = input.read(buffer)
                if (read < 0) break
                total += read
                if (total > MAX_CONFIG_BYTES) return ReadResult.TooLarge
                output.write(buffer, 0, read)
            }
            ReadResult.Text(String(output.toByteArray(), Charsets.UTF_8))
        }
    } catch (error: Exception) {
        Log.e(TAG, "Unable to read the selected configuration")
        ReadResult.Unreadable
    }

    private fun displayName(uri: Uri): String {
        val raw = try {
            appContext.contentResolver.query(
                uri,
                arrayOf(OpenableColumns.DISPLAY_NAME),
                null,
                null,
                null,
            )?.use { cursor ->
                if (cursor.moveToFirst()) cursor.getString(0) else null
            }
        } catch (error: Exception) {
            null
        }
        val candidate = (raw ?: uri.lastPathSegment)
            ?.substringAfterLast('/')
            ?.removeSuffix(".conf")
            ?.trim()
            .orEmpty()
        return candidate.ifEmpty { DEFAULT_PROFILE_NAME }.take(MAX_PROFILE_NAME_LENGTH)
    }

    private fun mapBackendError(reason: BackendException.Reason): VpnError = when (reason) {
        BackendException.Reason.VPN_NOT_AUTHORIZED -> VpnError.NOT_AUTHORIZED
        BackendException.Reason.TUNNEL_MISSING_CONFIG -> VpnError.INVALID_PROFILE
        else -> VpnError.ACTIVATION_FAILED
    }

    private sealed interface LoadedConfig {
        data class Success(val config: Config) : LoadedConfig
        data object Unreadable : LoadedConfig
        data object Invalid : LoadedConfig
    }

    private sealed interface ReadResult {
        data class Text(val value: String) : ReadResult
        data object TooLarge : ReadResult
        data object Unreadable : ReadResult
    }

    private companion object {
        const val TAG = "VpnRepository"
        const val TUNNEL_NAME = "PocketPanel"
        const val DEFAULT_PROFILE_NAME = "WireGuard profile"
        const val MAX_PROFILE_NAME_LENGTH = 60
        const val MAX_CONFIG_BYTES = WireGuardConfigParser.MAX_CONFIG_CHARS
        const val READ_BUFFER_BYTES = 8 * 1024
    }
}
