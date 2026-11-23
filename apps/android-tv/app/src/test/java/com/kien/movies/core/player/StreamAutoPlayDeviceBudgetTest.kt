package com.kien.movies.core.player

import com.kien.movies.data.local.StreamAutoPlayMode
import com.kien.movies.data.local.StreamAutoPlaySource
import com.kien.movies.domain.model.Stream
import com.kien.movies.domain.model.StreamBehaviorHints
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class StreamAutoPlayDeviceBudgetTest {
    private val tooHeavy = torrent(
        addonName = "AddonA",
        name = "Remux",
        infoHash = "0123456789abcdef0123456789abcdef01234567"
    ).copy(exceedsDeviceBudget = true)
    private val fitting = torrent(
        addonName = "AddonB",
        name = "Encode",
        infoHash = "89abcdef0123456789abcdef0123456789abcdef"
    )

    @Test
    fun `a P2P stream over the device budget is skipped by autoplay`() {
        assertEquals(fitting, selectFirstStream(listOf(tooHeavy, fitting)))
        assertNull(selectFirstStream(listOf(tooHeavy)))
    }

    @Test
    fun `an early binge group match skips a P2P stream over the device budget`() {
        assertEquals(fitting, selectEarlyBingeGroupMatch(listOf(tooHeavy, fitting)))
        assertNull(selectEarlyBingeGroupMatch(listOf(tooHeavy)))
    }

    private fun selectEarlyBingeGroupMatch(streams: List<Stream>): Stream? = StreamAutoPlaySelector.selectEarlyBingeGroupMatch(streams, BINGE_GROUP)

    private fun selectFirstStream(streams: List<Stream>): Stream? = StreamAutoPlaySelector.selectAutoPlayStream(
        streams = streams,
        mode = StreamAutoPlayMode.FIRST_STREAM,
        regexPattern = "",
        source = StreamAutoPlaySource.ALL_SOURCES,
        installedAddonNames = setOf("AddonA", "AddonB"),
        selectedAddons = emptySet(),
        selectedPlugins = emptySet()
    )

    private fun torrent(addonName: String, name: String, infoHash: String): Stream = Stream(
        name = name,
        title = null,
        description = null,
        url = null,
        ytId = null,
        infoHash = infoHash,
        fileIdx = null,
        externalUrl = null,
        behaviorHints = StreamBehaviorHints(
            notWebReady = null,
            bingeGroup = BINGE_GROUP,
            countryWhitelist = null,
            proxyHeaders = null
        ),
        addonName = addonName,
        addonLogo = null
    )

    private companion object {
        const val BINGE_GROUP = "group-a"
    }
}
