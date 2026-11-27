package dev.pocketpanel.ui.main

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import dev.pocketpanel.R
import dev.pocketpanel.elevated.ShizukuStatus
import dev.pocketpanel.system.DeviceInfo
import dev.pocketpanel.system.ThermalLevel
import dev.pocketpanel.ui.TestTags

/** GPU identity and thermal readings from documented Android APIs. */
@Composable
fun HardwareCard(
    deviceInfo: DeviceInfo?,
    shizukuStatus: ShizukuStatus = ShizukuStatus.Unavailable,
    onRequestShizukuPermission: () -> Unit = {},
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                text = stringResource(R.string.hardware_advanced),
                style = MaterialTheme.typography.titleMedium,
            )
            if (deviceInfo == null) {
                Text(
                    text = stringResource(R.string.hardware_monitoring_off),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            } else {
                HardwareRow(
                    label = stringResource(R.string.hardware_gpu_renderer),
                    value = deviceInfo.gpuRenderer ?: stringResource(R.string.not_available),
                )
                HardwareRow(
                    label = stringResource(R.string.hardware_gpu_vendor),
                    value = deviceInfo.gpuVendor ?: stringResource(R.string.not_available),
                )
                HardwareRow(
                    label = stringResource(R.string.hardware_opengl_es),
                    value = deviceInfo.glVersion ?: stringResource(R.string.not_available),
                )
                Text(
                    text = stringResource(R.string.hardware_gpu_note),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )

                // Values below come from the optional Shizuku user service.
                HardwareRow(
                    label = stringResource(R.string.hardware_soc_temperature),
                    value = deviceInfo.socTemperatureCelsius?.let {
                        stringResource(
                            R.string.hardware_soc_temperature_value,
                            it,
                            deviceInfo.socThermalZone.orEmpty(),
                        )
                    } ?: stringResource(R.string.not_available),
                )
                HardwareRow(
                    label = stringResource(R.string.hardware_gpu_busy),
                    value = deviceInfo.gpuBusyPercent?.let {
                        stringResource(R.string.monitoring_percent, it)
                    } ?: stringResource(R.string.not_available),
                )
                HardwareRow(
                    label = stringResource(R.string.hardware_gpu_clock),
                    value = deviceInfo.gpuClockKhz?.let {
                        stringResource(R.string.hardware_gpu_clock_value, it / 1000L)
                    } ?: stringResource(R.string.not_available),
                )
                Text(
                    text = stringResource(R.string.hardware_driver_note),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )

                HorizontalDivider()

                HardwareRow(
                    label = stringResource(R.string.hardware_thermal_status),
                    value = deviceInfo.thermalStatus?.let { thermalLabel(it) }
                        ?: unavailableThermal(deviceInfo.apiLevel, THERMAL_STATUS_API),
                )
                HardwareRow(
                    label = stringResource(R.string.hardware_thermal_headroom),
                    value = deviceInfo.thermalHeadroom?.let {
                        stringResource(R.string.hardware_thermal_headroom_value, it)
                    } ?: unavailableThermal(deviceInfo.apiLevel, THERMAL_HEADROOM_API),
                )
                Text(
                    text = stringResource(R.string.hardware_thermal_note),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )

                HorizontalDivider()

                HardwareRow(
                    label = stringResource(R.string.hardware_shizuku),
                    value = shizukuLabel(shizukuStatus),
                )
                if (shizukuStatus is ShizukuStatus.PermissionNeeded) {
                    Text(
                        text = stringResource(R.string.hardware_shizuku_explanation),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    TextButton(
                        onClick = onRequestShizukuPermission,
                        modifier = Modifier.testTag(TestTags.SHIZUKU_PERMISSION_BUTTON),
                    ) {
                        Text(text = stringResource(R.string.hardware_shizuku_grant))
                    }
                }
                if (shizukuStatus is ShizukuStatus.Granted) {
                    Text(
                        text = stringResource(R.string.hardware_shizuku_active),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}

@Composable
private fun shizukuLabel(status: ShizukuStatus): String = stringResource(
    when (status) {
        ShizukuStatus.Unavailable -> R.string.hardware_shizuku_unavailable
        ShizukuStatus.PermissionNeeded -> R.string.hardware_shizuku_permission
        ShizukuStatus.Granted -> R.string.hardware_shizuku_granted
    },
)

@Composable
private fun unavailableThermal(apiLevel: Int, requiredApi: Int): String =
    if (apiLevel < requiredApi) {
        stringResource(R.string.hardware_thermal_unsupported)
    } else {
        stringResource(R.string.not_available)
    }

@Composable
private fun HardwareRow(label: String, value: String) {
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
private fun thermalLabel(level: ThermalLevel): String = stringResource(
    when (level) {
        ThermalLevel.NONE -> R.string.thermal_none
        ThermalLevel.LIGHT -> R.string.thermal_light
        ThermalLevel.MODERATE -> R.string.thermal_moderate
        ThermalLevel.SEVERE -> R.string.thermal_severe
        ThermalLevel.CRITICAL -> R.string.thermal_critical
        ThermalLevel.EMERGENCY -> R.string.thermal_emergency
        ThermalLevel.SHUTDOWN -> R.string.thermal_shutdown
        ThermalLevel.UNKNOWN -> R.string.thermal_unknown
    },
)

private const val THERMAL_STATUS_API = 29
private const val THERMAL_HEADROOM_API = 30
