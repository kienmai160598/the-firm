package com.nuvio.tvos.phone

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertTrue

class AddonPairingSessionTest {
    @Test
    fun aSecondProposalWhileOneIsOpenOnTheTvIsAConflict() {
        val proposed = waiting().propose(listOf(URL_A), nowMillis = 1).first

        val (afterSecond, result) = proposed.propose(listOf(URL_B), nowMillis = 2)

        assertEquals(AddonProposeResult.Conflict, result)
        assertEquals(proposed.phase, afterSecond.phase)
    }

    @Test
    fun aProposalWhileTheFirstIsInstallingIsAConflict() {
        val installing = waiting().propose(listOf(URL_A), nowMillis = 1).first.confirm(proposalId = 1)

        val result = installing.propose(listOf(URL_B), nowMillis = 2).second

        assertEquals(AddonProposeResult.Conflict, result)
    }

    @Test
    fun confirmedProposalReportsItsOutcomesAndTheSessionTakesTheNextOne() {
        val outcomes = listOf(AddonInstallOutcome.Installed(URL_A, "Cinemeta"), AddonInstallOutcome.Failed(URL_B, "Timed out"))
        val installing = waiting().propose(listOf(URL_A, URL_B), nowMillis = 1).first.confirm(proposalId = 1)

        val done = installing.finish(proposalId = 1, outcomes = outcomes)

        assertEquals(AddonProposalStatus.Done(outcomes), done.status(proposalId = 1))
        assertIs<AddonProposeResult.Accepted>(done.propose(listOf(URL_A), nowMillis = 2).second)
    }

    @Test
    fun cancelledProposalStaysReadableForThePhoneAfterTheSessionWaitsAgain() {
        val proposed = waiting().propose(listOf(URL_A), nowMillis = 1).first

        val cancelled = proposed.cancel(proposalId = 1)

        assertEquals(AddonPairingPhase.Waiting, cancelled.phase)
        assertEquals(AddonProposalStatus.Cancelled, cancelled.status(proposalId = 1))
    }

    @Test
    fun confirmOfAnotherProposalChangesNothing() {
        val proposed = waiting().propose(listOf(URL_A), nowMillis = 1).first

        val after = proposed.confirm(proposalId = 7)

        assertEquals(proposed, after)
    }

    @Test
    fun aStoppedSessionTakesNoProposal() {
        val stopped = waiting().stop()

        val result = stopped.propose(listOf(URL_A), nowMillis = 1).second

        assertEquals(AddonProposeResult.Closed, result)
    }

    @Test
    fun theSessionIsIdleTenMinutesAfterTheLastRequest() {
        val touched = waiting().touch(nowMillis = 5_000)

        val justBefore = touched.isIdle(nowMillis = 5_000 + 599_999)
        val atTheLimit = touched.isIdle(nowMillis = 5_000 + 600_000)

        assertFalse(justBefore)
        assertTrue(atTheLimit)
    }

    private fun waiting() = AddonPairingSession().start(nowMillis = 0)

    private companion object {
        const val URL_A = "https://v3-cinemeta.strem.io/manifest.json"
        const val URL_B = "https://example.org/manifest.json"
    }
}
