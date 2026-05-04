package com.piashmsu.tvapk.data

import android.content.Context
import android.net.Uri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File

/**
 * Loads and merges channels from every enabled [PlaylistSource]. Sources are
 * fetched in parallel; if one source fails the others are still kept.
 *
 * Supports three URL schemes:
 *  - `http://` / `https://` — fetched over the network (the common case).
 *  - `content://` — read through Android's ContentResolver. Used by the
 *    "Pick M3U from device" file picker so users can pull a playlist out
 *    of any file-manager-accessible location.
 *  - `file://` and bare absolute paths — read directly off disk.
 */
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
        val sources = prefs.playlistSources.first().filter { it.enabled && it.url.isNotBlank() }
        if (sources.isEmpty()) {
            _state.value = LoadState.Idle
            _channels.value = emptyList()
            return@withContext Result.failure(IllegalStateException("No playlist sources configured."))
        }
        _state.value = LoadState.Loading
        val errors = mutableListOf<String>()
        val merged = coroutineScope {
            sources.map { src ->
                async { runCatching { fetchSource(src) }.onFailure { errors += "${src.name}: ${it.message}" } }
            }.awaitAll().mapNotNull { it.getOrNull() }
        }.flatten()

        if (merged.isEmpty() && errors.isNotEmpty()) {
            _state.value = LoadState.Error(errors.joinToString(" • "))
            return@withContext Result.failure(IllegalStateException(errors.joinToString(" • ")))
        }

        _channels.value = merged
        _state.value = LoadState.Success(merged.size)
        Result.success(merged.size)
    }

    private fun fetchSource(source: PlaylistSource): List<Channel> {
        val body = readSource(source)
        return M3UParser.parse(body, source)
    }

    private fun readSource(source: PlaylistSource): String {
        val raw = source.url.trim()
        return when {
            raw.startsWith("content://", ignoreCase = true) -> {
                val uri = Uri.parse(raw)
                context.contentResolver.openInputStream(uri)?.use { it.readBytes().toString(Charsets.UTF_8) }
                    ?: error("Empty file (${source.name})")
            }
            raw.startsWith("file://", ignoreCase = true) -> {
                File(Uri.parse(raw).path ?: error("Bad file URI")).readText(Charsets.UTF_8)
            }
            raw.startsWith("/") -> File(raw).readText(Charsets.UTF_8)
            else -> {
                val builder = Request.Builder().url(raw)
                builder.header("User-Agent", source.userAgent ?: "TVApk/1.0")
                if (!source.referer.isNullOrBlank()) builder.header("Referer", source.referer)
                http.newCall(builder.build()).execute().use { resp ->
                    if (!resp.isSuccessful) error("HTTP ${resp.code}")
                    resp.body?.string().orEmpty()
                }
            }
        }
    }

    fun groupedByCategory(query: String = ""): List<Category<Channel>> {
        val items = _channels.value
        val filtered = if (query.isBlank()) items
        else items.filter {
            it.name.contains(query, ignoreCase = true) ||
                it.group.contains(query, ignoreCase = true) ||
                it.sourceName.contains(query, ignoreCase = true)
        }
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
