package com.piashmsu.tvapk.data

/** A single live-TV channel parsed from an M3U playlist. */
data class Channel(
    val id: String,
    val name: String,
    val logo: String?,
    val group: String,
    val streamUrl: String,
    val tvgId: String? = null,
    val language: String? = null,
    val country: String? = null,
)

/** A movie or video-on-demand entry. */
data class Movie(
    val id: String,
    val title: String,
    val poster: String?,
    val streamUrl: String,
    val genre: String,
    val language: String,
    val year: Int?,
    val description: String?,
    val durationMinutes: Int?,
    val rating: Double?,
    val backdrop: String?,
)

/** UI-friendly grouping of items by category title. */
data class Category<T>(
    val title: String,
    val items: List<T>,
)
