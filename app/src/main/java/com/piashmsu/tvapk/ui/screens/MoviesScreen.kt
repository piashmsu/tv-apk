package com.piashmsu.tvapk.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items as listItems
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.piashmsu.tvapk.data.LoadState
import com.piashmsu.tvapk.data.Movie
import com.piashmsu.tvapk.ui.AppViewModel
import com.piashmsu.tvapk.ui.components.EmptyState
import com.piashmsu.tvapk.ui.components.GenreChip
import com.piashmsu.tvapk.ui.components.MovieCard

@Composable
fun MoviesScreen(onMovieTap: (Movie) -> Unit) {
    val vm: AppViewModel = viewModel(factory = AppViewModel.Factory)
    val movies by vm.movies.collectAsState()
    val state by vm.movieState.collectAsState()
    val movieUrl by vm.movieCatalogUrl.collectAsState()

    var selectedGenre by remember { mutableStateOf<String?>(null) }
    val genres = remember(movies) {
        movies.map { it.genre.ifBlank { "Other" } }.distinct().sorted()
    }
    val filtered = remember(movies, selectedGenre) {
        if (selectedGenre == null) movies else movies.filter { it.genre == selectedGenre }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        TopRow(
            title = "Movies",
            subtitle = if (movies.isEmpty()) "No movies yet" else "${movies.size} titles",
            onRefresh = { vm.refreshMovies() },
            isRefreshing = state is LoadState.Loading,
        )

        if (movieUrl.isBlank()) {
            EmptyState(
                title = "No catalog configured",
                body = "Add a movie catalog JSON URL in Settings to populate this tab.",
            )
            return
        }

        if (genres.isNotEmpty()) {
            LazyRow(
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                item {
                    GenreChip(label = "All", selected = selectedGenre == null) { selectedGenre = null }
                }
                listItems(genres) { genre ->
                    GenreChip(
                        label = genre,
                        selected = selectedGenre == genre,
                    ) {
                        selectedGenre = if (selectedGenre == genre) null else genre
                    }
                }
            }
        }

        if (state is LoadState.Loading && movies.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = MaterialTheme.colorScheme.secondary)
            }
        } else if (state is LoadState.Error && movies.isEmpty()) {
            EmptyState(
                title = "Couldn't load movies",
                body = (state as LoadState.Error).message,
                actionLabel = "Try again",
                onAction = { vm.refreshMovies() },
            )
        } else if (filtered.isEmpty()) {
            EmptyState(title = "Nothing here", body = "Try a different genre.")
        } else {
            LazyVerticalGrid(
                columns = GridCells.Adaptive(150.dp),
                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 4.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                modifier = Modifier.fillMaxWidth(),
            ) {
                items(filtered) { m ->
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
                item { Spacer(modifier = Modifier.height(24.dp)) }
            }
        }
    }
}
