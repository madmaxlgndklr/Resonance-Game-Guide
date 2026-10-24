# Changelog

All notable changes to this project are documented here.

The format follows [Keep a Changelog](https://keepachangelog.com/en/1.1.0/),
and this project uses [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

The app's `versionName` in `app/build.gradle.kts` matches the latest released
version below; `versionCode` increments by one for every release.

## [Unreleased]

### Added
- Nothing yet.

## [0.1.0] - 2026-10-24

First scaffold of the Android app. Verified to build from a clean checkout
(`./gradlew clean :app:assembleDebug :app:testDebugUnitTest :app:lintDebug`
passes with no lint errors), but the guide content is placeholder sample data.

### Added
- **Project structure** — single-module Gradle build (`:app`) with a Kotlin DSL
  configuration and a version catalog in `gradle/libs.versions.toml`.
  Gradle wrapper 8.11.1, AGP 8.7.3, Kotlin 2.0.21, `compileSdk`/`targetSdk` 35,
  `minSdk` 26, Java/JVM target 17.
- **`MainActivity`** — edge-to-edge window, a request for the display's fastest
  refresh mode at the current resolution (120 Hz on the Galaxy S24 Ultra), the
  Material 3 theme, and the app's navigation graph.
- **Navigation** — type-safe Navigation Compose routes (`@Serializable`) with a
  bottom navigation bar for the Walkthrough, Bestiary, Builds and Items tabs.
  Each tab keeps its own back stack and scroll position.
- **Theme** — Material 3 colour schemes (light and dark) in crystal blue, gold
  and violet, serif display/headline typography, and optional Material You
  dynamic colour (off by default).
- **Room database** (schema version 1) — tables for chapters, quests, quest
  steps, bosses, items and character builds, plus a separate `progress` table.
  Schemas are exported to `app/schemas/`.
- **Progress tracking** — check off quest steps, collected items and defeated
  bosses. State persists across launches and survives guide-content updates,
  and is included in Android cloud backup and device-to-device transfer.
- **MVVM layer** — `GuideRepository` as the single source of truth over Room,
  with one `ViewModel` per screen exposing `StateFlow` UI state through
  coroutines.
- **Walkthrough tab** — chapters with sticky headers, per-quest progress bars,
  and a quest detail screen with a step checklist and a mark-all action.
- **Bestiary tab** — boss cards with level, HP, weaknesses, resistances, drops
  and strategy; search by name, location or weakness; a defeated toggle.
- **Builds tab** — character build cards with role, skills and recommended
  gear, in an adaptive grid that reflows in landscape and split-screen.
- **Items tab** — item list with locations, category filters and a
  hide-collected toggle.
- **Immersive media viewer** — full-screen map and sprite viewer that hides the
  status and navigation bars, with pinch and double-tap zoom, pan clamping, and
  overlay controls that stay clear of the camera cutout.
- **Asset pipeline** — Coil 3 image loading from a configurable local directory
  (`resonance.assetsDir`, default `/home/madmaxlgndklr/Git/Resonance-Assets`)
  packaged into the APK, with a sideload path on the device that takes
  precedence. `scripts/push-assets.sh` pushes art over adb without a rebuild.
- **Release configuration** — R8 minification and resource shrinking, with
  ProGuard keep rules for the serializable navigation routes.
- **Placeholder guide content** in `SeedData.kt`: 3 chapters, 6 quests,
  18 steps, 4 bosses, 10 items and 4 character builds.
- **Backup rules for Android 11 and below** (`backup_rules.xml`), alongside the
  Android 12+ data-extraction rules, so progress is backed up on every
  supported release.
- **Exported Room schema** at
  `app/schemas/com.madmaxlgndklr.resonanceguide.data.local.GuideDatabase/1.json`,
  committed as the baseline for future migrations.
- **Lint configuration** (`app/lint.xml`) demoting version-update notices to
  informational, since dependency versions are pinned in the version catalog.

### Known limitations
- All guide content is invented sample data and must be replaced with verified
  information before release.
- No Room `Migration` objects exist yet. Any schema change needs one, or users
  lose their saved progress.
- No automated tests and no CI workflow yet.
- Map and sprite art is not included in this repository.

[Unreleased]: https://github.com/madmaxlgndklr/Resonance-Game-Guide/compare/v0.1.0...HEAD
[0.1.0]: https://github.com/madmaxlgndklr/Resonance-Game-Guide/releases/tag/v0.1.0
