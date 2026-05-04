package com.piashmsu.tvapk.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.piashmsu.tvapk.TvApkApp
import com.piashmsu.tvapk.data.AppContainer
import com.piashmsu.tvapk.data.AppPrefs
import com.piashmsu.tvapk.data.ChannelRepository
import com.piashmsu.tvapk.data.MovieRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class AppViewModel(private val container: AppContainer) : ViewModel() {

    val prefs: AppPrefs get() = container.prefs
    val channelRepo: ChannelRepository get() = container.channelRepo
    val movieRepo: MovieRepository get() = container.movieRepo

    val playlistUrl = prefs.playlistUrl.stateIn(viewModelScope, SharingStarted.Eagerly, "")
    val movieCatalogUrl = prefs.movieCatalogUrl.stateIn(viewModelScope, SharingStarted.Eagerly, "")

    val channelState = channelRepo.state
    val movieState = movieRepo.state
    val channels = channelRepo.channels
    val movies = movieRepo.movies

    init {
        viewModelScope.launch { channelRepo.refresh() }
        viewModelScope.launch { movieRepo.refresh() }
    }

    fun savePlaylistUrl(url: String) {
        viewModelScope.launch {
            prefs.setPlaylistUrl(url)
            channelRepo.refresh()
        }
    }

    fun saveMovieCatalogUrl(url: String) {
        viewModelScope.launch {
            prefs.setMovieCatalogUrl(url)
            movieRepo.refresh()
        }
    }

    fun refreshChannels() { viewModelScope.launch { channelRepo.refresh() } }
    fun refreshMovies() { viewModelScope.launch { movieRepo.refresh() } }

    companion object {
        val Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return AppViewModel(TvApkApp.instance.container) as T
            }
        }
    }
}
