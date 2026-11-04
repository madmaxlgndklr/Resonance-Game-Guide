package com.madmaxlgndklr.resonanceguide.data.local

/**
 * The tables version 3 adds, as DDL, used by `MIGRATION_2_3`.
 *
 * Version 3 is additive: it introduces `walkthrough_areas` and
 * `dialogue_choices` and changes nothing that version 2 created. So unlike
 * [GuideSchemaV2], which has to recreate a whole schema from scratch, this
 * holds only the new statements -- hence `ADDED_STATEMENTS` rather than
 * `STATEMENTS`, so the two cannot be confused at a call site.
 *
 * Every statement is the `createSql` of an entity or index in
 * `app/schemas/com.madmaxlgndklr.resonanceguide.data.local.GuideDatabase/3.json`,
 * verbatim and in Room's own order: each table followed by its indices. Room
 * validates the resulting schema against that file, so a single character out
 * of place fails the migration at runtime.
 *
 * Do not hand-edit. This list belongs to version 3 and does not change: a
 * schema change gets a new exported version and a migration of its own.
 * `GuideSchemaV3Test` checks it still matches 3.json, which keeps the drift a
 * build failure rather than a crash on a user's upgrade.
 *
 * It lives apart from [GuideDatabase] so a plain JVM test can read it without
 * loading Room's Android classes.
 */
internal object GuideSchemaV3 {
    val ADDED_STATEMENTS: List<String> = listOf(
        "CREATE TABLE IF NOT EXISTS `walkthrough_areas` (`id` TEXT NOT NULL, `chapterId` TEXT NOT NULL, `sortOrder` INTEGER NOT NULL, `name` TEXT NOT NULL, `kind` TEXT, `summary` TEXT, `steps` TEXT NOT NULL, `chests` TEXT NOT NULL, `bosses` TEXT NOT NULL, `shardCount` INTEGER NOT NULL, `sideQuests` TEXT NOT NULL, `notableSteals` TEXT NOT NULL, PRIMARY KEY(`id`), FOREIGN KEY(`chapterId`) REFERENCES `chapters`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE )",
        "CREATE INDEX IF NOT EXISTS `index_walkthrough_areas_chapterId` ON `walkthrough_areas` (`chapterId`)",
        "CREATE TABLE IF NOT EXISTS `dialogue_choices` (`id` TEXT NOT NULL, `chapterNumber` INTEGER, `speaker` TEXT NOT NULL, `prompt` TEXT, `choice` TEXT, `bonus` TEXT, PRIMARY KEY(`id`))",
        "CREATE INDEX IF NOT EXISTS `index_dialogue_choices_chapterNumber` ON `dialogue_choices` (`chapterNumber`)",
    )
}
