package com.nuvio.tvos.rules

/**
 * What a stream row shows, read from the free text an addon puts in a stream's name, title,
 * description and file name (apps/apple-tv/Sources/StreamFeatures.swift; tvOS keeps its Swift
 * copy until a later slice switches it to this one).
 *
 * [capabilities] holds dynamic range, audio, channels, codec, bit depth and source, in that order,
 * then the addon's own badges that are not the resolution, without duplicates (case blind).
 */
data class StreamFeatures(
    val resolution: String?,
    val size: String?,
    val seeders: Int?,
    val capabilities: List<String>,
) {
    companion object {
        /** [providedSize] from the addon wins over a size found in [texts]. */
        fun parse(
            texts: List<String>,
            providedBadges: List<String>,
            providedSize: String?,
        ): StreamFeatures {
            val haystack = texts.joinToString(separator = " \n ")
            val resolution = firstMatch(haystack, resolutionRules)
            val found = dynamicRanges(haystack) +
                listOfNotNull(
                    firstMatch(haystack, audioRules),
                    firstMatch(haystack, channelRules),
                    firstMatch(haystack, codecRules),
                    firstMatch(haystack, bitDepthRules),
                    firstMatch(haystack, sourceRules),
                )
            return StreamFeatures(
                resolution = resolution,
                size = providedSize ?: size(haystack),
                seeders = seeders(haystack),
                capabilities = unique(found + providedBadges.filter { badge -> badge != resolution }),
            )
        }

        private val resolutionRules = rules(
            """\b(2160p|4k|uhd)\b""" to "4K",
            """\b1440p\b""" to "1440p",
            """\b(1080p|fhd)\b""" to "1080p",
            """\b720p\b""" to "720p",
            """\b(576p|480p|360p)\b""" to "SD",
        )

        private val dynamicRangeRules = rules(
            """\b(dolby ?vision|dovi|dv)\b""" to "Dolby Vision",
            """\bhdr10(\+|plus)""" to "HDR10+",
            """\bhdr10\b(?!\+)""" to "HDR10",
            """\bhdr\b(?!10)""" to "HDR",
            """\bhlg\b""" to "HLG",
        )

        private val audioRules = rules(
            """\batmos\b""" to "Dolby Atmos",
            """\btrue ?hd\b""" to "TrueHD",
            """\bdts[ -:]?x\b""" to "DTS:X",
            """\bdts[ -]?hd""" to "DTS-HD",
            """\bdts\b""" to "DTS",
            """\b(ddp|dd\+|e-?ac-?3|dolby digital plus)""" to "Dolby Digital+",
            """\b(dd|ac-?3|dolby digital)\b""" to "Dolby Digital",
            """\bflac\b""" to "FLAC",
            """\bopus\b""" to "Opus",
            """\baac\b""" to "AAC",
        )

        private val channelRules = rules(
            """(?<!\d)7[ .]1(?!\d|\s?[KMGT]i?B)""" to "7.1",
            """(?<!\d)5[ .]1(?!\d|\s?[KMGT]i?B)""" to "5.1",
            """(?<!\d)2[ .]0(?!\d|\s?[KMGT]i?B)""" to "Stereo",
        )

        private val codecRules = rules(
            """\b(hevc|x265|h\.?265)\b""" to "HEVC",
            """\bav1\b""" to "AV1",
            """\b(avc|x264|h\.?264)\b""" to "H.264",
        )

        private val bitDepthRules = rules(
            """\b10[ -]?bit\b""" to "10-bit",
        )

        private val sourceRules = rules(
            """\bremux\b""" to "REMUX",
            """\b(blu-?ray|bdrip|brrip)\b""" to "Blu-ray",
            """\bweb-?dl\b""" to "WEB-DL",
            """\bweb-?rip\b""" to "WEBRip",
            """\bhdtv\b""" to "HDTV",
            """\b(cam|hdcam|telesync)\b""" to "CAM",
        )

        private val sizePattern = Regex(
            """(\d+(?:[.,]\d+)?)\s?(TB|GB|MB|TiB|GiB|MiB)\b""",
            RegexOption.IGNORE_CASE,
        )

        private val seedersPattern = Regex(
            """(?:👤|seeders?:?|seeds?:?)\s?(\d+)""",
            RegexOption.IGNORE_CASE,
        )

        private class Rule(val pattern: Regex, val label: String)

        private fun rules(vararg pairs: Pair<String, String>): List<Rule> =
            pairs.map { (pattern, label) -> Rule(Regex(pattern, RegexOption.IGNORE_CASE), label) }

        private fun dynamicRanges(text: String): List<String> {
            val found = dynamicRangeRules.filter { rule -> rule.pattern.containsMatchIn(text) }
                .map(Rule::label)
            val hasNamedHdr = found.any { label -> label.startsWith("HDR10") }
            return if (hasNamedHdr) found.filter { label -> label != "HDR" } else found
        }

        private fun firstMatch(text: String, rules: List<Rule>): String? =
            rules.firstOrNull { rule -> rule.pattern.containsMatchIn(text) }?.label

        private fun size(text: String): String? {
            val groups = sizePattern.find(text)?.groupValues ?: return null
            val amount = groups[1].replace(",", ".")
            val unit = groups[2].uppercase().replace("I", "")
            return "$amount $unit"
        }

        private fun seeders(text: String): Int? =
            seedersPattern.find(text)?.groupValues?.get(1)?.toIntOrNull()

        private fun unique(labels: List<String>): List<String> {
            val seen = mutableSetOf<String>()
            return labels.filter { label -> seen.add(label.lowercase()) }
        }
    }
}
