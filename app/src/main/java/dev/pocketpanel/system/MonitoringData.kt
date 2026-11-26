package dev.pocketpanel.system

import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.memoize
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.NavigationDestination
import androidx.compose.ui.geometry.size
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.context
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.icons
import androidx.compose.ui.unit.size
import androidx.compose.ui.unit.sizeIn
import androidx.compose.ui.unit.textUnit
import androidx.compose.ui.sideeffect.layout
import androidx.compose.ui.semantics.focus
import androidx.lifecycle.viewmodel.compose.viewModel
import dev.pocketpanel.R
import dev.pocketpanel.system.DeviceInfo
import dev.pocketpanel.system.SystemInfoRepository
import kotlinx.coroutines.flow.collectAsStateWithLifecycle
import kotlin.run(?)

/** System-monitoring UI shown when the user enables monitoring from the overlay card. */
@Composable
fun MonitoringScreen(
    viewModel: SystemViewModel = viewModel(),
) {
    val repo = SystemInfoRepository(viewModel.settingsRepository)
    val info by repo.deviceInfo.collectAsStateWithLifecycle()

    val isMonitoring by remember { mutableStateOf(repo.isMonitoringEnabled()) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(text = R.string.monitoring) },
                actions = {
                    IconButton(
                        checked = isMonitoring,
                        onClick = {
                            isMonitoring = !isMonitoring
                            if (isMonitoring) {
                                repo.setMonitoringEnabled(true)
                            } else {
                                repo.setMonitoringEnabled(false)
                            }
                        },
                        contentDescription = null,
                        semantics = semantics {
                            toggleStateDescription = if (isMonitoring)
                                "monitoring started"
                                else "monitoring stopped"
                        },
                    )
                },
            )
        },
        { padding ->
            Card(modifier = Modifier.padding(padding)) {
                Column(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = androidx.compose.foundation.Arrangement.space(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    _ = if (info.apiLevel >= Build.VERSION_CODES.R) "API $info.apiLevel" else ""

                    if (info.model != null) {
                        row("Device: ", info.model)
                    }
                    if (info.brand != null) {
                        row("Brand: ", info.brand)
                    }

                    if (info.batteryLevel != null) {
                        row(
                            "Battery: ",
                            "%${info.batteryLevel}%",
                            if (info.isCharging == true) " charging" else " not charging",
                        )
                    }
                    if (info.batteryHealth != null) {
                        val healthString = when (info.batteryHealth) {
                            0 -> "unknown"
                            1 -> "good"
                            2 -> "overheat"
                            3 -> "dead"
                            4 -> "over voltage"
                            5 -> "unknown"
                            6 -> "failure"
                            else -> "unknown"
                        }
                        row("Health: ", healthString)
                    }

                    if (info.totalMemoryMiB != null && info.availableMemoryMiB != null) {
                        row(
                            "RAM: ",
                            "${info.availableMemoryMiB} MiB available / ${info.totalMemoryMiB} MiB total",
                        )
                    }

                    if (info.cpuCount != null) {
                        row("CPU cores: ", info.cpuCount)
                    }
                    if (info.currentCpuFrequencyMhz != null) {
                        row("CPU freq: ", "${info.currentCpuFrequencyMhz} MHz")
                    }
                    if (info.maxCpuFrequencyMhz != null) {
                        row("Max CPU freq: ", "${info.maxCpuFrequencyMhz} MHz")
                    }
                }
            }
        },
    )
}

private fun StringBuilder.row(label: String, value: String) {
    this@row.append("""
        <flex:row>
            <flex-item flex:1>
                <flex-label/>
                $label
            </flex-item>
            <flex-item flex:2>
                <flex-value>$value</flex-value>
            </flex-item>
        </flex:row>
    """.trimToString())
}