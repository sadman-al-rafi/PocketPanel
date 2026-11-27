package dev.pocketpanel.overlay

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.content.res.Configuration
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.ServiceCompat
import dev.pocketpanel.MainActivity
import dev.pocketpanel.PocketPanelApplication
import dev.pocketpanel.R
import dev.pocketpanel.settings.HandleEdge
import dev.pocketpanel.settings.SettingsRepository
import dev.pocketpanel.settings.UserPreferences
import dev.pocketpanel.system.NetworkMonitor
import dev.pocketpanel.system.SystemInfoRepository
import dev.pocketpanel.vpn.VpnRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

/**
 * Foreground service that keeps the floating overlay alive while the user
 * has it enabled.
 *
 * The service is started only from a visible app or an existing overlay,
 * never from a boot receiver, and it is not sticky. It calls
 * `startForeground` immediately in [onStartCommand] to satisfy the platform
 * deadline, then creates the overlay window.
 */
class OverlayService : Service() {

    private val lifecycleOwner = OverlayLifecycleOwner()
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    private lateinit var settingsRepository: SettingsRepository
    private lateinit var vpnRepository: VpnRepository
    // These need an attached Context, so they are created in onCreate,
    // not in field initializers (which run before Service.attach).
    private lateinit var systemInfoRepository: SystemInfoRepository
    private lateinit var networkMonitor: NetworkMonitor
    private var monitoringJob: Job? = null
    private var controller: OverlayWindowController? = null
    private var starting = false
    private var lifecycleStarted = false

    override fun onCreate() {
        super.onCreate()
        val app = application as PocketPanelApplication
        settingsRepository = app.settingsRepository
        vpnRepository = app.vpnRepository
        systemInfoRepository = SystemInfoRepository(applicationContext)
        networkMonitor = NetworkMonitor(applicationContext)
        createNotificationChannel()
        isRunning = true
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_STOP) {
            serviceScope.launch {
                persistOverlayEnabled(false)
                stopSelf()
            }
            return START_NOT_STICKY
        }

        startForegroundNow()

        if (!OverlayPermission.isGranted(this)) {
            stopBecause("Overlay permission is missing")
            return START_NOT_STICKY
        }

        if (controller == null && !starting) {
            starting = true
            serviceScope.launch {
                val initial = readInitialPreferences()
                if (controller != null) {
                    starting = false
                    return@launch
                }
                if (!OverlayPermission.isGranted(this@OverlayService)) {
                    starting = false
                    stopBecause("Overlay permission was revoked during startup")
                    return@launch
                }

                lifecycleOwner.start()
                lifecycleStarted = true
                val newController = OverlayWindowController(
                    context = this@OverlayService,
                    lifecycleOwner = lifecycleOwner,
                    onPositionChanged = { edge, fraction ->
                        serviceScope.launch {
                            persistHandlePosition(edge, fraction)
                        }
                    },
                )
                if (!newController.show(initial)) {
                    starting = false
                    stopBecause("Unable to add the overlay window")
                    return@launch
                }

                controller = newController
                starting = false
                try {
                    serviceScope.launch {
                        settingsRepository.preferences.collect { preferences ->
                            newController.update(preferences)
                            restartMonitoring(newController, preferences)
                        }
                    }
                    serviceScope.launch {
                        combine(
                            vpnRepository.state,
                            vpnRepository.profile,
                        ) { state, profile ->
                            newController.updateVpnState(state, profile != null)
                        }.collect()
                    }
                } catch (error: CancellationException) {
                    throw error
                } catch (error: Exception) {
                    Log.e(TAG, "Overlay settings stream failed", error)
                    stopBecause("Unable to observe overlay settings")
                }
            }
        }

