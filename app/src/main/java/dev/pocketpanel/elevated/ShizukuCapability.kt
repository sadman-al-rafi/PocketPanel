package dev.pocketpanel.elevated

import android.content.pm.PackageManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import rikka.shizuku.Shizuku

/** Visibility of the optional Shizuku capability. */
sealed interface ShizukuStatus {
    data object Unavailable : ShizukuStatus
    data object PermissionNeeded : ShizukuStatus
    data object Granted : ShizukuStatus
}

/**
 * Runtime capability detection for Shizuku/Sui.
 *
 * All calls are guarded because the binder may be absent, dead or not yet
 * delivered. The app remains fully functional in [ShizukuStatus.Unavailable].
 */
object ShizukuCapability {

    private const val REQUEST_CODE = 1001

    private val _status = MutableStateFlow<ShizukuStatus>(ShizukuStatus.Unavailable)
    val status: StateFlow<ShizukuStatus> = _status.asStateFlow()

    private var initialized = false
    private var binderReceived = false
    private var permissionOverride: Boolean? = null

    fun initialize() {
        if (initialized) return
        initialized = true

        runCatching {
            Shizuku.addBinderReceivedListenerSticky {
                binderReceived = true
                permissionOverride = null
                refresh()
            }
            Shizuku.addBinderDeadListener {
                binderReceived = false
                permissionOverride = null
                _status.value = ShizukuStatus.Unavailable
            }
            Shizuku.addRequestPermissionResultListener { _, result ->
                permissionOverride = result == PackageManager.PERMISSION_GRANTED
                refresh()
            }
        }
        refresh()
    }

    fun refresh() {
        if (!binderReceived) {
            _status.value = ShizukuStatus.Unavailable
            return
        }
        val granted = permissionOverride
            ?: runCatching {
                Shizuku.checkSelfPermission() == PackageManager.PERMISSION_GRANTED
            }.getOrDefault(false)
        _status.value = if (granted) ShizukuStatus.Granted else ShizukuStatus.PermissionNeeded
    }

    fun requestPermission() {
        if (!binderReceived) return
        runCatching { Shizuku.requestPermission(REQUEST_CODE) }
    }

    /** 0 for root, 2000 for shell, or null when unavailable. */
    fun uid(): Int? = if (!binderReceived) {
        null
    } else {
        runCatching { Shizuku.getUid() }.getOrNull()
    }
}
