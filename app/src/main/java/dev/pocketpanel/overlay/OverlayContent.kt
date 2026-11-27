package dev.pocketpanel.overlay

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import dev.pocketpanel.R
import dev.pocketpanel.settings.HandleEdge
import dev.pocketpanel.settings.HandleSize
import dev.pocketpanel.settings.UserPreferences
import dev.pocketpanel.system.MemoryUsage
import dev.pocketpanel.ui.TestTags
import dev.pocketpanel.vpn.VpnConnectionState

/** Overlay window dimensions in Dp, shared with [OverlayWindowController]. */
data class OverlaySize(
    val handleWidth: Dp,
    val handleHeight: Dp,
    val panelWidth: Dp,
    val panelHeight: Dp,
) {
    val expandedWidth: Dp get() = handleWidth + panelWidth
    val expandedHeight: Dp get() = panelHeight
}

/** Fixed size presets; the user selects one in settings. */
object OverlayDimensions {
    val NORMAL = OverlaySize(48.dp, 96.dp, 272.dp, 312.dp)
    val COMPACT = OverlaySize(40.dp, 80.dp, 232.dp, 288.dp)

    fun forSize(size: HandleSize): OverlaySize =
        if (size == HandleSize.COMPACT) COMPACT else NORMAL
}

@Composable
fun OverlayContent(
    preferences: UserPreferences,
    expanded: Boolean,
    onToggleExpanded: () -> Unit,
    onCollapse: () -> Unit,
    onDragStart: () -> Unit,
    onDrag: (Float, Float) -> Unit,
    onDragEnd: () -> Unit,
    screenOrigin: () -> Offset,
    vpnState: VpnConnectionState,
    hasVpnProfile: Boolean,
    onVpnToggle: () -> Unit,
    monitoring: OverlayMonitoring?,
) {
    val isLeft = preferences.handleEdge == HandleEdge.LEFT
    val size = OverlayDimensions.forSize(preferences.handleSize)
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = if (isLeft) Alignment.CenterStart else Alignment.CenterEnd,
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (isLeft) {
                Handle(
                    size = size,
                    expanded = expanded,
                    onToggleExpanded = onToggleExpanded,
                    onDragStart = onDragStart,
                    onDrag = onDrag,
                    onDragEnd = onDragEnd,
                    screenOrigin = screenOrigin,
                )
                if (expanded) {
                    Panel(
                        size = size,
                        vpnState = vpnState,
                        hasVpnProfile = hasVpnProfile,
                        onVpnToggle = onVpnToggle,
                        monitoring = monitoring,
                        onCollapse = onCollapse,
                    )
                }
            } else {
                if (expanded) {
                    Panel(
                        size = size,
                        vpnState = vpnState,
                        hasVpnProfile = hasVpnProfile,
                        onVpnToggle = onVpnToggle,
                        monitoring = monitoring,
                        onCollapse = onCollapse,
                    )
                }
                Handle(
                    size = size,
                    expanded = expanded,
                    onToggleExpanded = onToggleExpanded,
                    onDragStart = onDragStart,
                    onDrag = onDrag,
                    onDragEnd = onDragEnd,
                    screenOrigin = screenOrigin,
                )
            }
        }
    }
}

