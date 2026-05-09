package com.piashmsu.tvapk

import android.app.PictureInPictureParams
import android.content.pm.ActivityInfo
import android.content.res.Configuration
import android.os.Bundle
import android.util.Rational
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import com.piashmsu.tvapk.ui.TvApkRoot
import com.piashmsu.tvapk.ui.theme.TvApkTheme
import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Build
import android.app.PendingIntent
import android.content.Intent
import androidx.core.app.NotificationCompat

class MainActivity : ComponentActivity() {

    companion object {
        const val CHANNEL_ID_PLAYBACK = "tvapk_playback"
        const val NOTIFICATION_ID_PLAYBACK = 1001
        @Volatile
        var pipAware: ((Boolean) -> Unit)? = null
    }

    var isInPipMode = false
        private set

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        createNotificationChannel()

        setContent {
            TvApkTheme {
                Surface(modifier = Modifier.fillMaxSize().background(Color.Black)) {
                    PiPStateListener()
                    TvApkRoot()
                }
            }
        }
    }

    @Composable
    private fun PiPStateListener() {
        val context = LocalContext.current
        DisposableEffect(Unit) {
            pipAware = { playing ->
                if (playing && Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    (context as? MainActivity)?.enterPictureInPicture()
                }
            }
            onDispose { pipAware = null }
        }
    }

    private fun enterPictureInPicture() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        try {
            val params = PictureInPictureParams.Builder()
                .setAspectRatio(Rational(16, 9))
                .build()
            enterPictureInPictureMode(params)
        } catch (_: Exception) { }
    }

    override fun onPictureInPictureModeChanged(
        isInPictureInPictureMode: Boolean,
        newConfig: Configuration
    ) {
        super.onPictureInPictureModeChanged(isInPictureInPictureMode, newConfig)
        isInPipMode = isInPictureInPictureMode
        if (!isInPictureInPictureMode) {
            // Returning from PiP — restore full screen
            requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
        }
    }

    override fun onUserLeaveHint() {
        super.onUserLeaveHint()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID_PLAYBACK,
                "Playback",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Background playback notification"
                setShowBadge(false)
            }
            val nm = getSystemService(NotificationManager::class.java)
            nm.createNotificationChannel(channel)
        }
    }

    fun showPlaybackNotification(title: String, isPlaying: Boolean) {
        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            this, 0, intent,
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) PendingIntent.FLAG_IMMUTABLE
            else PendingIntent.FLAG_UPDATE_CURRENT
        )
        val notification = NotificationCompat.Builder(this, CHANNEL_ID_PLAYBACK)
            .setContentTitle(title)
            .setContentText(if (isPlaying) "Now playing" else "Paused")
            .setSmallIcon(android.R.drawable.ic_media_play)
            .setContentIntent(pendingIntent)
            .setOngoing(isPlaying)
            .setSilent(true)
            .build()
        val nm = getSystemService(NotificationManager::class.java)
        nm.notify(NOTIFICATION_ID_PLAYBACK, notification)
    }

    fun cancelPlaybackNotification() {
        val nm = getSystemService(NotificationManager::class.java)
        nm.cancel(NOTIFICATION_ID_PLAYBACK)
    }
}
