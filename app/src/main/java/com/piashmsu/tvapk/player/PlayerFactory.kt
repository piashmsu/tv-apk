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
 * The default DefaultMediaSourceFactory already infers the right source for
 * the URL scheme/extension, but we wrap it here so we can inject a User-Agent
 * header and tweak timeouts in one place.
 */
fun buildPlayerForUrl(context: Context, @Suppress("UNUSED_PARAMETER") url: String): ExoPlayer {
    val httpFactory = DefaultHttpDataSource.Factory()
        .setUserAgent("TVApk/1.0 (Android)")
        .setConnectTimeoutMs(15_000)
        .setReadTimeoutMs(30_000)
        .setAllowCrossProtocolRedirects(true)
    val dataSourceFactory = DefaultDataSource.Factory(context, httpFactory)
    val factory = DefaultMediaSourceFactory(dataSourceFactory)
    return ExoPlayer.Builder(context)
        .setMediaSourceFactory(factory)
        .build()
}