        return START_NOT_STICKY
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        controller?.onConfigurationChanged()
    }

    override fun onDestroy() {
        isRunning = false
        monitoringJob?.cancel()
        monitoringJob = null
        serviceScope.cancel()
        controller?.hide()
        controller = null
        if (lifecycleStarted) {
            lifecycleStarted = false
            lifecycleOwner.destroy()
        }
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    /**
     * Sampling starts only while monitoring is enabled and the overlay is
     * attached, so the panel does not add a second always-on polling loop.
     */
    private fun restartMonitoring(
        controller: OverlayWindowController,
        preferences: UserPreferences,
    ) {
        monitoringJob?.cancel()
        monitoringJob = null
        if (!preferences.monitoringEnabled) {
            controller.updateMonitoring(null)
            return
        }
        val intervalMillis = preferences.monitoringIntervalSeconds * 1_000L
        monitoringJob = serviceScope.launch {
            combine(
                systemInfoRepository.deviceInfoFlow(intervalMillis),
                networkMonitor.networkInfoFlow(intervalMillis),
            ) { device, network -> OverlayMonitoring(device, network) }
                .collect { sample -> controller.updateMonitoring(sample) }
        }
    }

    private suspend fun readInitialPreferences(): UserPreferences = try {
        // DataStore's flow already maps I/O failures to defaults, so this
        // catch is only a guard against unexpected failures.
        settingsRepository.preferences.first()
    } catch (error: CancellationException) {
        throw error
    } catch (error: Exception) {
        Log.e(TAG, "Unable to read overlay settings; using defaults", error)
        UserPreferences()
    }

    private fun stopBecause(reason: String) {
        Log.w(TAG, "Stopping overlay service: $reason")
        serviceScope.launch {
            persistOverlayEnabled(false)
            stopSelf()
        }
    }

    private suspend fun persistOverlayEnabled(enabled: Boolean) {
        try {
            settingsRepository.setOverlayEnabled(enabled)
        } catch (error: CancellationException) {
            throw error
        } catch (error: Exception) {
            Log.e(TAG, "Unable to save the overlay setting", error)
        }
    }

    private suspend fun persistHandlePosition(edge: HandleEdge, fraction: Float) {
        try {
            settingsRepository.setHandlePosition(edge, fraction)
        } catch (error: CancellationException) {
            throw error
        } catch (error: Exception) {
            Log.e(TAG, "Unable to save the handle position", error)
        }
    }

    private fun startForegroundNow() {
        val type = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE
        } else {
            0
        }
        ServiceCompat.startForeground(
            this,
            NOTIFICATION_ID,
            buildNotification(),
            type,
        )
    }

    private fun createNotificationChannel() {
        val manager = getSystemService(NotificationManager::class.java)!!
        val channel = NotificationChannel(
            CHANNEL_ID,
            getString(R.string.overlay_notification_channel),
            NotificationManager.IMPORTANCE_LOW,
        ).apply {
            description = getString(R.string.overlay_notification_channel_description)
            setShowBadge(false)
        }
        manager.createNotificationChannel(channel)
    }

    private fun buildNotification(): Notification {
        val openIntent = Intent(this, MainActivity::class.java)
        val openPendingIntent = PendingIntent.getActivity(
            this,
            REQUEST_OPEN,
            openIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

        val stopIntent = Intent(this, OverlayService::class.java).setAction(ACTION_STOP)
        val stopPendingIntent = PendingIntent.getService(
            this,
            REQUEST_STOP,
            stopIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(getString(R.string.overlay_notification_title))
            .setContentText(getString(R.string.overlay_notification_text))
            .setContentIntent(openPendingIntent)
            .setOngoing(true)
            .setSilent(true)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .addAction(
                0,
                getString(R.string.overlay_notification_stop),
                stopPendingIntent,
            )
            .build()
    }

    companion object {
        const val ACTION_STOP = "dev.pocketpanel.action.STOP_OVERLAY"

        /**
         * True while this process has a live overlay service. It is not
         * persisted: after process death the next app launch restores the
         * overlay from the saved setting.
         */
        @Volatile
        var isRunning = false
            private set

        private const val TAG = "OverlayService"
        private const val CHANNEL_ID = "overlay"
        private const val NOTIFICATION_ID = 1
        private const val REQUEST_OPEN = 10
        private const val REQUEST_STOP = 11

        fun startIntent(context: Context): Intent =
            Intent(context, OverlayService::class.java)
    }
}
