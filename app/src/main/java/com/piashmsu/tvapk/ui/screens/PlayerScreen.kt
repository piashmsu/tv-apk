package com.piashmsu.tvapk.ui.screens

import android.app.Activity
import android.content.pm.ActivityInfo
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AspectRatio
import androidx.compose.material.icons.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Bedtime
import androidx.compose.material.icons.outlined.FastForward
import androidx.compose.material.icons.outlined.FastRewind
import androidx.compose.material.icons.outlined.FiberManualRecord
import androidx.compose.material.icons.outlined.Pause
import androidx.compose.material.icons.outlined.PlayArrow
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material.icons.outlined.Stop
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
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
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView
import com.piashmsu.tvapk.data.PlaybackTarget
import com.piashmsu.tvapk.data.PlaybackTargetHolder
import com.piashmsu.tvapk.data.RecentChannel
import com.piashmsu.tvapk.MainActivity
import com.piashmsu.tvapk.player.buildPlayerForUrl
import com.piashmsu.tvapk.record.RecordingArgs
import com.piashmsu.tvapk.record.RecordingService
import com.piashmsu.tvapk.record.RecordingState
import com.piashmsu.tvapk.ui.AppViewModel
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private const val CONTROL_HIDE_DELAY_MS = 3500L

private enum class AspectMode(val label: String, val mode: Int) {
    Fit("Fit", AspectRatioFrameLayout.RESIZE_MODE_FIT),
    Fill("Fill", AspectRatioFrameLayout.RESIZE_MODE_FILL),
    Zoom("Zoom", AspectRatioFrameLayout.RESIZE_MODE_ZOOM),
    Width("16:9", AspectRatioFrameLayout.RESIZE_MODE_FIXED_WIDTH),
    Height("4:3", AspectRatioFrameLayout.RESIZE_MODE_FIXED_HEIGHT),
}

private data class SleepOption(val label: String, val minutes: Int)

