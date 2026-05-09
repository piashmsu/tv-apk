package com.piashmsu.tvapk.data

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
import java.util.zip.GZIPInputStream

/**
 * In-memory EPG cache keyed by `tvg-id`. Refreshed from every
 * [PlaylistSource] that defines a non-blank `epgUrl`.
 *
 * Programmes are kept sorted by start time per channel so the player can
 * cheaply look up "now playing" and "up next" by binary search / linear scan.
 */
class EpgRepository(
    private val http: OkHttpClient,
    private val prefs: AppPrefs,
) {
    private val _byChannel = MutableStateFlow<Map<String, List<EpgProgramme>>>(emptyMap())
    val byChannel: StateFlow<Map<String, List<EpgProgramme>>> = _byChannel.asStateFlow()

    private val _state = MutableStateFlow<LoadState>(LoadState.Idle)
    val state: StateFlow<LoadState> = _state.asStateFlow()

    suspend fun refresh(): Result<Int> = withContext(Dispatchers.IO) {
        val urls = prefs.playlistSources.first()
            .filter { it.enabled && !it.epgUrl.isNullOrBlank() }
            .map { it.epgUrl!! }
            .distinct()

        if (urls.isEmpty()) {
            _state.value = LoadState.Idle
            _byChannel.value = emptyMap()
            return@withContext Result.failure(IllegalStateException("No EPG URLs configured."))
        }
        _state.value = LoadState.Loading

        val merged = coroutineScope {
            urls.map { u -> async { runCatching { fetchOne(u) }.getOrNull().orEmpty() } }
                .awaitAll().flatten()
        }
        if (merged.isEmpty()) {
            _state.value = LoadState.Error("No EPG programmes parsed.")
            _byChannel.value = emptyMap()
            return@withContext Result.failure(IllegalStateException("No EPG programmes parsed."))
        }

        val grouped = merged.groupBy { it.channelId }.mapValues { (_, list) ->
            list.sortedBy { it.start }
        }
        _byChannel.value = grouped
        _state.value = LoadState.Success(merged.size)
        Result.success(merged.size)
    }

    private fun fetchOne(url: String): List<EpgProgramme> {
        val req = Request.Builder().url(url)
            .header("User-Agent", "TVApk/1.0")
            .header("Accept-Encoding", "gzip")
            .build()
        http.newCall(req).execute().use { resp ->
            if (!resp.isSuccessful) error("HTTP ${resp.code}")
            val body = resp.body ?: error("empty body")
            val raw = body.byteStream()
            val stream = if (url.endsWith(".gz", ignoreCase = true) ||
                resp.header("Content-Type")?.contains("gzip", ignoreCase = true) == true) {
                GZIPInputStream(raw)
            } else raw
            stream.use { return EpgParser.parse(it) }
        }
    }

    /** Returns the programme that overlaps `now` for the given channel. */
    fun nowPlaying(tvgId: String?, now: Long = System.currentTimeMillis()): EpgProgramme? {
        if (tvgId.isNullOrBlank()) return null
        val list = _byChannel.value[tvgId] ?: return null
        return list.firstOrNull { now in it.start..it.end }
    }

    /** Returns the next programme strictly after `now`. */
    fun upNext(tvgId: String?, now: Long = System.currentTimeMillis()): EpgProgramme? {
        if (tvgId.isNullOrBlank()) return null
        val list = _byChannel.value[tvgId] ?: return null
        return list.firstOrNull { it.start > now }
    }

    /** Returns up to `limit` programmes for the channel, sorted by start. */
    fun timelineFor(tvgId: String?, limit: Int = 50): List<EpgProgramme> {
        if (tvgId.isNullOrBlank()) return emptyList()
        return _byChannel.value[tvgId].orEmpty().take(limit)
    }
}
