package com.piashmsu.tvapk

import android.app.Application
import com.piashmsu.tvapk.data.AppContainer

class TvApkApp : Application() {
    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        instance = this
        container = AppContainer(this)
    }

    companion object {
        lateinit var instance: TvApkApp
            private set
    }
}
