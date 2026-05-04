package com.piashmsu.tvapk.data

import android.content.Context
import okhttp3.OkHttpClient
import java.util.concurrent.TimeUnit

class AppContainer(context: Context) {
    val http: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .followRedirects(true)
        .followSslRedirects(true)
        .build()

    val prefs: AppPrefs = AppPrefs(context)
    val channelRepo: ChannelRepository = ChannelRepository(context, http, prefs)
    val movieRepo: MovieRepository = MovieRepository(context, http, prefs)
}