@Composable
private fun Handle(
    size: OverlaySize,
    expanded: Boolean,
    onToggleExpanded: () -> Unit,
    onDragStart: () -> Unit,
    onDrag: (Float, Float) -> Unit,
    onDragEnd: () -> Unit,
    screenOrigin: () -> Offset,
) {
    val description = stringResource(R.string.overlay_handle_description)
    val dragModifier = if (expanded) {
        Modifier
    } else {
        Modifier.pointerInput(Unit) {
            awaitEachGesture {
                val down = awaitFirstDown(requireUnconsumed = false)
                var lastScreenPosition = down.position + screenOrigin()
                var dragging = false
                try {
                    while (true) {
                        val event = awaitPointerEvent()
                        val change = event.changes.firstOrNull { it.id == down.id } ?: break
                        if (!change.pressed) break

                        // The window moves under the finger, so local deltas
                        // cancel out. Screen coordinates keep the drag stable.
                        val screenPosition = change.position + screenOrigin()
                        val delta = screenPosition - lastScreenPosition
                        if (!dragging && delta.getDistance() >= viewConfiguration.touchSlop) {
                            dragging = true
                            onDragStart()
                        }
                        if (dragging) {
                            change.consume()
                            onDrag(delta.x, delta.y)
                            lastScreenPosition = screenPosition
                        }
                    }
                } finally {
                    if (dragging) onDragEnd()
                }
            }
        }
    }

    Box(
        modifier = Modifier
            .size(size.handleWidth, size.handleHeight)
            .then(dragModifier)
            .clickable(role = Role.Button, onClick = onToggleExpanded)
            .semantics {
                contentDescription = description
            }
            .testTag(TestTags.OVERLAY_HANDLE),
        contentAlignment = Alignment.Center,
    ) {
        Box(
            modifier = Modifier
                .width(12.dp)
                .height(64.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(MaterialTheme.colorScheme.primary),
        )
    }
}

@Composable
private fun Panel(
    size: OverlaySize,
    vpnState: VpnConnectionState,
    hasVpnProfile: Boolean,
    onVpnToggle: () -> Unit,
    monitoring: OverlayMonitoring?,
    onCollapse: () -> Unit,
) {
    Card(
        modifier = Modifier
            .width(size.panelWidth)
            .height(size.panelHeight)
            .padding(8.dp),
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                text = stringResource(R.string.overlay_panel_title),
                style = MaterialTheme.typography.titleMedium,
            )
            if (monitoring != null) {
                val device = monitoring.deviceInfo
                val network = monitoring.networkInfo
                val unavailable = stringResource(R.string.not_available)
                val cpu = device.cpuUsagePercent?.let {
                    stringResource(R.string.monitoring_percent, it)
                } ?: unavailable
                val battery = device.batteryTemperatureCelsius?.let {
                    stringResource(R.string.monitoring_temperature, it)
                } ?: unavailable
                val memory = MemoryUsage.usedPercent(
                    device.totalMemoryBytes,
                    device.availableMemoryBytes,
                )?.let { stringResource(R.string.monitoring_percent, it) } ?: unavailable
                val ping = network.pingMillis?.let {
                    stringResource(R.string.network_ping_value, it)
                } ?: unavailable

                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        MetricTile(
                            label = stringResource(R.string.overlay_metric_cpu),
                            value = cpu,
                            modifier = Modifier.weight(1f),
                        )
                        MetricTile(
                            label = stringResource(R.string.overlay_metric_battery),
                            value = battery,
                            modifier = Modifier.weight(1f),
                        )
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        MetricTile(
                            label = stringResource(R.string.overlay_metric_memory),
                            value = memory,
                            modifier = Modifier.weight(1f),
                        )
                        MetricTile(
                            label = stringResource(R.string.overlay_metric_ping),
                            value = ping,
                            modifier = Modifier.weight(1f),
                        )
                    }
                }
            }
            Text(
                text = vpnStatusLabel(vpnState),
                style = MaterialTheme.typography.bodyMedium,
                color = if (vpnState is VpnConnectionState.Failed) {
                    MaterialTheme.colorScheme.error
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                },
            )
            if (!hasVpnProfile) {
                Text(
                    text = stringResource(R.string.overlay_vpn_no_profile),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.widthIn(max = 240.dp),
                )
            }
            Button(
                onClick = onVpnToggle,
                enabled = hasVpnProfile && vpnState !is VpnConnectionState.Connecting,
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 48.dp)
                    .testTag(TestTags.OVERLAY_VPN_BUTTON),
            ) {
                Text(
                    text = stringResource(
                        if (vpnState == VpnConnectionState.Connected) {
                            R.string.overlay_vpn_disconnect
                        } else {
                            R.string.overlay_vpn_connect
                        },
                    ),
                )
            }
            Spacer(modifier = Modifier.weight(1f))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
            ) {
                TextButton(onClick = onCollapse) {
                    Text(text = stringResource(R.string.overlay_panel_hide))
                }
            }
        }
    }
}

@Composable
private fun MetricTile(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.heightIn(min = 56.dp),
        verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(text = value, style = MaterialTheme.typography.titleSmall)
    }
}

@Composable
private fun vpnStatusLabel(state: VpnConnectionState): String = when (state) {
    VpnConnectionState.Disconnected -> stringResource(R.string.vpn_disconnected)
    VpnConnectionState.Connecting -> stringResource(R.string.vpn_connecting)
    VpnConnectionState.Connected -> stringResource(R.string.vpn_connected)
    is VpnConnectionState.Failed -> stringResource(R.string.overlay_vpn_permission_needed)
}
