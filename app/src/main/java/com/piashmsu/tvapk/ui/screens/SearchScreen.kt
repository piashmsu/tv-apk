package com.piashmsu.tvapk.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.Icon
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.piashmsu.tvapk.data.Channel
import com.piashmsu.tvapk.data.Movie
import com.piashmsu.tvapk.ui.AppViewModel
import com.piashmsu.tvapk.ui.components.ChannelTile
import com.piashmsu.tvapk.ui.components.EmptyState
import com.piashmsu.tvapk.ui.components.MovieCard
import com.piashmsu.tvapk.ui.components.SectionHeader

@Composable
fun SearchScreen(
    onChannelTap: (Channel) -> Unit,
    onMovieTap: (Movie) -> Unit,
) {
    val vm: AppViewModel = viewModel(factory = AppViewModel.Factory)
    val channels by vm.channels.collectAsState()
    val movies by vm.movies.collectAsState()

    var query by remember { mutableStateOf("") }

    val matchedChannels = remember(channels, query) {
        if (query.isBlank()) emptyList()
        else channels.filter { it.name.contains(query, ignoreCase = true) || it.group.contains(query, ignoreCase = true) }
            .take(40)
    }
    val matchedMovies = remember(movies, query) {
        if (query.isBlank()) emptyList()
        else movies.filter { it.title.contains(query, ignoreCase = true) || it.genre.contains(query, ignoreCase = true) || it.language.contains(query, ignoreCase = true) }
            .take(60)
    }

    Column(modifier = Modifier.fillMaxSize()) {
        Column(modifier = Modifier.padding(20.dp)) {
            Text("Search", color = Color.White, style = MaterialTheme.typography.headlineLarge)
            Text(
                "Channels and movies",
                color = Color(0xCCBFC4D6),
                style = MaterialTheme.typography.labelMedium,
            )
        }
        OutlinedTextField(
            value = query,
            onValueChange = { query = it },
            placeholder = { Text("Search…") },
            leadingIcon = { Icon(Icons.Outlined.Search, contentDescription = null) },
            singleLine = true,
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
                .padding(horizontal = 16.dp),
        )

        if (query.isBlank()) {
            EmptyState(
                title = "Type to search",
                body = "Find live channels and movies by name, genre, or language.",
            )
            return
        }

        if (matchedChannels.isEmpty() && matchedMovies.isEmpty()) {
            EmptyState(title = "No results", body = "Try a different keyword.")
            return
        }

        LazyColumn(
            contentPadding = PaddingValues(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            if (matchedChannels.isNotEmpty()) {
                item { SectionHeader(title = "Channels", subtitle = "${matchedChannels.size} match") }
                item {
                    LazyRow(
                        contentPadding = PaddingValues(horizontal = 14.dp),
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        items(matchedChannels) { ch ->
                            ChannelTile(
                                name = ch.name,
                                logo = ch.logo,
                                group = ch.group,
                                onClick = { onChannelTap(ch) },
                            )
                        }
                    }
                }
            }
            if (matchedMovies.isNotEmpty()) {
                item { SectionHeader(title = "Movies", subtitle = "${matchedMovies.size} match") }
                item {
                    LazyRow(
                        contentPadding = PaddingValues(horizontal = 14.dp),
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        items(matchedMovies) { m ->
                            MovieCard(
                                title = m.title,
                                poster = m.poster,
                                subtitle = listOfNotNull(
                                    m.year?.toString(),
                                    m.language.takeIf { it.isNotBlank() },
                                ).joinToString(" • "),
                                onClick = { onMovieTap(m) },
                            )
                        }
                    }
                }
            }
        }
    }
}
