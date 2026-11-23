package com.kien.movies.core.deeplink

import com.kien.movies.domain.deeplink.AppDeepLink
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class DeepLinkParserTest {
    @Test
    fun parsesMetaQueryDeepLink() {
        assertEquals(
            AppDeepLink.Meta(type = "series", id = "tt0944947"),
            DeepLinkParser.parse("movies://meta?type=series&id=tt0944947")
        )
    }

    @Test
    fun parsesPartyInviteDeepLink() {
        assertEquals(AppDeepLink.Party("ABC234"), DeepLinkParser.parse("movies://party/abc234"))
        assertEquals(
            AppDeepLink.Party("ABC234"),
            DeepLinkParser.parse("movies://party?code=ABC-234")
        )
        assertNull(DeepLinkParser.parse("movies://party/ABC10"))
    }

    @Test
    fun parsesAddonInstallDeepLink() {
        assertEquals(
            AppDeepLink.AddonInstall("https://free.nebulapro.xyz/sports/i/free/manifest.json"),
            DeepLinkParser.parse("movies://free.nebulapro.xyz/sports/i/free/manifest.json")
        )
    }

    @Test
    fun parsesStremioAddonInstallDeepLink() {
        assertEquals(
            AppDeepLink.AddonInstall("https://free.nebulapro.xyz/sports/i/free/manifest.json"),
            DeepLinkParser.parse("stremio://free.nebulapro.xyz/sports/i/free/manifest.json")
        )
    }

    @Test
    fun parsesDirectImdbDetailDeepLink() {
        assertEquals(
            AppDeepLink.Meta(type = "series", id = "tt0944947"),
            DeepLinkParser.parse("movies://series/tt0944947")
        )
    }

    @Test
    fun parsesProviderImdbDetailDeepLink() {
        assertEquals(
            AppDeepLink.Meta(type = "series", id = "tt0944947"),
            DeepLinkParser.parse("movies://imdb/series/tt0944947")
        )
    }

    @Test
    fun parsesProviderTmdbDetailDeepLink() {
        assertEquals(
            AppDeepLink.Meta(type = "series", id = "tmdb:1399"),
            DeepLinkParser.parse("movies://tmdb/tv/1399")
        )
    }

    @Test
    fun doesNotTreatAuthLinkAsAddonInstall() {
        assertNull(DeepLinkParser.parse("movies://auth/trakt?code=abc"))
    }

    @Test
    fun doesNotTreatNonHostStremioLinkAsAddonInstall() {
        assertNull(DeepLinkParser.parse("stremio://detail/series/tt0944947"))
    }
}
