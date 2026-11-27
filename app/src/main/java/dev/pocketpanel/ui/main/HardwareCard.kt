package dev.pocketpanel.ui.main

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import dev.pocketpanel.R
import dev.pocketpanel.system.DeviceInfo

/** GPU identity from the documented OpenGL ES API. */
@Composable
fun HardwareCard(deviceInfo: DeviceInfo?) {
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
            }
        }
    }
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
