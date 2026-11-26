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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import dev.pocketpanel.R
import dev.pocketpanel.settings.HandleEdge
import dev.pocketpanel.settings.UserPreferences
import dev.pocketpanel.ui.TestTags

/** Fixed overlay window sizes shared with [OverlayWindowController]. */
object OverlayDimensions {
    val handleWidth = 48.dp
    val handleHeight = 96.dp
    val panelWidth = 272.dp
    val panelHeight = 176.dp
    val expandedWidth = handleWidth + panelWidth
    val expandedHeight = panelHeight
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
) {
    val isLeft = preferences.handleEdge == HandleEdge.LEFT
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = if (isLeft) Alignment.CenterStart else Alignment.CenterEnd,
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (isLeft) {
                Handle(
                    expanded = expanded,
                    onToggleExpanded = onToggleExpanded,
                    onDragStart = onDragStart,
                    onDrag = onDrag,
                    onDragEnd = onDragEnd,
                    screenOrigin = screenOrigin,
                )
                if (expanded) {
                    Panel(onCollapse = onCollapse)
                }
            } else {
                if (expanded) {
                    Panel(onCollapse = onCollapse)
                }
                Handle(
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
            .size(OverlayDimensions.handleWidth, OverlayDimensions.handleHeight)
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
private fun Panel(onCollapse: () -> Unit) {
    Card(
        modifier = Modifier
            .width(OverlayDimensions.panelWidth)
            .height(OverlayDimensions.panelHeight)
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
            Text(
                text = stringResource(R.string.overlay_panel_body),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.widthIn(max = 240.dp),
            )
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
