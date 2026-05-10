package com.piashmsu.tvapk.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.piashmsu.tvapk.TvApkApp
import com.piashmsu.tvapk.data.AppContainer
import com.piashmsu.tvapk.data.AppPrefs
import com.piashmsu.tvapk.data.ChannelRepository
import com.piashmsu.tvapk.data.EpgRepository
import com.piashmsu.tvapk.data.MovieRepository
import com.piashmsu.tvapk.data.PlaylistSource
import com.piashmsu.tvapk.data.RecentChannel
import com.piashmsu.tvapk.data.RefreshInterval
import com.piashmsu.tvapk.work.PlaylistRefreshWorker
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import com.piashmsu.tvapk.DebugLog

class AppViewModel(private val container: AppContainer) : ViewModel() {

    val prefs: AppPrefs get() = container.prefs
    val channelRepo: ChannelRepository get() = container.channelRepo
    val movieRepo: MovieRepository get() = container.movieRepo
    val epgRepo: EpgRepository get() = container.epgRepo

    val playlistSources = prefs.playlistSources
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())
    val movieCatalogUrl = prefs.movieCatalogUrl
        .stateIn(viewModelScope, SharingStarted.Eagerly, "")
    val refreshInterval = prefs.refreshInterval
        .stateIn(viewModelScope, SharingStarted.Eagerly, RefreshInterval.Off)
    val lastAutoRefresh = prefs.lastAutoRefresh
        .stateIn(viewModelScope, SharingStarted.Eagerly, "")
    val favorites = prefs.favorites
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptySet())
    val recents = prefs.recents
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    val channelState = channelRepo.state
    val movieState = movieRepo.state
    val epgState = epgRepo.state
    val channels = channelRepo.channels
    val movies = movieRepo.movies
    val epg = epgRepo.byChannel
    val channelStatuses = channelRepo.statuses
    val probeProgress = channelRepo.probeProgress

    init {
        DebugLog.log("ViewModel init")
        viewModelScope.launch {
            runCatching { channelRepo.refresh() }
            runCatching {
                if (channelRepo.statuses.value.isEmpty()) {
                    DebugLog.log("Auto-probe trigger from init")
                    channelRepo.probeReachability()
                }
            }
        }
        viewModelScope.launch { runCatching { movieRepo.refresh() } }
        viewModelScope.launch { runCatching { epgRepo.refresh() } }
    }

    fun saveMovieCatalogUrl(url: String) {
        viewModelScope.launch {
            prefs.setMovieCatalogUrl(url)
            movieRepo.refresh()
        }
    }

    fun upsertPlaylistSource(source: PlaylistSource) {
        viewModelScope.launch {
            runCatching { prefs.upsertPlaylistSource(source) }
            runCatching { channelRepo.refresh() }
            runCatching { epgRepo.refresh() }
        }
    }

    fun togglePlaylistEnabled(source: PlaylistSource) {
        viewModelScope.launch {
            runCatching { prefs.upsertPlaylistSource(source.copy(enabled = !source.enabled)) }
            runCatching { channelRepo.refresh() }
            runCatching { epgRepo.refresh() }
        }
    }

    fun removePlaylistSource(id: String) {
        viewModelScope.launch {
            runCatching { prefs.removePlaylistSource(id) }
            runCatching { channelRepo.refresh() }
            runCatching { epgRepo.refresh() }
        }
    }

    fun setRefreshInterval(value: RefreshInterval) {
        viewModelScope.launch {
            prefs.setRefreshInterval(value)
            PlaylistRefreshWorker.configure(TvApkApp.instance, value)
        }
    }

    fun toggleFavorite(channelId: String) {
        viewModelScope.launch { prefs.toggleFavorite(channelId) }
    }

    fun pushRecent(entry: RecentChannel) {
        viewModelScope.launch { prefs.pushRecent(entry) }
    }

    fun refreshChannels() {
        viewModelScope.launch { runCatching { channelRepo.refresh() } }
    }
    fun refreshMovies() {
        viewModelScope.launch { runCatching { movieRepo.refresh() } }
    }
    fun refreshEpg() {
        viewModelScope.launch { runCatching { epgRepo.refresh() } }
    }
    fun probeChannels() {
        viewModelScope.launch { runCatching { channelRepo.probeReachability() } }
    }

    companion object {
        val Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return AppViewModel(TvApkApp.instance.container) as T
            }
        }
    }
}
