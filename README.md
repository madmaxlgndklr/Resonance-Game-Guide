# Resonance-Game-Guide

An offline, interactive guide for *Final Fantasy Resonance*, built as a native Android app and tuned for the Galaxy S24 Ultra.

**Stack:** Kotlin 2.0 · Jetpack Compose (Material 3) · MVVM with ViewModel + Coroutines/Flow · Room · Navigation Compose (type-safe routes) · Coil 3

## Building

Requirements: Android Studio Ladybug or newer (or the Android SDK with platform 35) and JDK 17+.

```bash
./gradlew :app:assembleDebug
./gradlew :app:installDebug   # with the phone connected over USB/Wi-Fi adb
```

## Project layout

```
app/src/main/java/com/madmaxlgndklr/resonanceguide/
├── MainActivity.kt              edge-to-edge setup, 120 Hz request, NavHost + bottom bar
├── ResonanceGuideApplication.kt app container, content seeding, Coil ImageLoader
├── AppContainer.kt              manual DI (database + repository)
├── assets/GuideAssets.kt        resolves map/sprite paths for Coil
├── data/local/                  Room entities, DAOs, database, SeedData
├── data/repository/             GuideRepository (single source of truth)
├── navigation/Routes.kt         @Serializable routes and the four top-level tabs
└── ui/
    ├── theme/                   Material 3 colour scheme and typography
    ├── components/              ImmersiveMode, GuideImage, shared bits
    ├── walkthrough/             chapter list + quest detail with step checklist
    ├── bestiary/                boss stats, weaknesses, search, defeated toggle
    ├── builds/                  character builds (adaptive grid)
    ├── items/                   item locations, category filter, collected toggle
    └── viewer/                  full-screen, immersive, pinch-zoom map/sprite viewer
```

## Data and progress

- Guide content (chapters, quests, steps, bosses, items, builds) is written into Room from `SeedData.kt` on first launch. **The bundled content is placeholder sample data.** Replace it with real guide data, then bump `SeedData.VERSION` so installed copies refresh.
- Check-offs (quest steps, collected items, defeated bosses) are stored in their own `progress` table, so they survive content refreshes and app restarts. Android backup includes the database.
- The schema is exported to `app/schemas/`. Add a `Migration` for every schema change; a destructive fallback would wipe users' progress.

## Maps and sprites

High-resolution art lives outside the repo. By default the build packages everything in

```
/home/madmaxlgndklr/Git/Resonance-Assets/
├── maps/chapter_00.webp ...
└── sprites/
    ├── bosses/hollow_sentinel.webp ...
    └── characters/aren.webp ...
```

into the APK's assets. Paths in `SeedData` are relative to this root. Change the location in `gradle.properties` (`resonance.assetsDir`) or per build with `-Presonance.assetsDir=/path`. If the directory is missing, the build still succeeds and the app shows placeholders.

To try new art without rebuilding, run `scripts/push-assets.sh [dir]`. Files pushed to `Android/data/com.madmaxlgndklr.resonanceguide/files/guide/` take priority over the bundled copies.

WebP is recommended for maps. The viewer decodes at up to 4096 px on the long edge, which stays sharp when zoomed on the QHD+ panel without exhausting memory.

## S24 Ultra notes

- Edge-to-edge everywhere. The map/sprite viewer hides the status and navigation bars (swipe from an edge to peek) and keeps its controls clear of the camera cutout.
- `MainActivity` requests the display's fastest mode at the current resolution (120 Hz). Lists use stable keys and `contentType` for smooth flings. Release builds are R8-minified.
- The Builds tab uses an adaptive grid, so it reflows in landscape and in DeX/split-screen.
