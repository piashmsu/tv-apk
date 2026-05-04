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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.LiveTv
import androidx.compose.material.icons.outlined.Movie
import androidx.compose.material.icons.outlined.PlayArrow
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.compose.ui.res.stringResource
import coil.compose.AsyncImage
import com.piashmsu.tvapk.R
import com.piashmsu.tvapk.data.Channel
import com.piashmsu.tvapk.data.Movie
import com.piashmsu.tvapk.ui.AppViewModel
import com.piashmsu.tvapk.ui.components.ChannelTile
import com.piashmsu.tvapk.ui.components.EmptyState
import com.piashmsu.tvapk.ui.components.MovieCard
import com.piashmsu.tvapk.ui.components.SectionHeader

@Composable
fun HomeScreen(
    onChannelTap: (Channel) -> Unit,
    onMovieTap: (Movie) -> Unit,
    onTabRequest: (String) -> Unit,
) {
    val vm: AppViewModel = viewModel(factory = AppViewModel.Factory)
    val channels by vm.channels.collectAsState()
    val movies by vm.movies.collectAsState()
    val playlistSources by vm.playlistSources.collectAsState()
    val movieUrl by vm.movieCatalogUrl.collectAsState()
    val recents by vm.recents.collectAsState()
    val favorites by vm.favorites.collectAsState()
    val epg by vm.epg.collectAsState()

    val featured = movies.firstOrNull { !it.backdrop.isNullOrBlank() }
        ?: movies.firstOrNull()
    val isOnboarding = playlistSources.isEmpty() && movieUrl.isBlank()

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        item { TopBrandBar() }
        if (isOnboarding) {
            item {
                EmptyState(
                    title = "Welcome to TV APK",
                    body = "Add your IPTV M3U playlist URL and (optional) movies catalog URL in Settings to start watching live TV channels and movies.",
                    actionLabel = "Open Settings",
                    onAction = { onTabRequest("settings") },
                )
            }
            item { DeveloperBadge() }
            return@LazyColumn
        }

        if (featured != null) {
            item { Hero(featured = featured, onPlay = { onMovieTap(featured) }) }
        }

        item {
            QuickActionRow(
                onLive = { onTabRequest("live") },
                onMovies = { onTabRequest("movies") },
                onSettings = { onTabRequest("settings") },
            )
        }

        if (recents.isNotEmpty()) {
            val recentChannels = recents.mapNotNull { rc ->
                channels.firstOrNull { it.id == rc.channelId }
            }.take(10)
            if (recentChannels.isNotEmpty()) {
                item {
                    SectionHeader(
                        title = "Recently watched",
                        subtitle = "Pick up where you left off",
                    )
                }
                item {
                    LazyRow(
                        contentPadding = PaddingValues(horizontal = 14.dp),
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        items(recentChannels, key = { it.id }) { ch ->
                            val now = epg[ch.tvgId.orEmpty()].orEmpty()
                                .firstOrNull { p -> System.currentTimeMillis() in p.start..p.end }
                            ChannelTile(
                                name = ch.name,
                                logo = ch.logo,
                                group = ch.country ?: ch.language ?: ch.group,
                                isFavorite = ch.id in favorites,
                                nowPlayingTitle = now?.title,
                                onClick = { onChannelTap(ch) },
                                onFavorite = { vm.toggleFavorite(ch.id) },
                            )
                        }
                    }
                }
            }
        }

        if (channels.isNotEmpty()) {
            item {
                SectionHeader(
                    title = "Live channels",
                    subtitle = "${channels.size} channel${if (channels.size != 1) "s" else ""} loaded",
                )
            }
            item {
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 14.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    items(channels.take(20)) { ch ->
                        val now = epg[ch.tvgId.orEmpty()].orEmpty()
                            .firstOrNull { p -> System.currentTimeMillis() in p.start..p.end }
                        ChannelTile(
                            name = ch.name,
                            logo = ch.logo,
                            group = ch.group,
                            isFavorite = ch.id in favorites,
                            nowPlayingTitle = now?.title,
                            onClick = { onChannelTap(ch) },
                            onFavorite = { vm.toggleFavorite(ch.id) },
                        )
                    }
                }
            }
        }

        val groupedMovies = movies.groupBy { it.genre.ifBlank { "Other" } }
        for ((genre, list) in groupedMovies) {
            item { SectionHeader(title = genre) }
            item {
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 14.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    items(list) { m ->
                        MovieCard(
                            title = m.title,
                            poster = m.poster,
                            subtitle = listOfNotNull(m.year?.toString(), m.language.takeIf { it.isNotBlank() }).joinToString(" • "),
                            onClick = { onMovieTap(m) },
                        )
                    }
                }
            }
        }

        item { DeveloperBadge() }
    }
}

