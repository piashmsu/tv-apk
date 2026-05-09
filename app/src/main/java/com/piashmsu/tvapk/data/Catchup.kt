package com.piashmsu.tvapk.data

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import java.util.concurrent.TimeUnit

/**
 * Build a catch-up / time-shift stream URL by substituting the well-known
 * tokens defined by the IPTV `catchup-source` convention.
 *
 *  - `${start}` / `{utc}` / `{timestamp}` — start time epoch seconds
 *  - `${end}`                              — end time epoch seconds
 *  - `${duration}` / `${offset}`           — duration / offset in seconds
 *  - `${Y}`, `${m}`, `${d}`, `${H}`, `${M}`, `${S}` — UTC date parts
 */
object CatchupUrlBuilder {

    fun build(
        template: String,
        startEpochSeconds: Long,
        durationSeconds: Long,
    ): String {
        val endEpochSeconds = startEpochSeconds + durationSeconds
        val nowSeconds = TimeUnit.MILLISECONDS.toSeconds(System.currentTimeMillis())
        val offset = (nowSeconds - startEpochSeconds).coerceAtLeast(0)

        val parts = formatParts(startEpochSeconds * 1000)

        return template
            .replaceTokens("start", startEpochSeconds.toString())
            .replaceTokens("utc", startEpochSeconds.toString())
            .replaceTokens("timestamp", startEpochSeconds.toString())
            .replaceTokens("end", endEpochSeconds.toString())
            .replaceTokens("duration", durationSeconds.toString())
            .replaceTokens("offset", offset.toString())
            .replaceTokens("Y", parts["Y"]!!)
            .replaceTokens("m", parts["m"]!!)
            .replaceTokens("d", parts["d"]!!)
            .replaceTokens("H", parts["H"]!!)
            .replaceTokens("M", parts["M"]!!)
            .replaceTokens("S", parts["S"]!!)
            .replaceTokens("now", nowSeconds.toString())
    }

    private fun String.replaceTokens(name: String, value: String): String =
        this.replace("\${$name}", value).replace("{$name}", value)

    private fun formatParts(epochMs: Long): Map<String, String> {
        fun fmt(p: String): String {
            val sdf = SimpleDateFormat(p, Locale.US)
            sdf.timeZone = TimeZone.getTimeZone("UTC")
            return sdf.format(Date(epochMs))
        }
        return mapOf(
            "Y" to fmt("yyyy"),
            "m" to fmt("MM"),
            "d" to fmt("dd"),
            "H" to fmt("HH"),
            "M" to fmt("mm"),
            "S" to fmt("ss"),
        )
    }
}
