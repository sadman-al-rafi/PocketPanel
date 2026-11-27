package dev.pocketpanel.vpn

import com.wireguard.config.BadConfigException
import com.wireguard.config.BadConfigException.Location
import com.wireguard.config.BadConfigException.Reason
import com.wireguard.config.Config
import java.io.IOException
import java.io.StringReader

/** Outcome of parsing and validating an imported configuration. */
sealed interface ConfigParseResult {
    data class Valid(val summary: VpnProfileSummary) : ConfigParseResult
    data class Invalid(val reason: ConfigError) : ConfigParseResult
}

/**
 * Parses a wg-quick configuration with the official WireGuard parser and
 * adds the checks a mobile client needs.
 *
 * Errors are reduced to an enum. The exception message, offending line
 * and private key are never propagated, so a key cannot leak through an
 * error dialog or a log entry.
 */
object WireGuardConfigParser {

    /** A configuration file is a small text file; larger input is rejected. */
    const val MAX_CONFIG_CHARS = 256 * 1024

    fun parse(text: String, name: String): ConfigParseResult {
        if (text.isBlank()) {
            return ConfigParseResult.Invalid(ConfigError.EMPTY)
        }
        if (text.length > MAX_CONFIG_CHARS) {
            return ConfigParseResult.Invalid(ConfigError.TOO_LARGE)
        }

        val config = try {
            Config.parse(StringReader(text).buffered())
        } catch (error: BadConfigException) {
            return ConfigParseResult.Invalid(error.toConfigError())
        } catch (error: IOException) {
            return ConfigParseResult.Invalid(ConfigError.OTHER)
        } catch (error: IllegalArgumentException) {
            return ConfigParseResult.Invalid(ConfigError.MISSING_INTERFACE)
        } catch (error: RuntimeException) {
            return ConfigParseResult.Invalid(ConfigError.OTHER)
        }

        val interfaze = config.`interface`
        if (interfaze.addresses.isEmpty()) {
            return ConfigParseResult.Invalid(ConfigError.MISSING_ADDRESS)
        }
        val peer = config.peers.firstOrNull()
            ?: return ConfigParseResult.Invalid(ConfigError.MISSING_PEER)
        val endpoint = peer.endpoint.orElse(null)
            ?: return ConfigParseResult.Invalid(ConfigError.MISSING_ENDPOINT)

        return ConfigParseResult.Valid(
            VpnProfileSummary(
                name = name,
                addressSummary = interfaze.addresses.first().toString(),
                endpointSummary = "${endpoint.host}:${endpoint.port}",
            ),
        )
    }

    private fun BadConfigException.toConfigError(): ConfigError = when (reason) {
        Reason.MISSING_SECTION -> ConfigError.MISSING_INTERFACE
        Reason.MISSING_ATTRIBUTE -> when (location) {
            Location.PRIVATE_KEY -> ConfigError.MISSING_PRIVATE_KEY
            Location.PUBLIC_KEY -> ConfigError.MISSING_PUBLIC_KEY
            else -> ConfigError.OTHER
        }
        Reason.INVALID_KEY -> ConfigError.INVALID_KEY
        Reason.INVALID_NUMBER, Reason.INVALID_VALUE -> ConfigError.INVALID_VALUE
        Reason.SYNTAX_ERROR -> ConfigError.SYNTAX
        Reason.UNKNOWN_ATTRIBUTE, Reason.UNKNOWN_SECTION -> ConfigError.UNKNOWN_ATTRIBUTE
        else -> ConfigError.OTHER
    }
}
