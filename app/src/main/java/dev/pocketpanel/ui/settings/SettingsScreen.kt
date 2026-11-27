package dev.pocketpanel.ui.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import dev.pocketpanel.R
import dev.pocketpanel.settings.HandleEdge
import dev.pocketpanel.settings.HandleSize
import dev.pocketpanel.settings.MainCardLayout
import dev.pocketpanel.settings.ThemeMode
import dev.pocketpanel.settings.UserPreferences
import dev.pocketpanel.ui.TestTags
import dev.pocketpanel.ui.descriptionRes
import dev.pocketpanel.ui.labelRes
import dev.pocketpanel.ui.theme.isDynamicColorAvailable

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    preferences: UserPreferences,
    onNavigateBack: () -> Unit,
    onThemeModeSelected: (ThemeMode) -> Unit,
    onDynamicColorChange: (Boolean) -> Unit,
    onMonitoringEnabledChange: (Boolean) -> Unit,
    onMonitoringIntervalChange: (Int) -> Unit,
    onHandleSizeChange: (HandleSize) -> Unit,
    onHandleEdgeChange: (HandleEdge) -> Unit,
    onMainCardLayoutChange: (MainCardLayout) -> Unit,
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(text = stringResource(R.string.settings)) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            painter = painterResource(R.drawable.ic_arrow_back),
                            contentDescription = stringResource(R.string.back),
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
            ) {
                Column(
                    modifier = Modifier.widthIn(max = 640.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    SectionHeader(text = stringResource(R.string.appearance))

                    ThemeMode.entries.forEach { mode ->
                        ThemeModeOption(
                            mode = mode,
                            selected = preferences.themeMode == mode,
                            onClick = { onThemeModeSelected(mode) },
                        )
                    }

                    HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp))

                    DynamicColorOption(
                        enabled = preferences.dynamicColor,
                        onValueChange = onDynamicColorChange,
                    )

                    HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp))

                    SectionHeader(text = stringResource(R.string.settings_main_screen))
                    MainCardLayout.entries.forEach { layout ->
                        ChoiceRow(
                            label = stringResource(layout.labelRes()),
                            description = stringResource(layout.descriptionRes()),
                            selected = preferences.mainCardLayout == layout,
                            onClick = { onMainCardLayoutChange(layout) },
                            modifier = Modifier.testTag(layout.testTag),
                        )
                    }

                    HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp))

                    SectionHeader(text = stringResource(R.string.settings_handle))
                    HandleSize.entries.forEach { size ->
                        ChoiceRow(
                            label = stringResource(size.labelRes()),
                            description = stringResource(size.descriptionRes()),
                            selected = preferences.handleSize == size,
                            onClick = { onHandleSizeChange(size) },
                            modifier = Modifier.testTag(size.testTag),
                        )
                    }
                    HandleEdge.entries.forEach { edge ->
                        ChoiceRow(
                            label = stringResource(edge.labelRes()),
                            description = stringResource(edge.descriptionRes()),
                            selected = preferences.handleEdge == edge,
                            onClick = { onHandleEdgeChange(edge) },
                            modifier = Modifier.testTag(edge.testTag),
                        )
                    }

                    HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp))

                    SectionHeader(text = stringResource(R.string.monitoring))
                    SwitchRow(
                        label = stringResource(R.string.monitoring),
                        description = stringResource(R.string.monitoring_description),
                        checked = preferences.monitoringEnabled,
                        onValueChange = onMonitoringEnabledChange,
                        modifier = Modifier.testTag(TestTags.SETTINGS_MONITORING_SWITCH),
                    )
                    if (preferences.monitoringEnabled) {
                        Text(
                            text = stringResource(R.string.monitoring_interval),
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.padding(top = 8.dp),
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

                    Text(
                        text = stringResource(R.string.settings_saved_note),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 16.dp, bottom = 8.dp),
                    )
                }
            }
        }
    }
}

@Composable
private fun SectionHeader(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleSmall,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(start = 4.dp, top = 8.dp, bottom = 4.dp),
    )
}

