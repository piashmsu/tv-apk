package com.piashmsu.tvapk.data

import kotlinx.coroutines.flow.MutableStateFlow

/**
 * Process-scoped holder for the currently selected playback target. The
 * navigation graph cannot easily round-trip rich [Channel] / [Movie] objects
 * through the back stack (URLs would need to encode HTTP headers, catch-up
 * config, etc.), so the launcher screens stash the target here and the
 * player reads it on entry.
 */
sealed interface PlaybackTarget {
    val title: String
    val streamUrl: String

    data class LiveChannel(val channel: Channel) : PlaybackTarget {
        override val title: String get() = channel.name
        override val streamUrl: String get() = channel.streamUrl
    }

    data class VideoOnDemand(val movie: Movie) : PlaybackTarget {
        override val title: String get() = movie.title
        override val streamUrl: String get() = movie.streamUrl
    }
}

object PlaybackTargetHolder {
    val current = MutableStateFlow<PlaybackTarget?>(null)
}
