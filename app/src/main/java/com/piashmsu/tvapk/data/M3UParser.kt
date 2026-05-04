package com.piashmsu.tvapk.data

/**
 * Minimal M3U / M3U8 (extended) playlist parser used to build the live-TV
 * channel list. Supports the standard `#EXTINF:` directives produced by
 * common IPTV provisioning tools (tvg-id, tvg-name, tvg-logo, group-title,
 * tvg-language, tvg-country).
 *
 * The parser is deliberately lenient — malformed lines are skipped rather
 * than aborting the whole import.
 */
object M3UParser {

    private val ATTR_REGEX = Regex("""([a-zA-Z0-9_-]+)="([^"]*)"""")

    fun parse(content: String): List<Channel> {
        val out = mutableListOf<Channel>()
        val lines = content.lineSequence().map { it.trim() }.filter { it.isNotEmpty() }.toList()

        var pendingName: String? = null
        var pendingAttrs: Map<String, String> = emptyMap()

        var index = 0
        for (raw in lines) {
            if (raw.startsWith("#EXTM3U")) continue
            if (raw.startsWith("#EXTINF")) {
                val (attrs, name) = parseExtInf(raw)
                pendingAttrs = attrs
                pendingName = name
            } else if (!raw.startsWith("#") && pendingName != null) {
                val name = pendingName
                val attrs = pendingAttrs
                out += Channel(
                    id = attrs["tvg-id"]?.takeIf { it.isNotBlank() }
                        ?: "ch-${index++}-${name.hashCode()}",
                    name = attrs["tvg-name"]?.takeIf { it.isNotBlank() } ?: name,
                    logo = attrs["tvg-logo"]?.takeIf { it.isNotBlank() },
                    group = attrs["group-title"]?.takeIf { it.isNotBlank() } ?: "Uncategorized",
                    streamUrl = raw,
                    tvgId = attrs["tvg-id"],
                    language = attrs["tvg-language"],
                    country = attrs["tvg-country"],
                )
                pendingName = null
                pendingAttrs = emptyMap()
            }
        }
        return out
    }

    private fun parseExtInf(line: String): Pair<Map<String, String>, String> {
        // Format: #EXTINF:<duration> [attr="val" ...],<display name>
        val afterColon = line.substringAfter(':', "")
        val commaIdx = afterColon.indexOf(',')
        val head = if (commaIdx >= 0) afterColon.substring(0, commaIdx) else afterColon
        val name = if (commaIdx >= 0) afterColon.substring(commaIdx + 1).trim() else ""
        val attrs = ATTR_REGEX.findAll(head).associate { m ->
            m.groupValues[1].lowercase() to m.groupValues[2]
        }
        return attrs to name
    }
}
