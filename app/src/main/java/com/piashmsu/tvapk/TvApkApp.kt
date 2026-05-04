package com.piashmsu.tvapk

import android.app.Application
import com.piashmsu.tvapk.data.AppContainer
import com.piashmsu.tvapk.work.PlaylistRefreshWorker
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class TvApkApp : Application() {
    lateinit var container: AppContainer
        private set

    private val appScope = CoroutineScope(Dispatchers.Default + SupervisorJob())

    override fun onCreate() {
        super.onCreate()
        instance = this
        container = AppContainer(this)

        // Initialise AdMob early so the first ad load is fast, and preload
        // a rewarded ad in the background.
        container.rewardedAds.initialiseOnce()
        container.rewardedAds.preload()

        appScope.launch {
            // Apply the user-configured periodic refresh, if any.
            val interval = container.prefs.refreshInterval.first()
            PlaylistRefreshWorker.configure(this@TvApkApp, interval)
        }
    }

    companion object {
        lateinit var instance: TvApkApp
            private set
    }
}
