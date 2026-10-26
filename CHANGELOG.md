# Changelog

All notable changes to this project are documented here.

The format follows [Keep a Changelog](https://keepachangelog.com/en/1.1.0/),
and this project uses [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

The app's `versionName` in `app/build.gradle.kts` matches the latest released
version below; `versionCode` increments by one for every release.

## [Unreleased]

### Added
- A debug build for sideloading at
  `dist/resonance-guide-v0.1.0-debug.apk`, with its checksum and install
  instructions in `dist/README.md`.

### Changed
- `dialogueChoices` is now recorded as **deferred by decision** rather than
  pending. The table, its pydantic schema and its Kotlin model all stay in
  place so the stat-bonus conversation options can be added later without a
  schema change; only the data is absent, and `meta.coverage.gaps` says so.

### Added (in 0.1.0, listed below)
- **CI workflow** (`.github/workflows/ci.yml`) on every push to `main` and
  every pull request, in three jobs:
  - **Build** — `assembleDebug`, `testDebugUnitTest` and `lintDebug`, with the
    lint report parsed so any error is annotated on the PR. Uploads the APK and
    the test and lint reports as artifacts.
  - **Dataset** — runs `validate_json.py`, and regenerates `GuideModels.kt` to
    fail if it has drifted from `schema.py`. That drift is what caused the
    decode failure fixed in this release, so it is now a build error. CI never
    crawls: both checks work offline against the committed dataset.
  - **Instrumented** — boots an API 35 emulator and runs
    `connectedDebugAndroidTest`, which is the only place `MigrationTest` and
    `GuideSeederTest` execute.
- `lint { abortOnError = true }` in `app/build.gradle.kts`, making explicit
  that a lint error fails the build while advisory warnings do not.
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
- `dialogueChoices` is the only empty table left.
- `dialogueChoices` is empty: no permitted source publishes the stat-bonus
  conversation options.
- The editorial "best items to steal" list is not reproduced; the dataset
  instead carries the per-enemy steal tables and rates, which are factual.
- Equipment counts disagree between sources (82/36/51 per IGN versus
  250/108/96 per atma.gg). The dataset carries the figures it could verify and
  records both.

### Fixed
- **`MIGRATION_1_2` left the database with no tables.** It dropped the version
  1 tables but never created the version 2 ones, and Room creates tables only
  when a database is first created, never on an upgrade path. Any user
  upgrading from a version 1 install would have ended up with an empty
  database and a failed schema validation. The migration now recreates the
  version 2 schema from `GuideSchemaV2`, which holds the `createSql` of every
  entity and index in the exported `2.json` verbatim. Caught by the new CI
  emulator job on its first run — the migration had been merged unverified.
- `GuideSchemaV2Test` (JVM) compares that DDL against the exported schema, so
  drift is a build failure rather than a crash on a user's upgrade.
- **`MigrationTest` never ran the migration.** It called
  `runMigrationsAndValidate` without passing `MIGRATION_1_2` in;
  `MigrationTestHelper` does not read the database's own migration list, so
  Room reported that a migration from 1 to 2 was required but not found and
  the test failed before reaching any of the schema work. The migration is now
  `internal` and handed to the helper explicitly.
- The emulator job now prints each instrumented test's failure message on a
  red run. `--stacktrace` had filled the log with Gradle internals and buried
  the actual assertion, which cost a diagnosis round.

### Changed
- **Room schema version 2: string primary keys.** Every content table is now
  keyed by the dataset's own stable snake_case id, and `progress.entityId` with
  it, so a check-off survives a content update. Version 1's integer keys could
  not be mapped onto them -- `5` identified nothing but a position in a
  hand-written list that no longer exists -- so `MIGRATION_1_2` drops the old
  tables, `progress` included, and the seeder repopulates from the asset.
  Version 1 was never released, so the only state lost is from pre-release test
  installs. There is still no `fallbackToDestructiveMigration`: future
  migrations must preserve `progress`, which is the whole point of the move.
- **Content tables now mirror the dataset**: `chapters`, `objectives`,
  `side_quests`, `enemies`, `optional_bosses` (Cave Masters and Espers),
  `visions`, `vision_skills`, `equipment`, `items`, `tablet_shards` and
  `oblivion_tiers`, replacing the old chapter/quest/step/boss/build shape.
- **`GuideSeeder`** loads `assets/ff_resonance_guide.json` into Room once per
  `datasetVersion`, and `SeedData.kt` is deleted.
- **Screens rebuilt on the new tables.** Walkthrough lists chapters with
  objective and shard progress; a chapter opens to its objective checklist, its
  side quests and its Tablet Shards with their gating warnings. Bestiary covers
  all 596 enemies plus Cave Masters and Espers, searchable by name, weakness,
  drop or steal. The Visions tab shows each Vision's skills with Mastery Rank,
  AP cost and Awakening tier. Items shows consumables and all 454 equipment
  rows with stats and acquisition routes.

### Tests
- `GuideDatasetTest` (JVM, runs in `./gradlew test`) parses the shipped asset
  with the generated models and checks collection counts, id format and
  uniqueness, shard numbering, mastery-rank values, and that every coverage
  shortfall is recorded. It caught a real defect before release: the generator
  mapped the mixed-type forge-weapon objects to `Map<String, String>`, which
  failed to decode and would have crashed the app on first launch. `Any` now
  maps to `JsonElement`.
- `MigrationTest` and `GuideSeederTest` (instrumented) cover the 1 -> 2
  migration against the exported schema, and seeding plus the guarantee that a
  content refresh leaves progress intact. These need a device:
  `./gradlew :app:connectedDebugAndroidTest`.

### Not done, deliberately
- **No artwork is bundled.** `assetsManifest` records official art by URL with
  its licence status and `bundled: false`. Press and promotional material is
  licensed for editorial coverage, not for redistribution inside an
  application, and the Wikipedia cover art is non-free media. See
  `scripts/guide_pipeline/README.md`.
- The instrumented tests had not been run as of the schema v2 commit; the CI
  workflow added here is what runs them. Check the first green run of the
  `Instrumented tests (emulator)` job before trusting the migration.

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
