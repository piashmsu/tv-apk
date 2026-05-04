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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.LiveTv
import androidx.compose.material.icons.outlined.Movie
import androidx.compose.material.icons.outlined.Save
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.piashmsu.tvapk.R
import com.piashmsu.tvapk.ui.AppViewModel

@Composable
fun SettingsScreen() {
    val vm: AppViewModel = viewModel(factory = AppViewModel.Factory)
    val playlistUrl by vm.playlistUrl.collectAsState()
    val movieUrl by vm.movieCatalogUrl.collectAsState()
    val uriHandler = LocalUriHandler.current

    var localPlaylist by remember { mutableStateOf(playlistUrl) }
    var localMovies by remember { mutableStateOf(movieUrl) }

    LaunchedEffect(playlistUrl) { if (localPlaylist.isBlank()) localPlaylist = playlistUrl }
    LaunchedEffect(movieUrl) { if (localMovies.isBlank()) localMovies = movieUrl }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = 32.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item { TopHeader() }

        item {
            Card("IPTV / M3U Playlist URL") {
                Text(
                    "Provide an HTTPS link to an .m3u or .m3u8 file (typically supplied by your IPTV provider). The app parses tvg-name, tvg-logo, group-title, and tvg-id directives.",
                    color = Color(0xCCBFC4D6),
                    style = MaterialTheme.typography.bodyMedium,
                )
                Spacer(Modifier.height(12.dp))
                OutlinedTextField(
                    value = localPlaylist,
                    onValueChange = { localPlaylist = it },
                    placeholder = { Text("https://example.com/playlist.m3u") },
                    singleLine = true,
                    colors = textFieldColors(),
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(Modifier.height(10.dp))
                PrimaryButton(text = "Save & load channels", onClick = { vm.savePlaylistUrl(localPlaylist) })
            }
        }

        item {
            Card("Movie catalog JSON URL (optional)") {
                Text(
                    "Provide a JSON endpoint that returns an array of movie objects (title, streamUrl, poster, genre, language, year, …). The Movies tab is empty until this is set.",
                    color = Color(0xCCBFC4D6),
                    style = MaterialTheme.typography.bodyMedium,
                )
                Spacer(Modifier.height(12.dp))
                OutlinedTextField(
                    value = localMovies,
                    onValueChange = { localMovies = it },
                    placeholder = { Text("https://example.com/movies.json") },
                    singleLine = true,
                    colors = textFieldColors(),
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(Modifier.height(10.dp))
                PrimaryButton(text = "Save & load catalog", onClick = { vm.saveMovieCatalogUrl(localMovies) })
            }
        }

        item {
            Card("About") {
                AboutRow("App", "TV APK • v1.0")
                AboutRow("Developer", stringResource(R.string.developer_name))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { uriHandler.openUri("https://" + "fb.com/piashmsuf") }
                        .padding(vertical = 6.dp),
                ) {
                    Text("Facebook", color = Color(0xAABFC4D6), modifier = Modifier.weight(1f))
                    Text(
                        stringResource(R.string.developer_handle),
                        color = MaterialTheme.colorScheme.secondary,
                    )
                }
                AboutRow("Player", "Media3 / ExoPlayer 1.4.1")
                AboutRow("Streaming", "HLS • DASH • SmoothStreaming • RTSP • Progressive")
            }
        }

        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
            ) {
                Text(
                    "TV APK is a player shell. The user is responsible for ensuring the streams and catalogs they configure are legal to consume in their jurisdiction.",
                    color = Color(0x88BFC4D6),
                    style = MaterialTheme.typography.labelMedium,
                )
            }
        }
    }
}

@Composable
private fun TopHeader() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(20.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(
                    Brush.linearGradient(
                        listOf(MaterialTheme.colorScheme.primary, MaterialTheme.colorScheme.tertiary)
                    )
                ),
            contentAlignment = Alignment.Center,
        ) {
            Icon(Icons.Outlined.Save, contentDescription = null, tint = Color.White)
        }
        Spacer(Modifier.size(12.dp))
        Column {
            Text("Settings", color = Color.White, style = MaterialTheme.typography.headlineLarge)
            Text(
                "Bring your own playlist & catalog",
                color = Color(0xCCBFC4D6),
                style = MaterialTheme.typography.labelMedium,
            )
        }
    }
}

@Composable
private fun Card(title: String, content: @Composable () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .clip(RoundedCornerShape(20.dp))
            .background(Color(0xFF111527))
            .padding(20.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = if (title.contains("Movie", ignoreCase = true)) Icons.Outlined.Movie else Icons.Outlined.LiveTv,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
            )
            Spacer(Modifier.size(8.dp))
            Text(title, color = Color.White, style = MaterialTheme.typography.titleLarge)
        }
        Spacer(Modifier.height(8.dp))
        content()
    }
}

@Composable
private fun PrimaryButton(text: String, onClick: () -> Unit) {
    Button(
        onClick = onClick,
        colors = ButtonDefaults.buttonColors(
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor = Color.White,
        ),
        shape = RoundedCornerShape(50),
    ) {
        Icon(Icons.Outlined.Save, contentDescription = null, modifier = Modifier.size(18.dp))
        Spacer(Modifier.size(6.dp))
        Text(text, style = MaterialTheme.typography.labelLarge)
    }
}

@Composable
private fun AboutRow(label: String, value: String) {
    Row(modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp)) {
        Text(label, color = Color(0xAABFC4D6), modifier = Modifier.weight(1f))
        Text(value, color = Color.White)
    }
}

@Composable
private fun textFieldColors() = OutlinedTextFieldDefaults.colors(
    focusedContainerColor = Color(0xFF0B0F1F),
    unfocusedContainerColor = Color(0xFF0B0F1F),
    focusedBorderColor = MaterialTheme.colorScheme.primary,
    unfocusedBorderColor = Color(0x33BFC4D6),
    focusedTextColor = Color.White,
    unfocusedTextColor = Color.White,
    focusedPlaceholderColor = Color(0x88BFC4D6),
    unfocusedPlaceholderColor = Color(0x88BFC4D6),
)
