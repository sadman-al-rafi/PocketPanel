package dev.pocketpanel.ui.main

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import dev.pocketpanel.R
import dev.pocketpanel.ui.TestTags
import dev.pocketpanel.ui.VpnUiState
import dev.pocketpanel.vpn.ConfigError
import dev.pocketpanel.vpn.VpnConnectionState
import dev.pocketpanel.vpn.VpnError
import dev.pocketpanel.vpn.VpnImportResult

/**
 * WireGuard profile import and tunnel control.
 *
 * The connect action is delegated to the Activity because the system
 * consent dialog must be launched for a result.
 */
@Composable
fun VpnCard(
    vpn: VpnUiState,
    onImportConfig: (Uri) -> Unit,
    onConnect: () -> Unit,
    onDisconnect: () -> Unit,
    onRemove: () -> Unit,
    onDismissMessage: () -> Unit,
) {
    val importLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument(),
    ) { uri ->
        if (uri != null) onImportConfig(uri)
    }

    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                text = stringResource(R.string.vpn),
                style = MaterialTheme.typography.titleMedium,
            )
            Text(
                text = stringResource(R.string.vpn_description),
                style = MaterialTheme.typography.bodyMedium,
            )

            val profile = vpn.profile
            if (profile == null) {
                Text(
                    text = stringResource(R.string.vpn_no_profile),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            } else {
                Text(
                    text = profile.name,
                    style = MaterialTheme.typography.titleSmall,
                )
                profile.endpointSummary?.let { endpoint ->
                    Text(
                        text = stringResource(R.string.vpn_endpoint, endpoint),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                profile.addressSummary?.let { address ->
                    Text(
                        text = stringResource(R.string.vpn_address, address),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            HorizontalDivider()

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = connectionLabel(vpn.connection),
                    style = MaterialTheme.typography.bodyLarge,
                    color = connectionColor(vpn.connection),
                    modifier = Modifier
                        .weight(1f)
                        .testTag(TestTags.VPN_STATUS),
                )
                Spacer(modifier = Modifier.width(12.dp))
                when (vpn.connection) {
                    VpnConnectionState.Connected -> OutlinedButton(
                        onClick = onDisconnect,
                        modifier = Modifier.testTag(TestTags.VPN_CONNECT_BUTTON),
                    ) {
                        Text(text = stringResource(R.string.vpn_disconnect))
                    }

                    VpnConnectionState.Connecting -> Button(
                        onClick = {},
                        enabled = false,
                        modifier = Modifier.testTag(TestTags.VPN_CONNECT_BUTTON),
                    ) {
                        Text(text = stringResource(R.string.vpn_connecting))
                    }

                    else -> Button(
                        onClick = onConnect,
                        enabled = profile != null,
                        modifier = Modifier.testTag(TestTags.VPN_CONNECT_BUTTON),
                    ) {
                        Text(text = stringResource(R.string.vpn_connect))
                    }
                }
            }

            vpn.importResult?.let { result ->
                val message = importResultMessage(result)
                if (message != null) {
                    Text(
                        text = message,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error,
                    )
                    TextButton(onClick = onDismissMessage) {
                        Text(text = stringResource(R.string.vpn_dismiss))
                    }
                }
            }

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                TextButton(
                    onClick = { importLauncher.launch(arrayOf("*/*")) },
                    modifier = Modifier.testTag(TestTags.VPN_IMPORT_BUTTON),
                ) {
                    Text(
                        text = stringResource(
                            if (profile == null) R.string.vpn_import else R.string.vpn_replace,
                        ),
                    )
                }
                if (profile != null) {
                    TextButton(
                        onClick = onRemove,
                        modifier = Modifier.testTag(TestTags.VPN_REMOVE_BUTTON),
                    ) {
                        Text(text = stringResource(R.string.vpn_remove))
                    }
                }
            }

            Text(
                text = stringResource(R.string.vpn_privacy_note),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun connectionLabel(state: VpnConnectionState): String = when (state) {
    VpnConnectionState.Disconnected -> stringResource(R.string.vpn_disconnected)
    VpnConnectionState.Connecting -> stringResource(R.string.vpn_connecting)
    VpnConnectionState.Connected -> stringResource(R.string.vpn_connected)
    is VpnConnectionState.Failed -> vpnErrorLabel(state.error)
}

@Composable
private fun connectionColor(state: VpnConnectionState): Color = when (state) {
    VpnConnectionState.Connected -> MaterialTheme.colorScheme.primary
    is VpnConnectionState.Failed -> MaterialTheme.colorScheme.error
    else -> MaterialTheme.colorScheme.onSurfaceVariant
}

@Composable
private fun vpnErrorLabel(error: VpnError): String = stringResource(
    when (error) {
        VpnError.NO_PROFILE -> R.string.vpn_error_no_profile
        VpnError.INVALID_PROFILE -> R.string.vpn_error_invalid_profile
        VpnError.PROFILE_UNREADABLE -> R.string.vpn_error_unreadable_profile
        VpnError.NOT_AUTHORIZED -> R.string.vpn_error_not_authorized
        VpnError.ACTIVATION_FAILED -> R.string.vpn_error_activation
        VpnError.UNKNOWN -> R.string.vpn_error_unknown
    },
)

@Composable
private fun importResultMessage(result: VpnImportResult): String? = when (result) {
    VpnImportResult.Success -> null
    VpnImportResult.UnreadableFile -> stringResource(R.string.vpn_error_unreadable_file)
    VpnImportResult.StorageFailed -> stringResource(R.string.vpn_error_storage)
    is VpnImportResult.Invalid -> configErrorLabel(result.reason)
}

@Composable
private fun configErrorLabel(error: ConfigError): String = stringResource(
    when (error) {
        ConfigError.EMPTY -> R.string.vpn_error_empty
        ConfigError.TOO_LARGE -> R.string.vpn_error_too_large
        ConfigError.MISSING_INTERFACE -> R.string.vpn_error_missing_interface
        ConfigError.MISSING_PRIVATE_KEY -> R.string.vpn_error_missing_private_key
        ConfigError.MISSING_PEER -> R.string.vpn_error_missing_peer
        ConfigError.MISSING_PUBLIC_KEY -> R.string.vpn_error_missing_public_key
        ConfigError.MISSING_ADDRESS -> R.string.vpn_error_missing_address
        ConfigError.MISSING_ENDPOINT -> R.string.vpn_error_missing_endpoint
        ConfigError.INVALID_KEY -> R.string.vpn_error_invalid_key
        ConfigError.INVALID_VALUE -> R.string.vpn_error_invalid_value
        ConfigError.SYNTAX -> R.string.vpn_error_syntax
        ConfigError.UNKNOWN_ATTRIBUTE -> R.string.vpn_error_unknown_attribute
        ConfigError.OTHER -> R.string.vpn_error_other
    },
)
