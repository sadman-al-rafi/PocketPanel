package dev.pocketpanel.vpn

/**
 * Non-secret description of the imported configuration.
 *
 * It is safe to keep in memory, DataStore or UI state. The WireGuard
 * private key is never part of this model.
 */
data class VpnProfileSummary(
    val name: String,
    val addressSummary: String?,
    val endpointSummary: String?,
)

/** Result of importing a configuration file. */
sealed interface VpnImportResult {
    data object Success : VpnImportResult
    data class Invalid(val reason: ConfigError) : VpnImportResult
    data object UnreadableFile : VpnImportResult
    data object StorageFailed : VpnImportResult
}

/** Connection state shown in the UI. */
sealed interface VpnConnectionState {
    data object Disconnected : VpnConnectionState
    data object Connecting : VpnConnectionState
    data object Connected : VpnConnectionState
    data class Failed(val error: VpnError) : VpnConnectionState
}

/** User-facing connection failures. */
enum class VpnError {
    NO_PROFILE,
    INVALID_PROFILE,
    PROFILE_UNREADABLE,
    NOT_AUTHORIZED,
    ACTIVATION_FAILED,
    UNKNOWN,
}

/** Why a configuration was rejected. */
enum class ConfigError {
    EMPTY,
    TOO_LARGE,
    MISSING_INTERFACE,
    MISSING_PRIVATE_KEY,
    MISSING_PEER,
    MISSING_PUBLIC_KEY,
    MISSING_ADDRESS,
    MISSING_ENDPOINT,
    INVALID_KEY,
    INVALID_VALUE,
    SYNTAX,
    UNKNOWN_ATTRIBUTE,
    OTHER,
}
