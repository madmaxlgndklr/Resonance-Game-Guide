package com.madmaxlgndklr.resonanceguide.data.local

/**
 * The version 2 schema as DDL, used by `MIGRATION_1_2` to recreate the
 * database after dropping the version 1 tables.
 *
 * Every statement is the `createSql` of an entity or index in
 * `app/schemas/com.madmaxlgndklr.resonanceguide.data.local.GuideDatabase/2.json`,
 * verbatim and in Room's own order: each table followed by its indices. Room
 * validates the resulting schema against that file, so a single character out
 * of place fails the migration at runtime.
 *
 * Do not hand-edit. This list belongs to version 2 and does not change: a
 * schema change gets a new exported version and a migration of its own.
 * `GuideSchemaV2Test` checks it still matches 2.json, which keeps the drift a
 * build failure rather than a crash on a user's upgrade.
 *
 * It lives apart from [GuideDatabase] so a plain JVM test can read it without
 * loading Room's Android classes.
 */
internal object GuideSchemaV2 {
    val STATEMENTS: List<String> = listOf(
        "CREATE TABLE IF NOT EXISTS `chapters` (`id` TEXT NOT NULL, `chapterNumber` INTEGER NOT NULL, `title` TEXT NOT NULL, `region` TEXT, `recommendedLevel` TEXT, `storyBosses` TEXT NOT NULL, `optionalFights` TEXT NOT NULL, `mapAsset` TEXT, PRIMARY KEY(`id`))",
        "CREATE TABLE IF NOT EXISTS `objectives` (`id` TEXT NOT NULL, `chapterId` TEXT NOT NULL, `step` INTEGER NOT NULL, `text` TEXT NOT NULL, `dungeonPuzzleSolution` TEXT, PRIMARY KEY(`id`), FOREIGN KEY(`chapterId`) REFERENCES `chapters`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE )",
        "CREATE INDEX IF NOT EXISTS `index_objectives_chapterId` ON `objectives` (`chapterId`)",
        "CREATE TABLE IF NOT EXISTS `side_quests` (`id` TEXT NOT NULL, `chapterNumber` INTEGER, `title` TEXT NOT NULL, `requester` TEXT, `location` TEXT, `region` TEXT, `prerequisites` TEXT, `rewards` TEXT NOT NULL, `objectives` TEXT NOT NULL, PRIMARY KEY(`id`))",
        "CREATE INDEX IF NOT EXISTS `index_side_quests_chapterNumber` ON `side_quests` (`chapterNumber`)",
        "CREATE TABLE IF NOT EXISTS `enemies` (`id` TEXT NOT NULL, `name` TEXT NOT NULL, `enemyType` TEXT, `level` INTEGER, `hp` INTEGER, `staggerGauge` INTEGER, `weaknesses` TEXT NOT NULL, `resistances` TEXT NOT NULL, `drops` TEXT NOT NULL, `stealables` TEXT NOT NULL, `traits` TEXT, `exp` INTEGER, `gil` INTEGER, `spriteAsset` TEXT, PRIMARY KEY(`id`))",
        "CREATE INDEX IF NOT EXISTS `index_enemies_enemyType` ON `enemies` (`enemyType`)",
        "CREATE INDEX IF NOT EXISTS `index_enemies_name` ON `enemies` (`name`)",
        "CREATE TABLE IF NOT EXISTS `optional_bosses` (`id` TEXT NOT NULL, `name` TEXT NOT NULL, `kind` TEXT NOT NULL, `level` INTEGER, `hp` INTEGER, `staggerGauge` INTEGER, `location` TEXT, `weaknesses` TEXT NOT NULL, `resistances` TEXT NOT NULL, `rewards` TEXT NOT NULL, `strategy` TEXT, `spriteAsset` TEXT, PRIMARY KEY(`id`))",
        "CREATE INDEX IF NOT EXISTS `index_optional_bosses_kind` ON `optional_bosses` (`kind`)",
        "CREATE TABLE IF NOT EXISTS `visions` (`id` TEXT NOT NULL, `name` TEXT NOT NULL, `origin` TEXT, `role` TEXT, `damageType` TEXT, `resonanceSkill` TEXT, `elements` TEXT NOT NULL, `chapter` INTEGER, `method` TEXT, `unlockRoute` TEXT, `spriteAsset` TEXT, PRIMARY KEY(`id`))",
        "CREATE INDEX IF NOT EXISTS `index_visions_role` ON `visions` (`role`)",
        "CREATE TABLE IF NOT EXISTS `vision_skills` (`visionId` TEXT NOT NULL, `skillId` TEXT NOT NULL, `name` TEXT NOT NULL, `category` TEXT, `description` TEXT, `masteryRank` INTEGER, `apCost` INTEGER, `awakeningTier` INTEGER, PRIMARY KEY(`visionId`, `skillId`), FOREIGN KEY(`visionId`) REFERENCES `visions`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE )",
        "CREATE INDEX IF NOT EXISTS `index_vision_skills_visionId` ON `vision_skills` (`visionId`)",
        "CREATE TABLE IF NOT EXISTS `equipment` (`id` TEXT NOT NULL, `name` TEXT NOT NULL, `slot` TEXT NOT NULL, `equipType` TEXT, `physAtk` INTEGER, `magAtk` INTEGER, `defense` INTEGER, `magDef` INTEGER, `speed` INTEGER, `sanctity` INTEGER, `effect` TEXT, `acquisitionMethod` TEXT, `sellPrice` INTEGER, PRIMARY KEY(`id`))",
        "CREATE INDEX IF NOT EXISTS `index_equipment_slot` ON `equipment` (`slot`)",
        "CREATE INDEX IF NOT EXISTS `index_equipment_name` ON `equipment` (`name`)",
        "CREATE TABLE IF NOT EXISTS `items` (`id` TEXT NOT NULL, `name` TEXT NOT NULL, `category` TEXT, `description` TEXT, `acquisitionMethod` TEXT, PRIMARY KEY(`id`))",
        "CREATE INDEX IF NOT EXISTS `index_items_category` ON `items` (`category`)",
        "CREATE TABLE IF NOT EXISTS `tablet_shards` (`id` TEXT NOT NULL, `number` INTEGER NOT NULL, `chapter` INTEGER, `region` TEXT, `exactLocationDescription` TEXT, `missableWarning` TEXT, `notes` TEXT, PRIMARY KEY(`id`))",
        "CREATE INDEX IF NOT EXISTS `index_tablet_shards_chapter` ON `tablet_shards` (`chapter`)",
        "CREATE TABLE IF NOT EXISTS `oblivion_tiers` (`id` TEXT NOT NULL, `chamber` TEXT NOT NULL, `totalShards` INTEGER NOT NULL, `rewardName` TEXT NOT NULL, `rewardDescription` TEXT, `quantity` INTEGER, PRIMARY KEY(`id`))",
        "CREATE INDEX IF NOT EXISTS `index_oblivion_tiers_totalShards` ON `oblivion_tiers` (`totalShards`)",
        "CREATE TABLE IF NOT EXISTS `progress` (`type` TEXT NOT NULL, `entityId` TEXT NOT NULL, `completed` INTEGER NOT NULL, `updatedAt` INTEGER NOT NULL, PRIMARY KEY(`type`, `entityId`))",
    )
}
