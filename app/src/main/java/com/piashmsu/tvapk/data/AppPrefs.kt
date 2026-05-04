package com.piashmsu.tvapk.data

import android.content.Context
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore(name = "tv_apk_prefs")

/**
 * User-controlled application preferences.
 *
 * The app intentionally ships with no preconfigured channels or movies.
 * The user must supply their own legal IPTV M3U URL and/or movie catalog
 * JSON URL through the Settings screen.
 */
class AppPrefs(private val context: Context) {

    private object Keys {
        val PLAYLIST_URL = stringPreferencesKey("playlist_url")
        val MOVIE_CATALOG_URL = stringPreferencesKey("movie_catalog_url")
        val LAST_PLAYED = stringPreferencesKey("last_played")
    }

    val playlistUrl: Flow<String> = context.dataStore.data.map {
        it[Keys.PLAYLIST_URL].orEmpty()
    }
    val movieCatalogUrl: Flow<String> = context.dataStore.data.map {
        it[Keys.MOVIE_CATALOG_URL].orEmpty()
    }
    val lastPlayed: Flow<String> = context.dataStore.data.map {
        it[Keys.LAST_PLAYED].orEmpty()
    }

    suspend fun setPlaylistUrl(url: String) = update(Keys.PLAYLIST_URL, url.trim())
    suspend fun setMovieCatalogUrl(url: String) = update(Keys.MOVIE_CATALOG_URL, url.trim())
    suspend fun setLastPlayed(title: String) = update(Keys.LAST_PLAYED, title.take(120))

    private suspend fun update(key: Preferences.Key<String>, value: String) {
        context.dataStore.edit { it[key] = value }
    }
}
