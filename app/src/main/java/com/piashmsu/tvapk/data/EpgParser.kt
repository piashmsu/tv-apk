package com.piashmsu.tvapk.data

import android.util.Xml
import org.xmlpull.v1.XmlPullParser
import java.io.InputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

/**
 * Minimal XMLTV parser. The XMLTV format used by IPTV EPG providers looks
 * roughly like:
 *
 * ```xml
 * <tv>
 *   <channel id="abc">…</channel>
 *   <programme channel="abc"
 *              start="20240601123000 +0000"
 *              stop ="20240601133000 +0000">
 *     <title>Show name</title>
 *     <desc>Description…</desc>
 *   </programme>
 * </tv>
 * ```
 *
 * The parser is stream-based (handles XMLTVs of several MB without loading
 * the whole document into memory) and is lenient about missing fields.
 */
object EpgParser {

    private val FORMATS = listOf(
        "yyyyMMddHHmmss Z",
        "yyyyMMddHHmmss",
        "yyyyMMddHHmm Z",
        "yyyyMMddHHmm",
    )

    fun parse(stream: InputStream): List<EpgProgramme> {
        val parser = Xml.newPullParser()
        parser.setFeature(XmlPullParser.FEATURE_PROCESS_NAMESPACES, false)
        parser.setInput(stream, null)

        val out = mutableListOf<EpgProgramme>()

        var event = parser.eventType
        while (event != XmlPullParser.END_DOCUMENT) {
            if (event == XmlPullParser.START_TAG && parser.name == "programme") {
                val start = parseDate(parser.getAttributeValue(null, "start"))
                val stop = parseDate(parser.getAttributeValue(null, "stop"))
                val channel = parser.getAttributeValue(null, "channel").orEmpty()
                var title: String? = null
                var desc: String? = null

                var inner = parser.next()
                while (!(inner == XmlPullParser.END_TAG && parser.name == "programme")) {
                    if (inner == XmlPullParser.START_TAG) {
                        when (parser.name) {
                            "title" -> title = parser.nextText()
                            "desc" -> desc = parser.nextText()
                            else -> skip(parser)
                        }
                    }
                    inner = parser.next()
                    if (inner == XmlPullParser.END_DOCUMENT) break
                }

                if (channel.isNotEmpty() && title != null && start > 0 && stop > 0) {
                    out += EpgProgramme(
                        channelId = channel,
                        title = title,
                        description = desc,
                        start = start,
                        end = stop,
                    )
                }
            }
            event = if (event == XmlPullParser.END_DOCUMENT) event else parser.next()
        }
        return out
    }

    private fun parseDate(raw: String?): Long {
        if (raw.isNullOrBlank()) return 0L
        for (fmt in FORMATS) {
            runCatching {
                val sdf = SimpleDateFormat(fmt, Locale.US)
                sdf.timeZone = TimeZone.getTimeZone("UTC")
                val d: Date = sdf.parse(raw) ?: return@runCatching
                return d.time
            }
        }
        return 0L
    }

    private fun skip(parser: XmlPullParser) {
        if (parser.eventType != XmlPullParser.START_TAG) return
        var depth = 1
        while (depth > 0) {
            when (parser.next()) {
                XmlPullParser.END_TAG -> depth--
                XmlPullParser.START_TAG -> depth++
                XmlPullParser.END_DOCUMENT -> return
            }
        }
    }
}
