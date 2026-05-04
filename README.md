# TV APK

A modern Android **player shell** for watching IPTV / live-TV channels and
movies (Hindi & Bangla dubbed catalogs, regional content, etc.) in a clean
Material 3 interface — built with Jetpack Compose and Media3 / ExoPlayer.

> **Bring your own content.** TV APK does **not** ship with any preconfigured
> channels, movies, or stream URLs. The user supplies their own (legal) IPTV
> M3U playlist link and an optional movie-catalog JSON URL via Settings; the
> app simply parses and plays them.

## Features

- **Live TV** — Loads any standard IPTV M3U / M3U8 playlist (parses
  `tvg-name`, `tvg-logo`, `tvg-id`, `tvg-language`, `tvg-country`,
  `group-title`). Channels are grouped by category with quick filter chips.
- **Movies** — Optional movie catalog loaded from a user-supplied JSON URL.
  Adaptive grid layout with poster art, genre & language filters. Designed for
  Hindi/Bangla dubbed Hollywood-style catalogs but works for any catalog
  shape (see schema below).
- **Search** — Global search across channels and movies.
- **Modern player** — Full-screen ExoPlayer with HLS / DASH /
  SmoothStreaming / RTSP / progressive support, custom controls, automatic
  landscape orientation, and edge-to-edge immersive mode.
- **Material 3 design** — Custom indigo-aqua-sunset theme, gradient
  backgrounds, adaptive icon, edge-to-edge layout, large dynamic type.
- **Android TV / Leanback ready** — Manifest declares `LEANBACK_LAUNCHER` so
  the app appears on Android TV launchers.

## Screens

- **Home** — Hero featured movie, quick actions, top live channels, browse
  rows by genre, developer credit footer.
- **Live TV** — Searchable, category-filtered list of live channels.
- **Movies** — Adaptive grid with genre filter chips.
- **Search** — Cross-content search.
- **Settings** — IPTV playlist URL, movie catalog URL, About / Developer.
- **Player** — Full-screen Media3 player with play/pause, +/- 10 s seek,
  buffering and error states.

## Movie catalog JSON shape

The Movies tab fetches an array of objects (one per movie). Common keys are
auto-detected — only `title` (or `name`) and `streamUrl` (or `url`, `stream`,
`src`, `video`) are strictly required.

```json
[
  {
    "id": "tt0133093",
    "title": "The Matrix (Hindi Dub)",
    "poster": "https://example.com/poster.jpg",
    "backdrop": "https://example.com/backdrop.jpg",
    "streamUrl": "https://example.com/stream.m3u8",
    "genre": "Hollywood Hindi Dub",
    "language": "Hindi",
    "year": 1999,
    "duration": 136,
    "rating": 8.7,
    "description": "A computer hacker learns the truth about reality."
  }
]
```

The endpoint may also wrap the array under `results`, `movies`, or `data`.

## Tech stack

- Kotlin 1.9.24, Android Gradle Plugin 8.5.2, Gradle 8.7
- Jetpack Compose (BOM 2024.09.02), Material 3
- Navigation Compose 2.7.7
- Media3 / ExoPlayer 1.4.1 (HLS, DASH, SmoothStreaming, RTSP, OkHttp data source)
- DataStore Preferences 1.1.1
- Coil 2.6.0 for posters & channel logos
- OkHttp 4.12.0 for catalog/playlist fetches

## Build

```bash
./gradlew :app:assembleDebug      # debug APK -> app/build/outputs/apk/debug/
./gradlew :app:assembleRelease    # release APK signed with the debug key
```

The release build is configured with `signingConfig = signingConfigs.getByName("debug")`
so it produces an installable APK out of the box. Replace with a real keystore
before publishing.

`compileSdk` and `targetSdk` are 34, `minSdk` is 23 (Android 6.0+).

## Legal

TV APK is a **player shell**. The user is responsible for ensuring the streams
and catalogs they configure are legal to consume in their jurisdiction. The
project ships with **no** preconfigured streams or content URLs.

## Developer

**Shorif Uddin Piash** — [fb.com/piashmsuf](https://fb.com/piashmsuf)
