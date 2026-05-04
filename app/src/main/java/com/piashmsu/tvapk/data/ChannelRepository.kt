package com.piashmsu.tvapk.data

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request

class ChannelRepository(
    private val context: Context,
    private val http: OkHttpClient,
    private val prefs: AppPrefs,
) {
    private val _state = MutableStateFlow<LoadState>(LoadState.Idle)
    val state: StateFlow<LoadState> = _state.asStateFlow()

    private val _channels = MutableStateFlow<List<Channel>>(emptyList())
    val channels: StateFlow<List<Channel>> = _channels.asStateFlow()

    suspend fun refresh(): Result<Int> = withContext(Dispatchers.IO) {
        val url = prefs.playlistUrl.first()
        if (url.isBlank()) {
            _state.value = LoadState.Idle
            _channels.value = emptyList()
            return@withContext Result.failure(IllegalStateException("No playlist URL configured."))
        }
        _state.value = LoadState.Loading
        runCatching {
            val req = Request.Builder().url(url).header("User-Agent", "TVApk/1.0").build()
            http.newCall(req).execute().use { resp ->
                if (!resp.isSuccessful) {
                    error("HTTP ${resp.code}")
                }
                val body = resp.body?.string().orEmpty()
                val parsed = M3UParser.parse(body)
                _channels.value = parsed
                _state.value = LoadState.Success(parsed.size)
                parsed.size
            }
        }.onFailure {
            _state.value = LoadState.Error(it.message ?: "Failed to load playlist")
        }
    }

    fun groupedByCategory(query: String = ""): List<Category<Channel>> {
        val items = _channels.value
        val filtered = if (query.isBlank()) items
        else items.filter { it.name.contains(query, ignoreCase = true) || it.group.contains(query, ignoreCase = true) }
        return filtered.groupBy { it.group }
            .toSortedMap()
            .map { (g, list) -> Category(g, list.sortedBy { it.name }) }
    }
}

sealed interface LoadState {
    data object Idle : LoadState
    data object Loading : LoadState
    data class Success(val count: Int) : LoadState
    data class Error(val message: String) : LoadState
}
