package com.nuvio.tvos

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

class TvosTraktDeviceFlowTest {
    @Test
    fun anApprovedPollCarriesTheTokens() {
        val body =
            """{"access_token":"a","refresh_token":"r","token_type":"bearer",""" +
                """"expires_in":7776000,"created_at":1700000000,"scope":"public"}"""

        val poll = traktDevicePoll(200, body)

        val token = assertIs<TraktDevicePoll.Approved>(poll).token
        assertEquals(TraktDeviceToken("a", "r", "bearer", 7_776_000, 1_700_000_000), token)
    }

    @Test
    fun pendingAndServerErrorsKeepPolling() {
        assertEquals(TraktDevicePoll.Pending, traktDevicePoll(400, ""))
        assertEquals(TraktDevicePoll.Pending, traktDevicePoll(503, ""))
    }

    @Test
    fun slowDownAddsFiveSeconds() {
        assertEquals(TraktDevicePoll.SlowDown, traktDevicePoll(429, ""))
        assertEquals(10, traktSlowerInterval(5))
    }

    @Test
    fun expiredDeniedAndInvalidCodesStop() {
        assertEquals(TraktDevicePoll.Stopped(TvosTraktFailure.EXPIRED), traktDevicePoll(410, ""))
        assertEquals(TraktDevicePoll.Stopped(TvosTraktFailure.DENIED), traktDevicePoll(418, ""))
        assertEquals(TraktDevicePoll.Stopped(TvosTraktFailure.FAILED), traktDevicePoll(404, ""))
        assertEquals(TraktDevicePoll.Stopped(TvosTraktFailure.FAILED), traktDevicePoll(409, ""))
    }

    @Test
    fun anApprovalWithoutTokensFails() {
        assertEquals(TraktDevicePoll.Stopped(TvosTraktFailure.FAILED), traktDevicePoll(200, "{}"))
    }

    @Test
    fun theDeviceCodeAnswerParses() {
        val body =
            """{"device_code":"d","user_code":"5055CC52","verification_url":"https://trakt.tv/activate",""" +
                """"expires_in":600,"interval":5}"""

        val code = traktJson.decodeFromString<TraktDeviceCode>(body)

        assertEquals(TraktDeviceCode("d", "5055CC52", "https://trakt.tv/activate", 600, 5), code)
    }
}
