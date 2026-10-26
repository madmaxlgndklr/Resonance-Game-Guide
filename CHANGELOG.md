# Changelog

All notable changes to this project are documented here.

The format follows [Keep a Changelog](https://keepachangelog.com/en/1.1.0/),
and this project uses [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

The app's `versionName` in `app/build.gradle.kts` matches the latest released
version below; `versionCode` increments by one for every release.

## [Unreleased]

### Added
- **Real guide dataset** at `app/src/main/assets/ff_resonance_guide.json`
  (954 KiB), replacing the invented `SeedData.kt` sample content. Built from a
  hand-verified seed plus the sub-tables we are permitted to crawl: 9 chapters
  with their objectives, 8 heroes, 26 Visions with learnable skills / Mastery
  Ranks / AP costs, 9 Espers, 33 side quests, 15 Cave Masters, 20 Colosseum
  battles, 12 Chamber of Arms trials, 8 Adamantite sources, 454 equipment
  rows, 88 items, 596 bestiary entries, 798 skills and the 25 Oblivion Palace
  reward tiers. Every trackable record carries an immutable snake_case `id`.
- **Per-record detail**, fetched from each entry's own page: an acquisition
  route and sell price on all 454 equipment rows, and a full stat block
  (HP, stagger gauge, ATK/DEF/MATK/MDEF/SANC/SPD, EXP, Gil), elemental
  weaknesses, traits and drop tables on all 596 bestiary entries, including
  per-enemy steal tables with rates on the 164 enemies that have one.
- **`scripts/guide_pipeline/`** — the pipeline that builds and checks the
  dataset: a source registry that records each site's robots.txt position, a
  fetcher that refuses any host the registry does not clear, pydantic
  validation, deterministic id helpers, a validator covering UTF-8 / schema /
  id uniqueness / foreign keys / counts, and a generator that emits the app's
  Kotlin models from the same schema.
- **`data/model/GuideModels.kt`** — 27 generated `@Serializable` data classes
  mirroring the dataset, plus `GuideDataSource` to load and cache it off the
  main thread.

### Known gaps in the dataset
Recorded in the file itself under `meta.coverage.gaps`, so they are visible to
the app and to anyone reading the JSON:
- **The 99 Tablet Shard locations are absent.** Every source that publishes
  them prohibits automated retrieval — IGN's terms bar scraping and dataset
  creation outright — so the table ships empty rather than invented. It needs
  either licensing or manual entry.
- `dialogueChoices` is empty: no permitted source publishes the stat-bonus
  conversation options.
- The editorial "best items to steal" list is not reproduced; the dataset
  instead carries the per-enemy steal tables and rates, which are factual.
- Equipment counts disagree between sources (82/36/51 per IGN versus
  250/108/96 per atma.gg). The dataset carries the figures it could verify and
  records both.

### Not done, deliberately
- **No artwork is bundled.** `assetsManifest` records official art by URL with
  its licence status and `bundled: false`. Press and promotional material is
  licensed for editorial coverage, not for redistribution inside an
  application, and the Wikipedia cover art is non-free media. See
  `scripts/guide_pipeline/README.md`.
- The Room schema still uses integer primary keys, so the app does not yet
  read the new dataset into its database. Wiring it up means moving the
  content tables to the dataset's string ids (schema version 2).

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
