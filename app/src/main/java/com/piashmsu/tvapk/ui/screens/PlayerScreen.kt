package com.piashmsu.tvapk.ui.screens

import android.app.Activity
import android.content.pm.ActivityInfo
import android.view.WindowInsets
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ArrowBack
import androidx.compose.material.icons.outlined.FastForward
import androidx.compose.material.icons.outlined.FastRewind
import androidx.compose.material.icons.outlined.FiberManualRecord
import androidx.compose.material.icons.outlined.Pause
import androidx.compose.material.icons.outlined.PlayArrow
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material.icons.outlined.Stop
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.ui.PlayerView
import com.piashmsu.tvapk.data.PlaybackTarget
import com.piashmsu.tvapk.data.PlaybackTargetHolder
import com.piashmsu.tvapk.data.RecentChannel
import com.piashmsu.tvapk.player.buildPlayerForUrl
import com.piashmsu.tvapk.record.RecordingArgs
import com.piashmsu.tvapk.record.RecordingService
import com.piashmsu.tvapk.record.RecordingState
import com.piashmsu.tvapk.ui.AppViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun PlayerScreen(onBack: () -> Unit) {
    val vm: AppViewModel = viewModel(factory = AppViewModel.Factory)
    val target by PlaybackTargetHolder.current.collectAsState()

    if (target == null) {
        Column(
            modifier = Modifier.fillMaxSize().background(Color.Black),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text("No stream selected", color = Color.White)
            Spacer(Modifier.height(8.dp))
            FilledTonalIconButton(onClick = onBack) {
                Icon(Icons.Outlined.ArrowBack, contentDescription = "Back")
            }
        }
        return
    }

    val current = target!!
    val context = LocalContext.current

    val ua = (current as? PlaybackTarget.LiveChannel)?.channel?.httpUserAgent
    val referer = (current as? PlaybackTarget.LiveChannel)?.channel?.httpReferer
    val headers = (current as? PlaybackTarget.LiveChannel)?.channel?.httpHeaders.orEmpty()

    val player = remember(current) {
        buildPlayerForUrl(context, current.streamUrl, ua, referer, headers).apply {
            setMediaItem(MediaItem.fromUri(current.streamUrl))
            prepare()
            playWhenReady = true
        }
    }
    var isPlaying by remember { mutableStateOf(true) }

    LaunchedEffect(current) {
        vm.prefs.setLastPlayed(current.title)
        if (current is PlaybackTarget.LiveChannel) {
            val ch = current.channel
            vm.pushRecent(
                RecentChannel(
                    channelId = ch.id,
                    name = ch.name,
                    logo = ch.logo,
                    streamUrl = ch.streamUrl,
                    timestamp = System.currentTimeMillis(),
                )
            )
        }
    }

    EnterImmersive()

    DisposableEffect(player) {
        val listener = object : Player.Listener {
            override fun onIsPlayingChanged(playing: Boolean) { isPlaying = playing }
        }
        player.addListener(listener)
        onDispose {
            player.removeListener(listener)
            player.release()
        }
    }

    val nowPlaying = (current as? PlaybackTarget.LiveChannel)?.let { lc ->
        val tvgId = lc.channel.tvgId
        vm.epgRepo.nowPlaying(tvgId)
    }
    val upNext = (current as? PlaybackTarget.LiveChannel)?.let { lc ->
        vm.epgRepo.upNext(lc.channel.tvgId)
    }

    val recordingState by RecordingService.state.collectAsState()
    val premiumUntil by vm.premiumUntil.collectAsState()
    val now = System.currentTimeMillis()
    val isPremium = premiumUntil > now
    var showUnlockDialog by remember { mutableStateOf(false) }
    var pendingStartRecording by remember { mutableStateOf(false) }
    val activity = context as? Activity

    LaunchedEffect(isPremium, pendingStartRecording, current) {
        if (pendingStartRecording && isPremium && current is PlaybackTarget.LiveChannel) {
            RecordingService.start(
                context,
                RecordingArgs(
                    streamUrl = current.streamUrl,
                    title = current.title,
                    userAgent = ua,
                    referer = referer,
                    extraHeaders = headers,
                ),
            )
            pendingStartRecording = false
        }
    }

    if (showUnlockDialog) {
        val adState by vm.rewardedAdState.collectAsState()
        AlertDialog(
            onDismissRequest = { showUnlockDialog = false },
            title = { Text("Unlock recording for 30 min") },
            text = {
                Text(
                    "Watch a short ad to unlock live-TV recording for the next 30 minutes. " +
                        "Recording is a premium feature — every ad you watch extends your unlock window.",
                )
            },
            confirmButton = {
                TextButton(
                    enabled = activity != null,
                    onClick = {
                        showUnlockDialog = false
                        pendingStartRecording = true
                        if (activity != null) {
                            val shown = vm.showRewardedAd(activity) { _ -> }
                            if (!shown) {
                                vm.preloadRewardedAd()
                                pendingStartRecording = false
                            }
                        }
                    },
                ) {
                    val label = when (val s = adState) {
                        is com.piashmsu.tvapk.ads.RewardedAdManager.AdState.Ready -> "Watch ad"
                        is com.piashmsu.tvapk.ads.RewardedAdManager.AdState.Loading -> "Loading ad…"
                        is com.piashmsu.tvapk.ads.RewardedAdManager.AdState.Showing -> "Showing…"
                        is com.piashmsu.tvapk.ads.RewardedAdManager.AdState.Error -> "Retry (${s.message})"
                        else -> "Watch ad"
                    }
                    Text(label)
                }
            },
            dismissButton = {
                TextButton(onClick = { showUnlockDialog = false }) { Text("Cancel") }
            },
        )
    }

    LaunchedEffect(Unit) { vm.preloadRewardedAd() }

    Box(modifier = Modifier.fillMaxSize().background(Color.Black)) {
        AndroidView(
            modifier = Modifier.fillMaxSize(),
            factory = { ctx ->
                PlayerView(ctx).apply {
                    this.player = player
                    useController = false
                    setShutterBackgroundColor(android.graphics.Color.BLACK)
                }
            },
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            FilledTonalIconButton(onClick = onBack) {
                Icon(Icons.Outlined.ArrowBack, contentDescription = "Back")
            }
            Spacer(Modifier.size(10.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    current.title,
                    color = Color.White,
                    style = MaterialTheme.typography.titleLarge,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                if (nowPlaying != null) {
                    Text(
                        "● ${nowPlaying.title}",
                        color = MaterialTheme.colorScheme.secondary,
                        style = MaterialTheme.typography.labelMedium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                } else {
                    Text(
                        "Streaming via ExoPlayer",
                        color = Color(0xCCBFC4D6),
                        style = MaterialTheme.typography.labelMedium,
                    )
                }
            }
            if (current is PlaybackTarget.LiveChannel) {
                val recordingActive = recordingState is RecordingState.Active
                FilledTonalIconButton(
                    onClick = {
                        if (recordingActive) {
                            RecordingService.stop(context)
                        } else if (isPremium) {
                            RecordingService.start(
                                context,
                                RecordingArgs(
                                    streamUrl = current.streamUrl,
                                    title = current.title,
                                    userAgent = ua,
                                    referer = referer,
                                    extraHeaders = headers,
                                ),
                            )
                        } else {
                            showUnlockDialog = true
                        }
                    },
                    colors = IconButtonDefaults.filledTonalIconButtonColors(
                        containerColor = if (recordingActive) Color(0xFFFF3E5C) else Color(0x99000000),
                        contentColor = Color.White,
                    ),
                ) {
                    Icon(
                        if (recordingActive) Icons.Outlined.Stop else Icons.Outlined.FiberManualRecord,
                        contentDescription = if (recordingActive) "Stop recording" else "Record",
                    )
                }
            }
        }

        Row(
            modifier = Modifier
                .align(Alignment.Center)
                .padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            FilledTonalIconButton(onClick = { player.seekBack() }) {
                Icon(Icons.Outlined.FastRewind, contentDescription = "Rewind 10s")
            }
            FilledTonalIconButton(
                onClick = {
                    if (isPlaying) player.pause() else player.play()
                },
                modifier = Modifier.size(72.dp),
            ) {
                Icon(
                    if (isPlaying) Icons.Outlined.Pause else Icons.Outlined.PlayArrow,
                    contentDescription = "Play/pause",
                    modifier = Modifier.size(36.dp),
                )
            }
            FilledTonalIconButton(onClick = { player.seekForward() }) {
                Icon(Icons.Outlined.FastForward, contentDescription = "Fast-forward 10s")
            }
        }

        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            (current as? PlaybackTarget.LiveChannel)?.let { lc ->
                if (lc.channel.hasCatchup) {
                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(50))
                            .background(Color(0x99000000))
                            .padding(horizontal = 10.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(
                            Icons.Outlined.Schedule,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.secondary,
                            modifier = Modifier.size(14.dp),
                        )
                        Spacer(Modifier.size(6.dp))
                        Text(
                            "Catch-up: rewind up to ${lc.channel.catchupDays} days",
                            color = Color.White,
                            style = MaterialTheme.typography.labelMedium,
                        )
                    }
                }
            }
            if (upNext != null) {
                Text(
                    "Up next: ${upNext.title} • ${formatTime(upNext.start)}",
                    color = Color(0xCCBFC4D6),
                    style = MaterialTheme.typography.labelMedium,
                )
            }
            (recordingState as? RecordingState.Active)?.let {
                Text(
                    "● Recording: ${it.bytesWritten / 1024 / 1024} MB written",
                    color = Color(0xFFFF3E5C),
                    style = MaterialTheme.typography.labelLarge,
                )
            }
            (recordingState as? RecordingState.Finished)?.let {
                Text(
                    "Recording saved → ${it.output}",
                    color = Color(0xFF8AE070),
                    style = MaterialTheme.typography.labelMedium,
                )
            }
            if (isPremium && current is PlaybackTarget.LiveChannel) {
                val mins = ((premiumUntil - now) / 60_000).coerceAtLeast(0)
                Text(
                    "Premium unlocked: ${mins}m left",
                    color = Color(0xFFFFD27A),
                    style = MaterialTheme.typography.labelMedium,
                )
            }
        }
    }
}

@Composable
private fun EnterImmersive() {
    val context = LocalContext.current
    DisposableEffect(Unit) {
        val activity = context as? Activity ?: return@DisposableEffect onDispose { }
        val window = activity.window
        val previousOrientation = activity.requestedOrientation
        WindowCompat.setDecorFitsSystemWindows(window, false)
        val controller = WindowInsetsControllerCompat(window, window.decorView)
        controller.systemBarsBehavior =
            WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        controller.hide(WindowInsetsCompat.Type.systemBars())
        activity.requestedOrientation =
            ActivityInfo.SCREEN_ORIENTATION_USER_LANDSCAPE
        onDispose {
            controller.show(WindowInsetsCompat.Type.systemBars())
            WindowCompat.setDecorFitsSystemWindows(window, true)
            activity.requestedOrientation = previousOrientation
        }
    }
}

private fun formatTime(epoch: Long): String =
    SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(epoch))