private val SLEEP_OPTIONS = listOf(
    SleepOption("Off", 0),
    SleepOption("15 min", 15),
    SleepOption("30 min", 30),
    SleepOption("60 min", 60),
    SleepOption("90 min", 90),
)

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
    var controlsVisible by remember { mutableStateOf(true) }
    var lastInteractionAt by remember { mutableLongStateOf(System.currentTimeMillis()) }
    var aspectMode by remember { mutableStateOf(AspectMode.Fit) }
    var sleepMenuOpen by remember { mutableStateOf(false) }
    var sleepDeadlineMs by remember { mutableLongStateOf(0L) }
    var sleepRemaining by remember { mutableStateOf("") }
    var seekIndicator by remember { mutableStateOf<Pair<Boolean, Long>?>(null) }

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

    LaunchedEffect(isPlaying, current.title) {
        val activity = context as? MainActivity ?: return@LaunchedEffect
        if (isPlaying) {
            activity.showPlaybackNotification(current.title, true)
        } else {
            activity.showPlaybackNotification(current.title, false)
        }
    }

    LaunchedEffect(controlsVisible, isPlaying, lastInteractionAt) {
        if (controlsVisible && isPlaying) {
            delay(CONTROL_HIDE_DELAY_MS)
            if (System.currentTimeMillis() - lastInteractionAt >= CONTROL_HIDE_DELAY_MS - 50) {
                controlsVisible = false
            }
        }
    }

    LaunchedEffect(sleepDeadlineMs) {
        if (sleepDeadlineMs <= 0L) {
            sleepRemaining = ""
            return@LaunchedEffect
        }
        while (true) {
            val remaining = sleepDeadlineMs - System.currentTimeMillis()
            if (remaining <= 0L) {
                player.pause()
                sleepDeadlineMs = 0L
                sleepRemaining = ""
                break
            }
            val mins = (remaining / 60000L).toInt()
            val secs = ((remaining / 1000L) % 60L).toInt()
            sleepRemaining = "%d:%02d".format(mins, secs)
            delay(500)
        }
    }

    LaunchedEffect(seekIndicator) {
        if (seekIndicator != null) {
            delay(700)
            seekIndicator = null
        }
    }

    EnterImmersive()

    DisposableEffect(player) {
        val listener = object : Player.Listener {
            override fun onIsPlayingChanged(playing: Boolean) {
                isPlaying = playing
                if (!playing) {
                    controlsVisible = true
                    lastInteractionAt = System.currentTimeMillis()
                }
            }
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

    val handleBack: () -> Unit = {
        if (isPlaying) {
            MainActivity.pipAware?.invoke(true)
        } else {
            onBack()
        }
    }

    BackHandler(onBack = handleBack)

    fun touch() {
        controlsVisible = true
        lastInteractionAt = System.currentTimeMillis()
    }

    Box(modifier = Modifier.fillMaxSize().background(Color.Black)) {
        AndroidView(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(player) {
                    detectTapGestures(
                        onTap = {
                            controlsVisible = !controlsVisible
                            if (controlsVisible) lastInteractionAt = System.currentTimeMillis()
                        },
                        onDoubleTap = { offset ->
                            val half = size.width / 2f
                            if (offset.x < half) {
                                player.seekBack()
                                seekIndicator = false to System.currentTimeMillis()
                            } else {
                                player.seekForward()
                                seekIndicator = true to System.currentTimeMillis()
                            }
                            touch()
                        },
                    )
                },
            factory = { ctx ->
                PlayerView(ctx).apply {
                    this.player = player
                    useController = false
                    setShutterBackgroundColor(android.graphics.Color.BLACK)
                    resizeMode = aspectMode.mode
                }
            },
            update = { view ->
                view.resizeMode = aspectMode.mode
            },
        )

        seekIndicator?.let { (forward, _) ->
            Box(
                modifier = Modifier
                    .align(if (forward) Alignment.CenterEnd else Alignment.CenterStart)
                    .padding(horizontal = 36.dp)
                    .clip(CircleShape)
                    .background(Color(0xCC0B0E22))
                    .padding(horizontal = 18.dp, vertical = 12.dp),
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        if (forward) Icons.Outlined.FastForward else Icons.Outlined.FastRewind,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.secondary,
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(
                        if (forward) "+10s" else "-10s",
                        color = Color.White,
                        style = MaterialTheme.typography.titleSmall,
                    )
                }
            }
        }

        AnimatedVisibility(
            visible = controlsVisible,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier.fillMaxSize(),
        ) {
            Box(modifier = Modifier.fillMaxSize()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color(0x66000000)),
                )

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    FilledTonalIconButton(onClick = handleBack) {
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

                    Box {
                        FilledTonalIconButton(
                            onClick = { sleepMenuOpen = true; touch() },
                            colors = IconButtonDefaults.filledTonalIconButtonColors(
                                containerColor = if (sleepDeadlineMs > 0L) MaterialTheme.colorScheme.primary.copy(alpha = 0.45f)
                                    else Color(0x99000000),
                                contentColor = Color.White,
                            ),
                        ) {
                            Icon(Icons.Outlined.Bedtime, contentDescription = "Sleep timer")
                        }
                        DropdownMenu(
                            expanded = sleepMenuOpen,
                            onDismissRequest = { sleepMenuOpen = false },
                        ) {
                            SLEEP_OPTIONS.forEach { opt ->
                                DropdownMenuItem(
                                    text = { Text(opt.label) },
                                    onClick = {
                                        sleepDeadlineMs = if (opt.minutes <= 0) 0L
                                            else System.currentTimeMillis() + opt.minutes * 60_000L
                                        sleepMenuOpen = false
                                        touch()
                                    },
                                )
                            }
                        }
                    }
                    Spacer(Modifier.size(6.dp))
                    FilledTonalIconButton(
                        onClick = {
                            aspectMode = AspectMode.values()[(aspectMode.ordinal + 1) % AspectMode.values().size]
                            touch()
                        },
                        colors = IconButtonDefaults.filledTonalIconButtonColors(
                            containerColor = Color(0x99000000),
                            contentColor = Color.White,
                        ),
                    ) {
                        Icon(Icons.Outlined.AspectRatio, contentDescription = "Aspect ratio")
                    }
                    Spacer(Modifier.size(6.dp))

                    if (current is PlaybackTarget.LiveChannel) {
                        val recordingActive = recordingState is RecordingState.Active
                        FilledTonalIconButton(
                            onClick = {
                                touch()
                                if (recordingActive) {
                                    RecordingService.stop(context)
                                } else {
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
                    FilledTonalIconButton(onClick = { player.seekBack(); touch() }) {
                        Icon(Icons.Outlined.FastRewind, contentDescription = "Rewind 10s")
                    }
                    FilledTonalIconButton(
                        onClick = {
                            if (isPlaying) player.pause() else player.play()
                            touch()
                        },
                        modifier = Modifier.size(72.dp),
                    ) {
                        Icon(
                            if (isPlaying) Icons.Outlined.Pause else Icons.Outlined.PlayArrow,
                            contentDescription = "Play/pause",
                            modifier = Modifier.size(36.dp),
                        )
                    }
                    FilledTonalIconButton(onClick = { player.seekForward(); touch() }) {
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
                    if (sleepRemaining.isNotBlank()) {
                        Row(
                            modifier = Modifier
                                .clip(RoundedCornerShape(50))
                                .background(Color(0x99000000))
                                .padding(horizontal = 10.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Icon(
                                Icons.Outlined.Bedtime,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(14.dp),
                            )
                            Spacer(Modifier.size(6.dp))
                            Text(
                                "Sleep in $sleepRemaining",
                                color = Color.White,
                                style = MaterialTheme.typography.labelMedium,
                            )
                        }
                    }
                    AssistChip(
                        onClick = { touch() },
                        enabled = false,
                        label = { Text("Aspect: ${aspectMode.label}", color = Color.White) },
                        colors = AssistChipDefaults.assistChipColors(
                            containerColor = Color(0x66000000),
                            disabledContainerColor = Color(0x66000000),
                            disabledLabelColor = Color.White,
                        ),
                    )
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
                    Text(
                        "Tap to toggle controls • Double-tap edges to seek ±10s",
                        color = Color(0x99BFC4D6),
                        style = MaterialTheme.typography.labelSmall,
                    )
                }
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