@Composable
private fun TopBrandBar() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(
                    Brush.linearGradient(
                        listOf(MaterialTheme.colorScheme.primary, MaterialTheme.colorScheme.secondary)
                    )
                ),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                Icons.Outlined.LiveTv,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(22.dp),
            )
        }
        Spacer(Modifier.size(10.dp))
        Column {
            Text(stringResource(R.string.app_name), color = Color.White, style = MaterialTheme.typography.titleLarge)
            Text(
                "Live TV • Hindi & Bangla • Movies",
                color = Color(0xCCBFC4D6),
                style = MaterialTheme.typography.labelMedium,
            )
        }
    }
}

@Composable
private fun Hero(featured: Movie, onPlay: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .height(220.dp)
            .clip(RoundedCornerShape(22.dp))
            .background(
                Brush.linearGradient(
                    listOf(Color(0xFF161B2E), Color(0xFF0B0F1F))
                )
            )
            .clickable(onClick = onPlay),
    ) {
        if (!featured.backdrop.isNullOrBlank()) {
            AsyncImage(
                model = featured.backdrop,
                contentDescription = featured.title,
                modifier = Modifier.fillMaxSize(),
                contentScale = androidx.compose.ui.layout.ContentScale.Crop,
            )
        } else if (!featured.poster.isNullOrBlank()) {
            AsyncImage(
                model = featured.poster,
                contentDescription = featured.title,
                modifier = Modifier.fillMaxSize(),
                contentScale = androidx.compose.ui.layout.ContentScale.Crop,
            )
        }
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        listOf(Color.Transparent, Color(0xCC04060B), Color(0xFF04060B))
                    )
                )
        )
        Column(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(20.dp),
        ) {
            Text(
                "Featured",
                color = MaterialTheme.colorScheme.secondary,
                style = MaterialTheme.typography.labelMedium,
            )
            Text(
                featured.title,
                color = Color.White,
                style = MaterialTheme.typography.headlineLarge,
                maxLines = 2,
            )
            Spacer(Modifier.height(8.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(50))
                        .background(MaterialTheme.colorScheme.primary)
                        .clickable(onClick = onPlay)
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Outlined.PlayArrow,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(18.dp),
                        )
                        Spacer(Modifier.size(6.dp))
                        Text("Watch now", color = Color.White, style = MaterialTheme.typography.labelLarge)
                    }
                }
                Spacer(Modifier.size(10.dp))
                Text(
                    listOfNotNull(
                        featured.year?.toString(),
                        featured.language.takeIf { it.isNotBlank() },
                        featured.durationMinutes?.let { "${it}m" },
                    ).joinToString(" • "),
                    color = Color(0xCCBFC4D6),
                    style = MaterialTheme.typography.labelMedium,
                )
            }
        }
    }
}

@Composable
private fun QuickActionRow(
    onLive: () -> Unit,
    onMovies: () -> Unit,
    onSettings: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        QuickAction(
            label = "Live TV",
            icon = Icons.Outlined.LiveTv,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.weight(1f),
            onClick = onLive,
        )
        QuickAction(
            label = "Movies",
            icon = Icons.Outlined.Movie,
            tint = MaterialTheme.colorScheme.secondary,
            modifier = Modifier.weight(1f),
            onClick = onMovies,
        )
        QuickAction(
            label = "Settings",
            icon = Icons.Outlined.Settings,
            tint = MaterialTheme.colorScheme.tertiary,
            modifier = Modifier.weight(1f),
            onClick = onSettings,
        )
    }
}

@Composable
private fun QuickAction(
    label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    tint: Color,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .background(Color(0xFF111527))
            .clickable(onClick = onClick)
            .padding(vertical = 14.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Icon(icon, contentDescription = label, tint = tint)
        Spacer(Modifier.height(4.dp))
        Text(label, color = Color.White, style = MaterialTheme.typography.labelLarge)
    }
}

@Composable
fun DeveloperBadge() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(20.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            "Developed by ${stringResource(R.string.developer_name)}",
            color = Color(0xAABFC4D6),
            style = MaterialTheme.typography.labelLarge,
        )
        Text(
            stringResource(R.string.developer_handle),
            color = MaterialTheme.colorScheme.secondary,
            style = MaterialTheme.typography.labelMedium,
        )
    }
}

