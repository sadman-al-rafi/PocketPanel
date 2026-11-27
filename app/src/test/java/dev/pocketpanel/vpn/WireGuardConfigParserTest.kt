package dev.pocketpanel.vpn

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** Tests the local validation layer over the official WireGuard parser. */
class WireGuardConfigParserTest {

    @Test
    fun validConfigurationReturnsOnlyNonSecretSummary() {
        val result = WireGuardConfigParser.parse(validConfig(), "home.conf")

        val valid = result as? ConfigParseResult.Valid
            ?: throw AssertionError("Expected a valid configuration, got $result")
        assertEquals("home.conf", valid.summary.name)
        assertEquals("10.0.0.2/32", valid.summary.addressSummary)
        assertEquals("203.0.113.5:51820", valid.summary.endpointSummary)
        assertFalse(valid.summary.toString().contains(PRIVATE_KEY))
    }

    @Test
    fun blankConfigurationIsRejected() {
        assertError("", ConfigError.EMPTY)
        assertError("   \n  ", ConfigError.EMPTY)
    }

    @Test
    fun oversizedConfigurationIsRejected() {
        val huge = "x".repeat(WireGuardConfigParser.MAX_CONFIG_CHARS + 1)
        assertError(huge, ConfigError.TOO_LARGE)
    }

    @Test
    fun missingInterfaceSectionIsRejected() {
        val text = "[Peer]\nPublicKey = $PUBLIC_KEY\nAllowedIPs = 0.0.0.0/0\n"
        assertError(text, ConfigError.MISSING_INTERFACE)
    }

    @Test
    fun missingPrivateKeyIsRejected() {
        val text = validConfig(interfaceLines = "Address = 10.0.0.2/32\n")
        assertError(text, ConfigError.MISSING_PRIVATE_KEY)
    }

    @Test
    fun missingPeerSectionIsRejected() {
        val text = "[Interface]\nPrivateKey = $PRIVATE_KEY\nAddress = 10.0.0.2/32\n"
        assertError(text, ConfigError.MISSING_PEER)
    }

    @Test
    fun missingPublicKeyIsRejected() {
        val text = validConfig(peerLines = "AllowedIPs = 0.0.0.0/0\n")
        assertError(text, ConfigError.MISSING_PUBLIC_KEY)
    }

    @Test
    fun missingAddressIsRejected() {
        val text = validConfig(
            interfaceLines = "PrivateKey = $PRIVATE_KEY\nDNS = 1.1.1.1\n",
        )
        assertError(text, ConfigError.MISSING_ADDRESS)
    }

    @Test
    fun missingEndpointIsRejected() {
        val text = validConfig(peerLines = "PublicKey = $PUBLIC_KEY\nAllowedIPs = 0.0.0.0/0\n")
        assertError(text, ConfigError.MISSING_ENDPOINT)
    }

    @Test
    fun invalidKeyIsRejectedWithoutEchoingIt() {
        val secret = "not-a-valid-wireguard-key"
        val text = validConfig(interfaceLines = "PrivateKey = $secret\nAddress = 10.0.0.2/32\n")

        val result = WireGuardConfigParser.parse(text, "broken.conf")
        val invalid = result as? ConfigParseResult.Invalid
            ?: throw AssertionError("Expected an invalid configuration, got $result")
        assertEquals(ConfigError.INVALID_KEY, invalid.reason)
        assertFalse(invalid.toString().contains(secret))
    }

    @Test
    fun unknownAttributeIsRejected() {
        val text = validConfig(
            interfaceLines = "$PRIVATE_KEY_LINE\nAddress = 10.0.0.2/32\nBogusSetting = 1\n",
        )
        assertError(text, ConfigError.UNKNOWN_ATTRIBUTE)
    }

    @Test
    fun malformedLineIsRejectedAsSyntaxError() {
        val text = validConfig(
            interfaceLines = "$PRIVATE_KEY_LINE\nAddress = 10.0.0.2/32\nthis is not an attribute\n",
        )
        assertError(text, ConfigError.SYNTAX)
    }

    @Test
    fun commentsAndMultiplePeersAreAccepted() {
        val text = """
            # Imported from a wg-quick file
            [Interface]
            PrivateKey = $PRIVATE_KEY
            Address = 10.0.0.2/32

            [Peer]
            PublicKey = $PUBLIC_KEY
            Endpoint = 203.0.113.5:51820
            AllowedIPs = 0.0.0.0/0

            [Peer]
            PublicKey = $SECOND_PUBLIC_KEY
            Endpoint = 203.0.113.6:51820
            AllowedIPs = ::/0
        """.trimIndent()

        val result = WireGuardConfigParser.parse(text, "multi")
        assertTrue(
            "Expected a valid configuration, got $result",
            result is ConfigParseResult.Valid,
        )
    }

    private fun assertError(text: String, expected: ConfigError) {
        val result = WireGuardConfigParser.parse(text, "test")
        val invalid = result as? ConfigParseResult.Invalid
            ?: throw AssertionError("Expected $expected, got $result")
        assertEquals(expected, invalid.reason)
    }

    private fun validConfig(
        interfaceLines: String = PRIVATE_KEY_LINE + "Address = 10.0.0.2/32\nDNS = 1.1.1.1\n",
        peerLines: String = "PublicKey = $PUBLIC_KEY\nAllowedIPs = 0.0.0.0/0\nEndpoint = 203.0.113.5:51820\n",
    ): String = "[Interface]\n$interfaceLines\n[Peer]\n$peerLines"

    private companion object {
        const val PRIVATE_KEY = "AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA="
        const val PUBLIC_KEY = "AQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQE="
        const val SECOND_PUBLIC_KEY = "AgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgI="
        const val PRIVATE_KEY_LINE = "PrivateKey = " + PRIVATE_KEY + "\n"
    }
}
