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
import org.json.JSONArray
import org.json.JSONObject

/**
 * Movie catalog repository. The catalog is loaded from a user-supplied
 * JSON endpoint with the following shape:
 *
 * ```json
 * [
 *   {
 *     "id": "tt0133093",
 *     "title": "The Matrix (Hindi Dub)",
 *     "poster": "https://example.com/poster.jpg",
 *     "backdrop": "https://example.com/backdrop.jpg",
 *     "streamUrl": "https://example.com/stream.m3u8",
 *     "genre": "Hollywood Hindi Dub",
 *     "language": "Hindi",
 *     "year": 1999,
 *     "duration": 136,
 *     "rating": 8.7,
 *     "description": "..."
 *   }
 * ]
 * ```
 *
 * The app does not bundle any catalog. Until a URL is configured the
 * movies tab simply shows an onboarding card pointing at Settings.
 */
class MovieRepository(
    private val context: Context,
    private val http: OkHttpClient,
    private val prefs: AppPrefs,
) {
    private val _state = MutableStateFlow<LoadState>(LoadState.Idle)
    val state: StateFlow<LoadState> = _state.asStateFlow()

    private val _movies = MutableStateFlow<List<Movie>>(emptyList())
    val movies: StateFlow<List<Movie>> = _movies.asStateFlow()

    suspend fun refresh(): Result<Int> = withContext(Dispatchers.IO) {
        val url = prefs.movieCatalogUrl.first()
        if (url.isBlank()) {
            _state.value = LoadState.Idle
            _movies.value = emptyList()
            return@withContext Result.failure(IllegalStateException("No movie catalog URL configured."))
        }
        _state.value = LoadState.Loading
        runCatching {
            val req = Request.Builder().url(url).header("User-Agent", "TVApk/1.0").build()
            http.newCall(req).execute().use { resp ->
                if (!resp.isSuccessful) error("HTTP ${resp.code}")
                val body = resp.body?.string().orEmpty()
                val parsed = parse(body)
                _movies.value = parsed
                _state.value = LoadState.Success(parsed.size)
                parsed.size
            }
        }.onFailure {
            _state.value = LoadState.Error(it.message ?: "Failed to load movies")
        }
    }

    fun groupedByGenre(query: String = ""): List<Category<Movie>> {
        val items = _movies.value
        val filtered = if (query.isBlank()) items
        else items.filter {
            it.title.contains(query, ignoreCase = true) ||
                it.genre.contains(query, ignoreCase = true) ||
                it.language.contains(query, ignoreCase = true)
        }
        return filtered.groupBy { it.genre.ifBlank { "Other" } }
            .toSortedMap()
            .map { (g, list) -> Category(g, list.sortedByDescending { it.year ?: 0 }) }
    }

    private fun parse(body: String): List<Movie> {
        val trimmed = body.trim()
        val array: JSONArray = when {
            trimmed.startsWith("[") -> JSONArray(trimmed)
            trimmed.startsWith("{") -> {
                val obj = JSONObject(trimmed)
                when {
                    obj.has("results") -> obj.getJSONArray("results")
                    obj.has("movies") -> obj.getJSONArray("movies")
                    obj.has("data") -> obj.getJSONArray("data")
                    else -> JSONArray().apply { put(obj) }
                }
            }
            else -> return emptyList()
        }
        val out = mutableListOf<Movie>()
        for (i in 0 until array.length()) {
            val o = array.optJSONObject(i) ?: continue
            val streamUrl = listOf("streamUrl", "url", "stream", "src", "video")
                .firstNotNullOfOrNull { k -> o.optStringOrNull(k) } ?: continue
            val title = listOf("title", "name").firstNotNullOfOrNull { k -> o.optStringOrNull(k) }
                ?: continue
            out += Movie(
                id = o.optStringOrNull("id") ?: "movie-$i-${title.hashCode()}",
                title = title,
                poster = listOf("poster", "image", "thumbnail", "thumb").firstNotNullOfOrNull { o.optStringOrNull(it) },
                streamUrl = streamUrl,
                genre = o.optStringOrNull("genre") ?: o.optStringOrNull("category") ?: "Other",
                language = o.optStringOrNull("language") ?: o.optStringOrNull("lang") ?: "",
                year = (o.opt("year") as? Number)?.toInt()
                    ?: o.optStringOrNull("year")?.toIntOrNull(),
                description = o.optStringOrNull("description") ?: o.optStringOrNull("overview"),
                durationMinutes = (o.opt("duration") as? Number)?.toInt()
                    ?: o.optStringOrNull("duration")?.toIntOrNull(),
                rating = (o.opt("rating") as? Number)?.toDouble()
                    ?: o.optStringOrNull("rating")?.toDoubleOrNull(),
                backdrop = o.optStringOrNull("backdrop") ?: o.optStringOrNull("hero"),
            )
        }
        return out
    }
}

private fun JSONObject.optStringOrNull(key: String): String? {
    if (!has(key) || isNull(key)) return null
    val s = optString(key, "")
    return s.takeIf { it.isNotBlank() && it != "null" }
}
