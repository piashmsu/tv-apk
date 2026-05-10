package com.piashmsu.tvapk

import android.app.Application
import android.util.Log
import coil.ImageLoader
import coil.ImageLoaderFactory
import coil.disk.DiskCache
import coil.memory.MemoryCache
import com.piashmsu.tvapk.data.AppContainer
import com.piashmsu.tvapk.work.PlaylistRefreshWorker
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class TvApkApp : Application(), ImageLoaderFactory {
    lateinit var container: AppContainer
        private set

    private val exceptionHandler = CoroutineExceptionHandler { _, throwable ->
        Log.e("TvApk", "Unhandled coroutine exception", throwable)
    }

    private val appScope = CoroutineScope(Dispatchers.Default + SupervisorJob() + exceptionHandler)

    override fun onCreate() {
        super.onCreate()
        instance = this
        container = AppContainer(this)

        appScope.launch {
            runCatching { container.prefs.seedDefaultsIfNeeded() }
            runCatching {
                val interval = container.prefs.refreshInterval.first()
                PlaylistRefreshWorker.configure(this@TvApkApp, interval)
            }
        }
    }

    override fun newImageLoader(): ImageLoader = ImageLoader.Builder(this)
        .crossfade(true)
        .okHttpClient { container.http }
        .memoryCache {
            MemoryCache.Builder(this)
                .maxSizePercent(0.20)
                .build()
        }
        .diskCache {
            DiskCache.Builder()
                .directory(cacheDir.resolve("image_cache"))
                .maxSizeBytes(64L * 1024 * 1024)
                .build()
        }
        .respectCacheHeaders(false)
        .build()

    companion object {
        lateinit var instance: TvApkApp
            private set
    }
}
