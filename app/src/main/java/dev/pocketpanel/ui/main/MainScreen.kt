package dev.pocketpanel.ui.main

import android.os.BatteryManager
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import dev.pocketpanel.R
import dev.pocketpanel.settings.UserPreferences
import dev.pocketpanel.system.DeviceInfo
import dev.pocketpanel.ui.TestTags
import dev.pocketpanel.ui.labelRes
import dev.pocketpanel.ui.theme.isDynamicColorAvailable

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    preferences: UserPreferences,
    overlayPermissionGranted: Boolean,
    notificationPermissionGranted: Boolean,
    onOverlayEnabledChange: (Boolean) -> Unit,
    onRequestOverlayPermission: () -> Unit,
    onOpenSettings: () -> Unit,
    deviceInfo: DeviceInfo? = null,
    onMonitoringEnabledChange: (Boolean) -> Unit = {},
    onMonitoringIntervalChange: (Int) -> Unit = {},
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(text = stringResource(R.string.app_name)) },
                actions = {
                    IconButton(onClick = onOpenSettings) {
                        Icon(
                            painter = painterResource(R.drawable.ic_settings),
                            contentDescription = stringResource(R.string.settings),
                        )
                    }
                },
            )
        },
    ) { innerPadding ->
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
        ) {
            val horizontalPadding = if (maxWidth >= 600.dp) 32.dp else 16.dp
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = horizontalPadding, vertical = 16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                Column(
                    modifier = Modifier.widthIn(max = 640.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    WelcomeCard()
                    OverlayCard(
                        preferences = preferences,
                        permissionGranted = overlayPermissionGranted,
                        notificationPermissionGranted = notificationPermissionGranted,
                        onOverlayEnabledChange = onOverlayEnabledChange,
                        onRequestOverlayPermission = onRequestOverlayPermission,
                    )
                    AppearanceCard(
                        preferences = preferences,
                        onOpenSettings = onOpenSettings,
                    )
                    MonitoringCard(
                        preferences = preferences,
                        deviceInfo = deviceInfo,
                        onMonitoringEnabledChange = onMonitoringEnabledChange,
                        onMonitoringIntervalChange = onMonitoringIntervalChange,
                    )
                }
            }
        }
    }
}

