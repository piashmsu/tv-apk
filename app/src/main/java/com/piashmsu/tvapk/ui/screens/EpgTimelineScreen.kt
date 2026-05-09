package com.piashmsu.tvapk.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ArrowBack
import androidx.compose.material.icons.outlined.LiveTv
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.piashmsu.tvapk.data.Channel
import com.piashmsu.tvapk.data.ChannelStatus
import com.piashmsu.tvapk.data.EpgProgramme
import com.piashmsu.tvapk.ui.AppViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit

private val HOUR_WIDTH_DP = 140.dp
private val ROW_HEIGHT_DP = 84.dp
private val CHANNEL_COL_DP = 120.dp
private val TIME_HEADER_H_DP = 36.dp
private val VISIBLE_HOURS = 8

private data class TimelineProgramme(
    val channelId: String,
    val channelName: String,
    val channelLogo: String?,
    val programme: EpgProgramme,
    val startCol: Int,
    val widthCols: Int,
)

@Composable
fun EpgTimelineScreen(onBack: () -> Unit, onChannelTap: (Channel) -> Unit) {
    val vm: AppViewModel = viewModel(factory = AppViewModel.Factory)
    val channels by vm.channels.collectAsState()
    val epg by vm.epg.collectAsState()
    val statuses by vm.channelStatuses.collectAsState()

    val density = LocalDensity.current
    val hourWidthPx = with(density) { HOUR_WIDTH_DP.toPx() }
    val totalHours = VISIBLE_HOURS

    val now = remember { System.currentTimeMillis() }
    val cal = remember { java.util.Calendar.getInstance() }

    val hourStart by remember {
        derivedStateOf {
            cal.apply { timeInMillis = now }
            cal.set(java.util.Calendar.MINUTE, 0)
            cal.set(java.util.Calendar.SECOND, 0)
            cal.set(java.util.Calendar.MILLISECOND, 0)
            cal.timeInMillis
        }
    }

    val hours = remember { (0 until totalHours).map { hourStart + it * 3600_000L } }

    val channelList by remember(channels, statuses) {
        derivedStateOf {
            channels.filter { ch ->
                val st = statuses[ch.id]
                st == null || st == ChannelStatus.Online
            }.sortedBy { it.name }
        }
    }

    val timeline by remember(channelList, epg, now) {
        derivedStateOf {
            val gridStart = hours.first()
            val gridEnd = hours.last() + 3600_000L
            val slotMs = 5 * 60 * 1000L
            val totalCols = ((gridEnd - gridStart) / slotMs).toInt()

            channelList.flatMap { ch ->
                val tvgId = ch.tvgId.orEmpty()
                val programmes = epg[tvgId].orEmpty().filter { p ->
                    p.start < gridEnd && p.end > gridStart
                }
                programmes.map { p ->
                    val startMs = (p.start - gridStart).coerceAtLeast(0)
                    val endMs = (p.end - gridStart).coerceAtMost(gridEnd - gridStart)
                    val startCol = (startMs / slotMs).toInt()
                    val widthCols = ((endMs / slotMs) - startCol).coerceAtLeast(1L).toInt()
                    TimelineProgramme(
                        channelId = ch.id,
                        channelName = ch.name,
                        channelLogo = ch.logo,
                        programme = p,
                        startCol = startCol,
                        widthCols = widthCols,
                    )
                }
            }
        }
    }

    val nowOffsetCol by remember {
        derivedStateOf {
            val slotMs = 5 * 60 * 1000L
            ((now - hourStart) / slotMs).toInt()
        }
    }

    Column(modifier = Modifier.fillMaxSize().background(Color(0xFF050616))) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            FilledTonalIconButton(onClick = onBack) {
                Icon(Icons.Outlined.ArrowBack, contentDescription = "Back")
            }
            Spacer(Modifier.size(10.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text("EPG Timeline", color = Color.White, style = MaterialTheme.typography.titleLarge)
                Text(
                    "${channelList.size} channels • ${SimpleDateFormat("MMM dd, HH:mm", Locale.getDefault()).format(Date(now))}",
                    color = Color(0xCCBFC4D6),
                    style = MaterialTheme.typography.labelMedium,
                )
            }
        }

        if (channelList.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        Icons.Outlined.LiveTv, contentDescription = null,
                        tint = Color(0x66BFC4D6), modifier = Modifier.size(48.dp),
                    )
                    Spacer(Modifier.height(12.dp))
                    Text(
                        "No online channels with EPG data",
                        color = Color(0xCCBFC4D6),
                        style = MaterialTheme.typography.bodyLarge,
                    )
                }
            }
            return
        }

        val hScrollState = rememberScrollState()
        val lvState = rememberLazyListState()

        Row(modifier = Modifier.fillMaxSize()) {
            LazyColumn(
                state = lvState,
                modifier = Modifier.width(CHANNEL_COL_DP).fillMaxHeight(),
                contentPadding = PaddingValues(bottom = 40.dp),
            ) {
                item(key = "time_header_ph") {
                    Spacer(modifier = Modifier.height(TIME_HEADER_H_DP))
                }
                itemsIndexed(channelList, key = { _, ch -> "lbl-${ch.id}" }) { _, ch ->
                    ChannelLabel(
                        name = ch.name,
                        logo = ch.logo,
                        onClick = { onChannelTap(ch) },
                    )
                }
            }

            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .clipToBounds()
                    .horizontalScroll(hScrollState),
            ) {
                Row(
                    modifier = Modifier
                        .height(TIME_HEADER_H_DP)
                        .background(Color(0xFF0B0E22)),
                ) {
                    hours.forEach { h ->
                        val label = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(h))
                        Box(
                            modifier = Modifier
                                .width(HOUR_WIDTH_DP)
                                .fillMaxHeight()
                                .border(0.5.dp, Color(0x22BFC4D6)),
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(
                                label, color = Color(0xCCBFC4D6),
                                style = MaterialTheme.typography.labelSmall,
                            )
                        }
                    }
                }

                LazyColumn(
                    state = lvState,
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(bottom = 40.dp),
                ) {
                    itemsIndexed(channelList, key = { _, ch -> "row-${ch.id}" }) { rowIdx, ch ->
                        val channelProgrammes = timeline.filter { it.channelId == ch.id }
                        Box(
                            modifier = Modifier
                                .width(HOUR_WIDTH_DP * totalHours)
                                .height(ROW_HEIGHT_DP)
                                .border(0.5.dp, Color(0x15BFC4D6)),
                        ) {
                            channelProgrammes.forEach { tp ->
                                ProgrammeBlock(
                                    title = tp.programme.title,
                                    isLive = tp.programme.isLive(now),
                                    startCol = tp.startCol,
                                    widthCols = tp.widthCols,
                                    totalCols = ((hours.last() + 3600_000L - hours.first()) / (5 * 60 * 1000L)).toInt(),
                                )
                            }
                            if (nowOffsetCol in 0 until (VISIBLE_HOURS * 12)) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxHeight()
                                        .width(2.dp)
                                        .offset { IntOffset(x = (nowOffsetCol * with(density) { (hourWidthPx / 12).toInt() }), y = 0) }
                                        .background(MaterialTheme.colorScheme.primary),
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ChannelLabel(name: String, logo: String?, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .width(CHANNEL_COL_DP)
            .height(ROW_HEIGHT_DP)
            .border(0.5.dp, Color(0x15BFC4D6))
            .clickable(onClick = onClick)
            .padding(horizontal = 6.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(32.dp)
                .clip(CircleShape)
                .background(Color(0xFF1A1F46)),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                name.take(2).uppercase(),
                color = Color.White,
                fontSize = 10.sp,
                textAlign = TextAlign.Center,
            )
        }
        Spacer(Modifier.size(6.dp))
        Text(
            name,
            color = Color.White,
            style = MaterialTheme.typography.labelSmall,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
private fun ProgrammeBlock(
    title: String,
    isLive: Boolean,
    startCol: Int,
    widthCols: Int,
    totalCols: Int,
) {
    val slotW = HOUR_WIDTH_DP / 12
    val density = LocalDensity.current
    val bgColor = when {
        isLive -> MaterialTheme.colorScheme.primary.copy(alpha = 0.35f)
        startCol % 3 == 0 -> Color(0xFF1A2F5A)
        startCol % 3 == 1 -> Color(0xFF2A1F4A)
        else -> Color(0xFF1F3A4A)
    }
    val borderColor = when {
        isLive -> MaterialTheme.colorScheme.primary.copy(alpha = 0.70f)
        else -> Color(0x33FFFFFF)
    }
    Box(
        modifier = Modifier
            .offset { IntOffset(x = (startCol * slotW.value * density.density).toInt(), y = 0) }
            .width(slotW * widthCols)
            .height(ROW_HEIGHT_DP)
            .padding(horizontal = 2.dp, vertical = 3.dp)
            .clip(RoundedCornerShape(6.dp))
            .background(bgColor)
            .border(1.dp, borderColor, RoundedCornerShape(6.dp))
            .padding(horizontal = 6.dp, vertical = 4.dp),
    ) {
        Text(
            title,
            color = Color.White,
            style = MaterialTheme.typography.labelSmall,
            maxLines = 3,
            overflow = TextOverflow.Ellipsis,
        )
        if (isLive) {
            Box(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(2.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primary)
                    .size(6.dp),
            )
        }
    }
}
