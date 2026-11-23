package com.kien.movies

import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.isFocused
import androidx.compose.ui.test.junit4.createEmptyComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.kien.movies.debug.DebugLaunch
import com.kien.movies.streams.STREAMS_TITLE_TAG
import com.kien.movies.streams.StreamsDebugFixture
import com.kien.movies.streams.streamRowTag
import com.kien.movies.support.TvRobot
import org.junit.After
import org.junit.Assert.assertNotNull
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/** The Streams ACs (docs/android-tv-rebuild.md, section 6), with the remote's keys only. */
@RunWith(AndroidJUnit4::class)
class StreamsTest {
    @get:Rule
    val compose = createEmptyComposeRule()

    private val tv = TvRobot(compose)

    @After
    fun closeApp() = tv.close()

    /**
     * N4, the Streams step: Back from Streams puts focus back on the Details control that opened
     * it (the play button once A6 lands, the stub's button before). The fixture gives Streams a
     * row to focus without network.
     */
    @Test
    fun n4_backFromStreamsReturnsToTheControlThatOpenedIt() {
        tv.launch(DebugLaunch.ROUTE to "details/movie/tt0032138", DebugLaunch.STREAM_FIXTURE to true)
        tv.awaitFocus(hasClickAction(), TvRobot.DEFAULT_TIMEOUT_MS)
        val opener = focusedNodeMatcher()
        tv.select()
        tv.await(hasTestTag(STREAMS_TITLE_TAG))
        tv.awaitFocus(hasTestTag(streamRowTag(StreamsDebugFixture.DIRECT_KEY)), TvRobot.DEFAULT_TIMEOUT_MS)

        tv.back()

        tv.awaitFocus(opener)
    }

    /** Matches the focused node by its test tag, or by its text when it has no tag. */
    private fun focusedNodeMatcher(): SemanticsMatcher {
        val config = compose.onNode(isFocused()).fetchSemanticsNode().config
        config.getOrNull(SemanticsProperties.TestTag)?.let { tag -> return hasTestTag(tag) }
        val text = config.getOrNull(SemanticsProperties.Text)?.joinToString { line -> line.text }
        assertNotNull("The focused Details control has neither a tag nor a text", text)
        return hasText(text.orEmpty())
    }
}
