# Resonance Game Guide

[![Version](https://img.shields.io/badge/version-0.1.0-blue)](CHANGELOG.md)
[![Platform](https://img.shields.io/badge/platform-Android%208.0%2B-3DDC84)](#requirements)
[![Kotlin](https://img.shields.io/badge/Kotlin-2.0.21-7F52FF)](https://kotlinlang.org)
[![Compose BOM](https://img.shields.io/badge/Compose%20BOM-2024.12.01-4285F4)](https://developer.android.com/jetpack/compose)

An offline, interactive guide for *Final Fantasy Resonance*, built as a native
Android app and tuned for the Samsung Galaxy S24 Ultra.

> **Status: 0.1.0 — early scaffold.** The architecture, screens and database are
> in place, but **all guide content is placeholder sample data**. See
> [CHANGELOG.md](CHANGELOG.md) for the full version history and
> [Roadmap](#roadmap) for what comes next.

---

## Contents

- [Features](#features)
- [Requirements](#requirements)
- [Building](#building)
- [Project layout](#project-layout)
- [Architecture](#architecture)
- [Guide content](#guide-content)
- [Maps and sprites](#maps-and-sprites)
- [Galaxy S24 Ultra notes](#galaxy-s24-ultra-notes)
- [Versioning](#versioning)
- [Roadmap](#roadmap)

---

## Features

| Tab | What it does |
| --- | --- |
| **Walkthrough** | Chapters with sticky headers, quest progress bars, and a step-by-step checklist per quest with hints. |
| **Bestiary** | Boss cards with level, HP, weaknesses, resistances, drops and strategy. Search by name, location or weakness; mark bosses defeated. |
| **Builds** | Character build cards with role, skills and recommended gear, in a grid that reflows in landscape and split-screen. |
| **Items** | Item locations with category filters and a hide-collected toggle. |

Across all tabs:

- **Progress tracking** — check off quest steps, collected items and defeated
  bosses. Progress persists across launches and survives guide-content updates.
- **Fully offline** — everything is stored locally in Room. No network
  permission is declared.
- **Immersive media viewer** — maps and sprites open full-screen with the system
  bars hidden, with pinch and double-tap zoom.

## Requirements

| | |
| --- | --- |
| Android Studio | Ladybug (2024.2.1) or newer |
| JDK | 17 or newer |
| Android SDK | Platform 35, Build-Tools 35.0.0 |
| Device | Android 8.0 (API 26) or newer |

## Building

```bash
git clone https://github.com/madmaxlgndklr/Resonance-Game-Guide.git
cd Resonance-Game-Guide

./gradlew :app:assembleDebug     # build the debug APK
./gradlew :app:installDebug      # install on a connected device
./gradlew :app:assembleRelease   # minified release build (needs signing config)
```

Open the project in Android Studio and it will sync and build the same way.
The SDK location is read from `local.properties` or `$ANDROID_HOME`.

## Project layout

```
app/src/main/java/com/madmaxlgndklr/resonanceguide/
├── MainActivity.kt                 edge-to-edge setup, 120 Hz request, NavHost + bottom bar
├── ResonanceGuideApplication.kt    app container, content seeding, Coil ImageLoader
├── AppContainer.kt                 manual dependency injection
├── assets/GuideAssets.kt           resolves map/sprite paths for Coil
├── data/
│   ├── local/                      Room entities, DAOs, database, type converters, SeedData
│   └── repository/                 GuideRepository — single source of truth
├── navigation/Routes.kt            @Serializable routes and the four top-level tabs
└── ui/
    ├── theme/                      Material 3 colour schemes and typography
    ├── components/                 ImmersiveMode, GuideImage, shared composables
    ├── walkthrough/                chapter list + quest detail with step checklist
    ├── bestiary/                   boss stats, search, defeated toggle
    ├── builds/                     character builds in an adaptive grid
    ├── items/                      item locations, filters, collected toggle
    └── viewer/                     full-screen immersive pinch-zoom viewer
```

Supporting files:

| Path | Purpose |
| --- | --- |
| `gradle/libs.versions.toml` | Version catalog — the single place to bump dependency versions. |
| `app/schemas/` | Exported Room schemas, one JSON file per database version. Commit these. |
| `scripts/push-assets.sh` | Sideload maps and sprites onto a device over adb. |
| `CHANGELOG.md` | Version history. |

## Architecture

MVVM, with a unidirectional data flow:

```
Room (DAOs)  ──►  GuideRepository  ──►  ViewModel (StateFlow)  ──►  Composable
     ▲                                                                   │
     └──────────────────── user actions (check off, filter) ─────────────┘
```

- **Room** holds both guide content and user progress, in separate tables, and
  exposes everything as `Flow`, so the UI updates the moment a row changes.
- **`GuideRepository`** is the only thing the ViewModels talk to. It owns
  content seeding and all progress writes.
- **ViewModels** combine flows into one immutable UI state per screen and expose
  it as a `StateFlow`, collected with `collectAsStateWithLifecycle()`.
- **Composables** are stateless with respect to data: they render the UI state
  and send events back up.

### Dependencies

| Library | Version | Used for |
| --- | --- | --- |
| Jetpack Compose | BOM 2024.12.01 | UI toolkit, Material 3 |
| Navigation Compose | 2.8.5 | Type-safe routes, bottom-bar back stacks |
| Room | 2.6.1 (KSP) | Local database |
| Coil | 3.0.4 | Image loading for maps and sprites |
| kotlinx.coroutines | 1.9.0 | Async work and flows |
| kotlinx.serialization | 1.7.3 | Navigation route and list-column encoding |

## Guide content

Content lives in
[`data/local/SeedData.kt`](app/src/main/java/com/madmaxlgndklr/resonanceguide/data/local/SeedData.kt)
and is written into Room on first launch.

> ⚠️ **The bundled content is invented placeholder data** that exists only to
> exercise the schema and screens. Replace it with verified guide data before
> any release.

To update content:

1. Edit the lists in `SeedData.kt`, keeping IDs stable so existing progress
   still matches.
2. Increment `SeedData.VERSION`.
3. On the next launch, installed copies rewrite their content tables. The
   `progress` table is left untouched, so users keep their check-offs.

### Database migrations

The schema version is **1**, exported to `app/schemas/1.json`.

Any change to a table requires a Room `Migration` added in
`GuideDatabase.build()`. There is deliberately **no**
`fallbackToDestructiveMigration()`, because it would erase users' progress.

## Maps and sprites

High-resolution art is **not** stored in this repository. The build packages
whatever is in the directory named by the `resonance.assetsDir` Gradle property:

```
/home/madmaxlgndklr/Git/Resonance-Assets/     ← default location
├── maps/
│   ├── chapter_00.webp
│   ├── chapter_01.webp
│   └── chapter_02.webp
└── sprites/
    ├── bosses/hollow_sentinel.webp …
    └── characters/aren.webp …
```

Asset paths in `SeedData.kt` are relative to this root. Change the location in
`gradle.properties`, or per build:

```bash
./gradlew :app:assembleDebug -Presonance.assetsDir=/path/to/art
```

If the directory does not exist, the build still succeeds and the app shows
placeholder images.

**Iterating on art without rebuilding:**

```bash
scripts/push-assets.sh [assets-dir]
```

This pushes files to `Android/data/com.madmaxlgndklr.resonanceguide/files/guide/`
on the device, which the app checks *before* its bundled copies.

**Format:** WebP is recommended for maps. The viewer decodes up to 4096 px on
the long edge — sharp when zoomed on a QHD+ panel, without exhausting memory.

## Galaxy S24 Ultra notes

- **Edge-to-edge** throughout. The media viewer hides the status and navigation
  bars (swipe from an edge to peek) and restores them on exit.
- **120 Hz** — `MainActivity` requests the display's fastest mode at the current
  resolution. Lists use stable keys and `contentType` hints for smooth flinging
  at QHD+.
- **Large screens** — the Builds tab uses an adaptive grid, so it reflows in
  landscape, split-screen and DeX.
- **Camera cutout** — viewer controls are inset to stay clear of it.

## Versioning

This project follows [Semantic Versioning](https://semver.org). The app's
`versionName` matches the latest entry in [CHANGELOG.md](CHANGELOG.md), and
`versionCode` increments by one per release.

| Version | Date | Summary |
| --- | --- | --- |
| [0.1.0](CHANGELOG.md#010---2026-10-24) | 2026-10-24 | Initial scaffold: architecture, four tabs, Room database, immersive viewer. |

To cut a release: update `CHANGELOG.md`, bump `versionName`/`versionCode` in
`app/build.gradle.kts`, commit, then tag it:

```bash
git tag -a v0.1.0 -m "v0.1.0"
git push origin v0.1.0
```

## Roadmap

- [ ] Replace placeholder content with verified guide data
- [ ] Move guide content to a JSON asset so it can be edited without Kotlin
- [ ] Add map and sprite art
- [ ] Reset-progress action (already supported by the repository layer)
- [ ] Instrumented tests for the database and progress persistence
- [ ] GitHub Actions workflow to build on every push
- [ ] Baseline profile for faster startup and smoother first-run scrolling