@Composable
private fun WelcomeCard() {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                text = stringResource(R.string.main_welcome_title),
                style = MaterialTheme.typography.headlineSmall,
            )
            Text(
                text = stringResource(R.string.main_welcome_body),
                style = MaterialTheme.typography.bodyMedium,
            )
            HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
            Text(
                text = stringResource(R.string.main_foundation_note),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun OverlayCard(
    preferences: UserPreferences,
    permissionGranted: Boolean,
    notificationPermissionGranted: Boolean,
    onOverlayEnabledChange: (Boolean) -> Unit,
    onRequestOverlayPermission: () -> Unit,
) {
    val enabled = preferences.overlayEnabled && permissionGranted
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                text = stringResource(R.string.overlay),
                style = MaterialTheme.typography.titleMedium,
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = stringResource(R.string.overlay_description),
                        style = MaterialTheme.typography.bodyMedium,
                    )
                    Text(
                        text = if (permissionGranted) {
                            stringResource(if (enabled) R.string.on else R.string.off)
                        } else {
                            stringResource(R.string.overlay_permission_required)
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = if (permissionGranted) {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        } else {
                            MaterialTheme.colorScheme.error
                        },
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Switch(
                    checked = enabled,
                    onCheckedChange = onOverlayEnabledChange,
                    modifier = Modifier.testTag(TestTags.OVERLAY_SWITCH),
                )
            }
            if (!permissionGranted) {
                TextButton(
                    onClick = onRequestOverlayPermission,
                    modifier = Modifier.testTag(TestTags.OVERLAY_PERMISSION_BUTTON),
                ) {
                    Text(text = stringResource(R.string.overlay_permission_grant))
                }
            }
            if (enabled && !notificationPermissionGranted) {
                Text(
                    text = stringResource(R.string.overlay_notification_denied),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun AppearanceCard(
    preferences: UserPreferences,
    onOpenSettings: () -> Unit,
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                text = stringResource(R.string.current_appearance),
                style = MaterialTheme.typography.titleMedium,
            )
            AppearanceRow(
                label = stringResource(R.string.theme),
                value = stringResource(preferences.themeMode.labelRes()),
            )
            AppearanceRow(
                label = stringResource(R.string.dynamic_color),
                value = dynamicColorValue(preferences.dynamicColor),
            )
            TextButton(onClick = onOpenSettings) {
                Text(text = stringResource(R.string.change_in_settings))
            }
        }
    }
}

@Composable
private fun AppearanceRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(text = label, style = MaterialTheme.typography.bodyLarge)
        Text(
            text = value,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun MonitoringCard(
    preferences: UserPreferences,
    deviceInfo: DeviceInfo?,
    onMonitoringEnabledChange: (Boolean) -> Unit,
    onMonitoringIntervalChange: (Int) -> Unit,
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                text = stringResource(R.string.monitoring),
                style = MaterialTheme.typography.titleMedium,
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = stringResource(R.string.monitoring_description),
                        style = MaterialTheme.typography.bodyMedium,
                    )
                    Text(
                        text = stringResource(
                            if (preferences.monitoringEnabled) R.string.on else R.string.off,
                        ),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Switch(
                    checked = preferences.monitoringEnabled,
                    onCheckedChange = onMonitoringEnabledChange,
                    modifier = Modifier.testTag(TestTags.MONITORING_TOGGLE),
                )
            }

            if (preferences.monitoringEnabled) {
                HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
                if (deviceInfo == null) {
                    Text(
                        text = stringResource(R.string.monitoring_waiting),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                } else {
                    MonitoringReadings(deviceInfo)
                }

                HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
                Text(
                    text = stringResource(R.string.monitoring_interval),
                    style = MaterialTheme.typography.bodyMedium,
                )
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    MONITORING_INTERVALS.forEach { seconds ->
                        TextButton(onClick = { onMonitoringIntervalChange(seconds) }) {
                            Text(
                                text = stringResource(
                                    R.string.monitoring_interval_seconds,
                                    seconds,
                                ),
                                color = if (preferences.monitoringIntervalSeconds == seconds) {
                                    MaterialTheme.colorScheme.primary
                                } else {
                                    MaterialTheme.colorScheme.onSurfaceVariant
                                },
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun MonitoringReadings(deviceInfo: DeviceInfo) {
    MonitoringReading(
        label = stringResource(R.string.monitoring_cpu_usage),
        value = deviceInfo.cpuUsagePercent?.let {
            stringResource(R.string.monitoring_percent, it)
        } ?: stringResource(R.string.not_available),
    )
    MonitoringReading(
        label = stringResource(R.string.monitoring_cpu_cores),
        value = deviceInfo.cpuCount?.toString()
            ?: stringResource(R.string.not_available),
    )
    val availableMemory = deviceInfo.availableMemoryBytes
    val totalMemory = deviceInfo.totalMemoryBytes
    MonitoringReading(
        label = stringResource(R.string.monitoring_memory),
        value = if (availableMemory != null && totalMemory != null) {
            stringResource(
                R.string.monitoring_memory_value,
                availableMemory / MIB,
                totalMemory / MIB,
            )
        } else {
            stringResource(R.string.not_available)
        },
    )
    MonitoringReading(
        label = stringResource(R.string.monitoring_battery_level),
        value = deviceInfo.batteryLevelPercent?.let {
            stringResource(R.string.monitoring_percent, it)
        } ?: stringResource(R.string.not_available),
    )
    MonitoringReading(
        label = stringResource(R.string.monitoring_battery_temperature),
        value = deviceInfo.batteryTemperatureCelsius?.let {
            stringResource(R.string.monitoring_temperature, it)
        } ?: stringResource(R.string.not_available),
    )
    MonitoringReading(
        label = stringResource(R.string.monitoring_charging),
        value = deviceInfo.isCharging?.let {
            stringResource(if (it) R.string.yes else R.string.no)
        } ?: stringResource(R.string.not_available),
    )
    MonitoringReading(
        label = stringResource(R.string.monitoring_battery_health),
        value = deviceInfo.batteryHealth?.let { batteryHealthLabel(it) }
            ?: stringResource(R.string.not_available),
    )
}

@Composable
private fun MonitoringReading(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(text = label, style = MaterialTheme.typography.bodyMedium)
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun batteryHealthLabel(health: Int): String = when (health) {
    BatteryManager.BATTERY_HEALTH_GOOD -> stringResource(R.string.monitoring_health_good)
    BatteryManager.BATTERY_HEALTH_OVERHEAT -> stringResource(R.string.monitoring_health_overheat)
    BatteryManager.BATTERY_HEALTH_DEAD -> stringResource(R.string.monitoring_health_dead)
    BatteryManager.BATTERY_HEALTH_OVER_VOLTAGE ->
        stringResource(R.string.monitoring_health_over_voltage)
    BatteryManager.BATTERY_HEALTH_UNSPECIFIED_FAILURE ->
        stringResource(R.string.monitoring_health_failure)
    BatteryManager.BATTERY_HEALTH_COLD -> stringResource(R.string.monitoring_health_cold)
    else -> stringResource(R.string.monitoring_health_unknown)
}

@Composable
private fun dynamicColorValue(enabled: Boolean): String = when {
    !isDynamicColorAvailable() -> stringResource(R.string.not_available)
    enabled -> stringResource(R.string.on)
    else -> stringResource(R.string.off)
}

private val MONITORING_INTERVALS = listOf(1, 2, 5, 10)

private const val MIB = 1024L * 1024L
