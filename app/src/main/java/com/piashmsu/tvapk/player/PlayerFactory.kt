package com.piashmsu.tvapk.player

import android.content.Context
import androidx.media3.datasource.DefaultDataSource
import androidx.media3.datasource.DefaultHttpDataSource
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory

/**
 * Builds an ExoPlayer instance configured to handle the dominant IPTV stream
 * formats (HLS, MPEG-DASH, SmoothStreaming, RTSP, and progressive MP4/MKV).
 *
 * `userAgent`, `referer`, and `extraHeaders` are forwarded to the HTTP data
 * source so DRM-free streams that require a specific User-Agent / Referer
 * (a common protection against hot-linking) play correctly.
 */
fun buildPlayerForUrl(
    context: Context,
    url: String,
    userAgent: String? = null,
    referer: String? = null,
    extraHeaders: Map<String, String> = emptyMap(),
): ExoPlayer {
    @Suppress("UNUSED_VARIABLE")
    val targetUrl = url

    val httpFactory = DefaultHttpDataSource.Factory()
        .setUserAgent(userAgent ?: "TVApk/1.0 (Android)")
        .setConnectTimeoutMs(15_000)
        .setReadTimeoutMs(30_000)
        .setAllowCrossProtocolRedirects(true)

    val headers = buildMap {
        if (!referer.isNullOrBlank()) put("Referer", referer)
        putAll(extraHeaders)
    }
    if (headers.isNotEmpty()) httpFactory.setDefaultRequestProperties(headers)

    val dataSourceFactory = DefaultDataSource.Factory(context, httpFactory)
    val factory = DefaultMediaSourceFactory(dataSourceFactory)
    return ExoPlayer.Builder(context)
        .setMediaSourceFactory(factory)
        .build()
}
