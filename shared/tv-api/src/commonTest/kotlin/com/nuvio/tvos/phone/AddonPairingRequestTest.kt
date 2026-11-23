package com.nuvio.tvos.phone

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertTrue

class AddonPairingRequestTest {
    private val token = AddonPairingToken.fromRandomBytes(ByteArray(16) { index -> (index * 17).toByte() })

    @Test
    fun theTokenIs32HexCharactersAndMatchesOnlyItself() {
        val value = token.value

        assertEquals("00112233445566778899aabbccddeeff", value)
        assertTrue(token.matches("00112233445566778899aabbccddeeff"))
        assertFalse(token.matches("00112233445566778899aabbccddeefe"))
        assertFalse(token.matches("00112233445566778899aabbccddeeff0"))
        assertFalse(token.matches("00112233"))
        assertFalse(token.matches(""))
        assertFalse(token.matches(null))
    }

    @Test
    fun everyPathIsForbiddenWithoutTheRightToken() {
        val paths = listOf("GET" to "/", "POST" to "/api/addons", "GET" to "/api/status/1", "GET" to "/nothing")

        val routes = paths.flatMap { (method, path) ->
            listOf(addonPairingRoute(method, path, null, token), addonPairingRoute(method, path, "wrong", token))
        }

        assertEquals(List(8) { AddonPairingRoute.Forbidden }, routes)
    }

    @Test
    fun theRightTokenReachesPageProposalAndStatus() {
        val right = token.value

        val routes = listOf(
            addonPairingRoute("GET", "/", right, token),
            addonPairingRoute("POST", "/api/addons", right, token),
            addonPairingRoute("GET", "/api/status/12", right, token),
            addonPairingRoute("GET", "/api/addons", right, token),
            addonPairingRoute("GET", "/api/status/abc", right, token)
        )

        assertEquals(
            listOf(
                AddonPairingRoute.Page,
                AddonPairingRoute.Propose,
                AddonPairingRoute.Status(12),
                AddonPairingRoute.NotFound,
                AddonPairingRoute.NotFound
            ),
            routes
        )
    }

    @Test
    fun aBodyWithoutALengthOrOver16KbIsRefusedBeforeItIsRead() {
        assertEquals(AddonBodyProblem.MissingLength, addonBodyProblem(null))
        assertEquals(AddonBodyProblem.TooLarge, addonBodyProblem(16_385))
        assertEquals(null, addonBodyProblem(16_384))
    }

    @Test
    fun urlsAreTrimmedAndRepeatsAndBlanksDropped() {
        val body = """{"urls": [" https://a.example/manifest.json ", "", "https://a.example/manifest.json", "stremio://b.example/manifest.json"]}"""

        val parsed = parseAddonProposal(body)

        assertEquals(AddonProposalParse.Accepted(listOf("https://a.example/manifest.json", "stremio://b.example/manifest.json")), parsed)
    }

    @Test
    fun proposalsOutsideTheLimitsAreRejected() {
        val elevenUrls = (1..11).joinToString(prefix = """{"urls": [""", postfix = "]}") { index -> "\"https://a.example/$index\"" }
        val longUrl = "https://a.example/" + "x".repeat(2_048)
        val bodies = listOf(
            elevenUrls,
            """{"urls": ["$longUrl"]}""",
            """{"urls": ["ftp://a.example/manifest.json"]}""",
            """{"urls": ["javascript:alert(1)"]}""",
            """{"urls": ["https://a.example/with space"]}""",
            """{"urls": []}""",
            """{"urls": [42]}""",
            "https://a.example/manifest.json"
        )

        val results = bodies.map(::parseAddonProposal)

        results.forEach { result -> assertIs<AddonProposalParse.Rejected>(result) }
    }
}
