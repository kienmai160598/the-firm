package com.kien.movies.settings

import com.nuvio.tvos.TvosTraktFailure
import com.nuvio.tvos.TvosTraktPhase
import com.nuvio.tvos.TvosTraktState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class TraktSummaryTest {
    @Test
    fun aSignedInAccountShowsItsUsername() {
        val state = TvosTraktState(phase = TvosTraktPhase.SIGNED_IN, username = "kien")

        assertEquals("kien", state.summary)
    }

    @Test
    fun aBuildWithoutCredentialsSaysNotAvailable() {
        assertEquals(NOT_AVAILABLE, TvosTraktState(phase = TvosTraktPhase.NOT_CONFIGURED).summary)
    }

    @Test
    fun aSignInInProgressStillReadsNotSignedIn() {
        val waiting = TvosTraktState(phase = TvosTraktPhase.WAITING_FOR_APPROVAL, userCode = "ABCD1234")

        assertEquals("Not Signed In", waiting.summary)
    }

    @Test
    fun onlyAFailedSignInHasAFailureText() {
        val denied = TvosTraktState(phase = TvosTraktPhase.SIGNED_OUT, failure = TvosTraktFailure.DENIED)

        assertNull(TvosTraktState(phase = TvosTraktPhase.SIGNED_OUT).failureText)
        assertEquals("Access was denied on Trakt.", denied.failureText)
    }
}
