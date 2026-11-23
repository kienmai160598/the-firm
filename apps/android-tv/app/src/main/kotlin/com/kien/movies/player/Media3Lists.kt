package com.kien.movies.player

import androidx.annotation.OptIn
import androidx.media3.common.Tracks
import androidx.media3.common.text.Cue
import androidx.media3.common.text.CueGroup
import androidx.media3.common.util.UnstableApi

/**
 * The two media3 lists the player reads, typed as Guava `ImmutableList`. Guava is on the app's
 * runtime classpath (through the bridge) but not on its compile classpath, and the build file is
 * frozen for this slice, so the lists are read through reflection here, in one place. Once the
 * app declares `implementation(libs.guava)`, replace these with `tracks.groups` and
 * `cueGroup.cues`. The release keep rule for `androidx.media3.**` keeps both members.
 */
internal object Media3Lists {
    private val groupsGetter = Tracks::class.java.getMethod("getGroups")
    private val cuesField = CueGroup::class.java.getField("cues")

    @Suppress("UNCHECKED_CAST")
    fun groups(tracks: Tracks): List<Tracks.Group> = groupsGetter.invoke(tracks) as List<Tracks.Group>

    @OptIn(UnstableApi::class)
    @Suppress("UNCHECKED_CAST")
    fun cues(cueGroup: CueGroup): List<Cue> = cuesField.get(cueGroup) as List<Cue>
}