@Composable
private fun ThemeModeOption(
    mode: ThemeMode,
    selected: Boolean,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.medium)
            .selectable(
                selected = selected,
                onClick = onClick,
                role = Role.RadioButton,
            )
            .testTag(mode.testTag)
            .heightIn(min = 64.dp)
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        RadioButton(selected = selected, onClick = null)
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = stringResource(mode.labelRes()),
                style = MaterialTheme.typography.bodyLarge,
            )
            Text(
                text = stringResource(mode.descriptionRes()),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun DynamicColorOption(
    enabled: Boolean,
    onValueChange: (Boolean) -> Unit,
) {
    val available = isDynamicColorAvailable()
    val checked = available && enabled
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.medium)
            .toggleable(
                value = checked,
                enabled = available,
                role = Role.Switch,
                onValueChange = onValueChange,
            )
            .testTag(TestTags.DYNAMIC_COLOR_SWITCH)
            .heightIn(min = 64.dp)
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = stringResource(R.string.dynamic_color),
                style = MaterialTheme.typography.bodyLarge,
            )
            Text(
                text = stringResource(
                    if (available) {
                        R.string.dynamic_color_description
                    } else {
                        R.string.dynamic_color_unavailable
                    },
                ),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Spacer(modifier = Modifier.width(12.dp))
        Switch(
            checked = checked,
            onCheckedChange = null,
            enabled = available,
        )
    }
}

private val ThemeMode.testTag: String
    get() = when (this) {
        ThemeMode.SYSTEM -> TestTags.THEME_SYSTEM
        ThemeMode.LIGHT -> TestTags.THEME_LIGHT
        ThemeMode.DARK -> TestTags.THEME_DARK
    }

@Composable
private fun ChoiceRow(
    label: String,
    description: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.medium)
            .selectable(selected = selected, onClick = onClick, role = Role.RadioButton)
            .heightIn(min = 64.dp)
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        RadioButton(selected = selected, onClick = null)
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(text = label, style = MaterialTheme.typography.bodyLarge)
            Text(
                text = description,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun SwitchRow(
    label: String,
    description: String,
    checked: Boolean,
    onValueChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.medium)
            .toggleable(
                value = checked,
                role = Role.Switch,
                onValueChange = onValueChange,
            )
            .heightIn(min = 64.dp)
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(text = label, style = MaterialTheme.typography.bodyLarge)
            Text(
                text = description,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Spacer(modifier = Modifier.width(12.dp))
        Switch(checked = checked, onCheckedChange = null)
    }
}

@Composable
private fun MainCardLayout.labelRes(): Int = when (this) {
    MainCardLayout.STANDARD -> R.string.layout_standard
    MainCardLayout.FOCUS -> R.string.layout_focus
    MainCardLayout.MINIMAL -> R.string.layout_minimal
}

@Composable
private fun MainCardLayout.descriptionRes(): Int = when (this) {
    MainCardLayout.STANDARD -> R.string.layout_standard_description
    MainCardLayout.FOCUS -> R.string.layout_focus_description
    MainCardLayout.MINIMAL -> R.string.layout_minimal_description
}

@Composable
private fun HandleSize.labelRes(): Int = when (this) {
    HandleSize.COMPACT -> R.string.handle_size_compact
    HandleSize.NORMAL -> R.string.handle_size_normal
}

@Composable
private fun HandleSize.descriptionRes(): Int = when (this) {
    HandleSize.COMPACT -> R.string.handle_size_compact_description
    HandleSize.NORMAL -> R.string.handle_size_normal_description
}

@Composable
private fun HandleEdge.labelRes(): Int = when (this) {
    HandleEdge.LEFT -> R.string.handle_edge_left
    HandleEdge.RIGHT -> R.string.handle_edge_right
}

@Composable
private fun HandleEdge.descriptionRes(): Int = when (this) {
    HandleEdge.LEFT -> R.string.handle_edge_left_description
    HandleEdge.RIGHT -> R.string.handle_edge_right_description
}

private val MainCardLayout.testTag: String
    get() = when (this) {
        MainCardLayout.STANDARD -> TestTags.LAYOUT_STANDARD
        MainCardLayout.FOCUS -> TestTags.LAYOUT_FOCUS
        MainCardLayout.MINIMAL -> TestTags.LAYOUT_MINIMAL
    }

private val HandleSize.testTag: String
    get() = when (this) {
        HandleSize.COMPACT -> TestTags.HANDLE_SIZE_COMPACT
        HandleSize.NORMAL -> TestTags.HANDLE_SIZE_NORMAL
    }

private val HandleEdge.testTag: String
    get() = when (this) {
        HandleEdge.LEFT -> TestTags.HANDLE_EDGE_LEFT
        HandleEdge.RIGHT -> TestTags.HANDLE_EDGE_RIGHT
    }

private val MONITORING_INTERVALS = listOf(1, 2, 5, 10)
