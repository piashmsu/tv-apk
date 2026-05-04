package com.piashmsu.tvapk.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.LiveTv
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.piashmsu.tvapk.data.Channel
import com.piashmsu.tvapk.data.Category
import com.piashmsu.tvapk.data.LoadState
import com.piashmsu.tvapk.ui.AppViewModel
import com.piashmsu.tvapk.ui.components.ChannelTile
import com.piashmsu.tvapk.ui.components.EmptyState
import com.piashmsu.tvapk.ui.components.GenreChip
import com.piashmsu.tvapk.ui.components.SectionHeader

@Composable
fun LiveTvScreen(onChannelTap: (Channel) -> Unit) {
    val vm: AppViewModel = viewModel(factory = AppViewModel.Factory)
    val channels by vm.channels.collectAsState()
    val state by vm.channelState.collectAsState()
    val playlistUrl by vm.playlistUrl.collectAsState()

    var query by remember { mutableStateOf("") }
    var selectedGroup by remember { mutableStateOf<String?>(null) }

    Column(modifier = Modifier.fillMaxSize()) {
        TopRow(
            title = "Live TV",
            subtitle = if (channels.isEmpty()) "No channels yet" else "${channels.size} channels",
            onRefresh = { vm.refreshChannels() },
            isRefreshing = state is LoadState.Loading,
        )

        if (playlistUrl.isBlank()) {
            EmptyState(
                title = "No playlist configured",
                body = "Add your IPTV M3U playlist URL in Settings to load live TV channels.",
            )
            return
        }

        OutlinedTextField(
            value = query,
            onValueChange = { query = it },
            placeholder = { Text("Search channels…") },
            leadingIcon = { Icon(Icons.Outlined.Search, contentDescription = null) },
            singleLine = true,
            keyboardOptions = KeyboardOptions(),
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = Color(0xFF111527),
                unfocusedContainerColor = Color(0xFF111527),
                focusedBorderColor = MaterialTheme.colorScheme.primary,
                unfocusedBorderColor = Color(0x33BFC4D6),
                focusedTextColor = Color.White,
                unfocusedTextColor = Color.White,
                focusedPlaceholderColor = Color(0x88BFC4D6),
                unfocusedPlaceholderColor = Color(0x88BFC4D6),
                focusedLeadingIconColor = MaterialTheme.colorScheme.primary,
                unfocusedLeadingIconColor = Color(0x88BFC4D6),
            ),
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 6.dp),
        )

        val groups: List<Category<Channel>> = remember(channels, query) {
            vm.channelRepo.groupedByCategory(query)
        }
        val groupNames = groups.map { it.title }

        if (groupNames.isNotEmpty()) {
            LazyRow(
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                item {
                    GenreChip(
                        label = "All",
                        selected = selectedGroup == null,
                        onClick = { selectedGroup = null },
                    )
                }
                items(groupNames) { name ->
                    GenreChip(
                        label = name,
                        selected = selectedGroup == name,
                        onClick = { selectedGroup = if (selectedGroup == name) null else name },
                    )
                }
            }
        }

        val visibleGroups = if (selectedGroup == null) groups else groups.filter { it.title == selectedGroup }

        if (state is LoadState.Loading && channels.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
            }
        } else if (state is LoadState.Error && channels.isEmpty()) {
            EmptyState(
                title = "Couldn't load channels",
                body = (state as LoadState.Error).message,
                actionLabel = "Try again",
                onAction = { vm.refreshChannels() },
            )
        } else if (visibleGroups.isEmpty()) {
            EmptyState(title = "No matches", body = "Try a different search.")
        } else {
            LazyColumn(
                contentPadding = PaddingValues(bottom = 24.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                visibleGroups.forEach { cat ->
                    item {
                        SectionHeader(
                            title = cat.title,
                            subtitle = "${cat.items.size} channel${if (cat.items.size != 1) "s" else ""}",
                        )
                    }
                    item {
                        LazyRow(
                            contentPadding = PaddingValues(horizontal = 14.dp),
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                        ) {
                            items(cat.items) { ch ->
                                ChannelTile(
                                    name = ch.name,
                                    logo = ch.logo,
                                    group = ch.country ?: ch.language ?: ch.group,
                                    onClick = { onChannelTap(ch) },
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
internal fun TopRow(
    title: String,
    subtitle: String,
    onRefresh: () -> Unit,
    isRefreshing: Boolean,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(title, color = Color.White, style = MaterialTheme.typography.headlineLarge)
            Text(subtitle, color = Color(0xCCBFC4D6), style = MaterialTheme.typography.labelMedium)
        }
        IconButton(onClick = onRefresh, enabled = !isRefreshing) {
            if (isRefreshing) {
                CircularProgressIndicator(
                    color = MaterialTheme.colorScheme.primary,
                    strokeWidth = 2.dp,
                    modifier = Modifier.size(20.dp),
                )
            } else {
                Icon(
                    Icons.Outlined.Refresh,
                    contentDescription = "Refresh",
                    tint = Color.White,
                )
            }
        }
    }
}
