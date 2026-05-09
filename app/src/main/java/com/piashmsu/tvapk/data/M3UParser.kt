package com.piashmsu.tvapk.data

/**
 * Lenient M3U / M3U8 (extended) playlist parser.
 *
 * Recognises:
 *  - `#EXTINF:` directives with `tvg-id`, `tvg-name`, `tvg-logo`, `group-title`,
 *    `tvg-language`, `tvg-country` attributes.
 *  - `#EXTINF` catch-up attributes: `catchup`, `catchup-source`, `catchup-days`,
 *    `catchup-type` (so the player can build a time-shift timeline).
 *  - `#EXTVLCOPT:http-user-agent=…` / `http-referrer=…` / `http-referer=…`.
 *  - `#KODIPROP:http-user-agent=…` / `http-referrer=…`.
 *  - `#KODIPROP:inputstream.adaptive.stream-headers=key=val&key=val…`
 *    (Kodi-style packed header bundle).
 *
 * Per-source defaults (`source.userAgent`, `source.referer`) are applied to
 * every channel that does not specify its own override. Malformed lines are
 * skipped rather than aborting the whole import.
 */
object M3UParser {

    private val ATTR_REGEX = Regex("""([a-zA-Z0-9_-]+)="([^"]*)"""")

    fun parse(content: String, source: PlaylistSource? = null): List<Channel> {
        val out = mutableListOf<Channel>()
        val lines = content.lineSequence().map { it.trim() }.filter { it.isNotEmpty() }.toList()

        var pendingName: String? = null
        var pendingAttrs: Map<String, String> = emptyMap()
        var pendingHeaders: MutableMap<String, String> = mutableMapOf()
        var pendingUserAgent: String? = null
        var pendingReferer: String? = null

        var index = 0
        for (raw in lines) {
            when {
                raw.startsWith("#EXTM3U") -> continue

                raw.startsWith("#EXTINF") -> {
                    val (attrs, name) = parseExtInf(raw)
                    pendingAttrs = attrs
                    pendingName = name
                    pendingHeaders = mutableMapOf()
                    pendingUserAgent = null
                    pendingReferer = null
                }

                raw.startsWith("#EXTVLCOPT:") -> {
                    val payload = raw.removePrefix("#EXTVLCOPT:")
                    handleHeaderDirective(payload, pendingHeaders) { ua, ref ->
                        if (ua != null) pendingUserAgent = ua
                        if (ref != null) pendingReferer = ref
                    }
                }

                raw.startsWith("#KODIPROP:") -> {
                    val payload = raw.removePrefix("#KODIPROP:")
                    if (payload.startsWith("inputstream.adaptive.stream-headers=", ignoreCase = true)) {
                        val packed = payload.substringAfter('=', "")
                        for (pair in packed.split('&')) {
                            val k = pair.substringBefore('=', "").trim()
                            val v = pair.substringAfter('=', "").trim()
                            if (k.isEmpty()) continue
                            when (k.lowercase()) {
                                "user-agent", "useragent" -> pendingUserAgent = v
                                "referer", "referrer" -> pendingReferer = v
                                else -> pendingHeaders[k] = v
                            }
                        }
                    } else {
                        handleHeaderDirective(payload, pendingHeaders) { ua, ref ->
                            if (ua != null) pendingUserAgent = ua
                            if (ref != null) pendingReferer = ref
                        }
                    }
                }

                raw.startsWith("#") -> continue

                pendingName != null -> {
                    val name = pendingName
                    val attrs = pendingAttrs
                    val srcId = source?.id ?: ""
                    val srcName = source?.name ?: ""
                    val ua = pendingUserAgent ?: source?.userAgent
                    val ref = pendingReferer ?: source?.referer
                    val days = (attrs["catchup-days"] ?: attrs["catchup-time"])?.toIntOrNull()

                    out += Channel(
                        id = (attrs["tvg-id"]?.takeIf { it.isNotBlank() }
                            ?: "ch-${srcId}-${index++}-${name.hashCode()}"),
                        name = attrs["tvg-name"]?.takeIf { it.isNotBlank() } ?: name,
                        logo = attrs["tvg-logo"]?.takeIf { it.isNotBlank() },
                        group = attrs["group-title"]?.takeIf { it.isNotBlank() }
                            ?: "Uncategorized",
                        streamUrl = raw,
                        tvgId = attrs["tvg-id"],
                        language = attrs["tvg-language"],
                        country = attrs["tvg-country"],
                        sourceId = srcId,
                        sourceName = srcName,
                        httpUserAgent = ua,
                        httpReferer = ref,
                        httpHeaders = pendingHeaders.toMap(),
                        catchupSource = attrs["catchup-source"]?.takeIf { it.isNotBlank() },
                        catchupDays = days,
                        catchupType = attrs["catchup"]?.takeIf { it.isNotBlank() }
                            ?: attrs["catchup-type"],
                    )

                    pendingName = null
                    pendingAttrs = emptyMap()
                    pendingHeaders = mutableMapOf()
                    pendingUserAgent = null
                    pendingReferer = null
                }
            }
        }
        return out
    }

    private inline fun handleHeaderDirective(
        payload: String,
        headers: MutableMap<String, String>,
        onUaOrReferer: (ua: String?, ref: String?) -> Unit,
    ) {
        val key = payload.substringBefore('=', "").trim().lowercase()
        val value = payload.substringAfter('=', "").trim()
        when (key) {
            "http-user-agent", "user-agent", "useragent" ->
                onUaOrReferer(value, null)
            "http-referrer", "http-referer", "referer", "referrer" ->
                onUaOrReferer(null, value)
            else -> if (key.startsWith("http-") && value.isNotEmpty()) {
                headers[key.removePrefix("http-")] = value
            }
        }
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
