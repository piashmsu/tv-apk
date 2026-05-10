<div align="center">

<img src="app/src/main/res/mipmap-xhdpi/ic_launcher.webp" width="120" />

# 🎬 TV APK — Vibe Edition

### Live TV • IPTV Player • Movie Streaming

*A modern Android live-TV & movie player — built with Jetpack Compose, Material 3 & Media3*

[![Android CI](https://github.com/piashmsu/tv-apk/actions/workflows/android.yml/badge.svg)](https://github.com/piashmsu/tv-apk/actions)
![Min SDK](https://img.shields.io/badge/API-23%2B-brightgreen)
![Kotlin](https://img.shields.io/badge/Kotlin-1.9.24-blueviolet)
![Compose](https://img.shields.io/badge/Compose-BOM%202024.09-informational)

</div>

---

## ✨ Features

### 📺 Live TV
| Feature | Description |
|---|---|
| **Multi-source IPTV** | Load multiple M3U/M3U8 playlists — URL or local file |
| **Bangladesh TV** | Pre-configured BD channels + EPG (iptv-org) |
| **India TV** | Hindi/regional channels + EPG |
| **Hollywood Movies** | English movie channels |
| **World Sports** | Global sports channels |
| **Channel Groups** | Auto-grouped by category, filter chips |
| **Online/Offline Status** | Probe reachability, green/red badges |
| **Re-check Offline** | Retry only offline channels |
| **EPG Now Playing** | Current + upcoming programme info |
| **EPG Timeline View** | Grid-style TV guide — channels × time slots |
| **Favorites** | Star channels for quick access |
| **Recent Channels** | Recently watched history |
| **Manual Probe** | Full user control — check when YOU want |
| **File Picker** | Load .m3u files from device storage |

### 🎥 Player
| Feature | Description |
|---|---|
| **ExoPlayer / Media3** | HLS, DASH, SmoothStreaming, RTSP, progressive |
| **PiP (Picture-in-Picture)** | Mini window — Home button or PiP button |
| **Background Audio** | Listen with screen off, notification controls |
| **Loading Indicator** | Smooth transition — no black screen |
| **Custom Controls** | Play/pause, ±10s seek, sleep timer |
| **Aspect Ratio** | Fit / Fill / Zoom / 16:9 / 4:3 |
| **Catch-up / Rewind** | Rewind live streams with catchup support |
| **Recording** | Record streams to device storage |
| **Sleep Timer** | Auto-pause after 15/30/60/90 min |
| **Gesture Controls** | Tap controls, double-tap edges to seek |

### 🎥 Movies
| Feature | Description |
|---|---|
| **JSON Catalog** | Load from user-supplied URL |
| **Genre Filter** | Browse by genre |
| **Poster Art** | Auto-loaded via Coil |
| **Backdrop Hero** | Featured movie showcase |

### 🎨 UI / UX
| Feature | Description |
|---|---|
| **Material 3 Design** | Indigo-aqua-neon "Vibe" theme |
| **Gradient Backgrounds** | Smooth purple-blue-neon theme |
| **Edge-to-Edge** | Immersive full-screen experience |
| **Bottom Nav Pill** | Floating glassmorphic navigation bar |
| **Android TV Ready** | Leanback launcher support |
| **TV Remote Optimized** | D-pad navigable |
| **Search** | Global cross-content search |

### ⚙️ System
| Feature | Description |
|---|---|
| **Auto-refresh** | Background playlist refresh (WorkManager) |
| **EPG Auto-load** | XMLTV guides per source |
| **Debug Logging** | Crash log viewer in Settings |
| **Crash Protection** | Graceful error handling, no silent crashes |
| **40% Faster Probe** | 2s timeouts, 20 concurrent checks |
| **Race-condition Safe** | Mutex-locked refreshes |

---

## 🚀 Build & Install

```bash
# Clone
git clone https://github.com/piashmsu/tv-apk.git
cd tv-apk

# Build debug
./gradlew :app:assembleDebug

# Build release
./gradlew :app:assembleRelease
```

| Config | Value |
|---|---|
| `compileSdk` / `targetSdk` | 34 |
| `minSdk` | 23 (Android 6.0+) |
| `AGP` | 8.5.2 |
| `Kotlin` | 1.9.24 |
| `Compose` | BOM 2024.09.02 |
| `Media3` | 1.4.1 |

APK found at: `app/build/outputs/apk/{debug,release}/`

---

## 🛠 Tech Stack

```
Jetpack Compose ─── UI framework
Material 3 ──────── Design system
Navigation Compose ─ Screen routing
Media3 / ExoPlayer ─ Video & audio playback
DataStore ───────── Preferences persistence
OkHttp ──────────── Networking
Coil ────────────── Image loading
WorkManager ─────── Background refresh
Kotlin Coroutines ─ Async operations
```

---

## 🗺 Future Roadmap

| Priority | Feature | Status |
|---|---|---|
| 🔴 P1 | **Android TV Leanback UI** — full TV remote optimized interface | Planned |
| 🔴 P1 | **VPN / Proxy support** — per-source proxy config | Planned |
| 🔴 P1 | **Stream quality selector** — auto/manual bitrate | Planned |
| 🟡 P2 | **Offline download** — download movies for offline viewing | Planned |
| 🟡 P2 | **Favorites show alert** — EPG-based notification | Planned |
| 🟡 P2 | **Parental lock** — PIN-protected channels | Planned |
| 🟢 P3 | **Home screen widget** — favorite channels widget | Planned |
| 🟢 P3 | **Multiple themes** — Light/Dark/AMOLED | Planned |
| 🟢 P3 | **Subtitle support** — external .srt/.vtt loading | Planned |
| 🟢 P3 | **Chromecast / DLNA** — cast to TV | Planned |

---

## 📂 Project Structure

```
app/src/main/java/com/piashmsu/tvapk/
├── MainActivity.kt          ← PiP, notifications, app entry
├── TvApkApp.kt              ← Application, DI, exception handler
├── DebugLog.kt              ← Crash log file writer
├── data/
│   ├── Models.kt            ← Channel, Movie, EPG, PlaylistSource
│   ├── ChannelRepository.kt ← M3U loading, channel probing
│   ├── MovieRepository.kt   ← JSON catalog loader
│   ├── EpgRepository.kt     ← XMLTV EPG parser
│   ├── M3UParser.kt         ← M3U/M3U8 parser
│   ├── EpgParser.kt         ← XMLTV parser
│   ├── AppPrefs.kt          ← DataStore preferences
│   └── AppContainer.kt      ← DI container
├── ui/
│   ├── TvApkRoot.kt         ← NavHost + bottom bar
│   ├── AppViewModels.kt     ← Shared ViewModel
│   ├── theme/               ← Material 3 "Vibe" theme
│   ├── components/          ← ChannelTile, MovieCard, GenreChip
│   └── screens/
│       ├── HomeScreen.kt    ← Hero, quick actions, rows
│       ├── LiveTvScreen.kt  ← Channel list + probe UI
│       ├── EpgTimelineScreen.kt ← TV guide grid
│       ├── MoviesScreen.kt  ← Genre-filtered movie grid
│       ├── SearchScreen.kt  ← Global search
│       ├── SettingsScreen.kt← Playlists, debug logs
│       └── PlayerScreen.kt  ← PiP, recording, full player
├── player/
│   └── PlayerFactory.kt     ← ExoPlayer builder
├── record/
│   └── RecordingService.kt  ← HLS/progressive recorder
└── work/
    └── PlaylistRefreshWorker.kt ← WorkManager auto-refresh
```

---

## 👨‍💻 Developer

<div align="center">

### Shorif Uddin Piash

**[fb.com/piashmsuf](https://fb.com/piashmsuf)**

*TV APK — Vibe Edition | Made with ❤️ in Bangladesh*

</div>

---

## ⚖️ Disclaimer

TV APK is a **player shell**. It does NOT host, distribute, or provide any streams, channels, or content. The user is solely responsible for ensuring the streams they configure are legal to consume in their jurisdiction.

