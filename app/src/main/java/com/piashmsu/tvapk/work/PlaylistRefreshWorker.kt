package com.piashmsu.tvapk.work

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.piashmsu.tvapk.TvApkApp
import com.piashmsu.tvapk.data.RefreshInterval
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit

/**
 * Periodic WorkManager job that refreshes every configured playlist source
 * and EPG endpoint in the background — so the user sees up-to-date channels
 * and "now playing" information even after the app has been backgrounded.
 *
 * The interval is user-controlled via [RefreshInterval].
 */
class PlaylistRefreshWorker(
    appContext: Context,
    params: WorkerParameters,
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result {
        val container = TvApkApp.instance.container
        runCatching { container.channelRepo.refresh() }
        runCatching { container.movieRepo.refresh() }
        runCatching { container.epgRepo.refresh() }
        runCatching {
            val stamp = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).format(Date())
            container.prefs.setLastAutoRefresh(stamp)
        }
        return Result.success()
    }

    companion object {
        const val WORK_NAME = "tv_apk_playlist_refresh"

        fun configure(context: Context, interval: RefreshInterval) {
            val wm = WorkManager.getInstance(context)
            if (interval == RefreshInterval.Off) {
                wm.cancelUniqueWork(WORK_NAME)
                return
            }
            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build()
            val req = PeriodicWorkRequestBuilder<PlaylistRefreshWorker>(
                interval.hours.toLong(), TimeUnit.HOURS
            )
                .setConstraints(constraints)
                .build()
            wm.enqueueUniquePeriodicWork(
                WORK_NAME,
                ExistingPeriodicWorkPolicy.UPDATE,
                req,
            )
        }
    }
}
