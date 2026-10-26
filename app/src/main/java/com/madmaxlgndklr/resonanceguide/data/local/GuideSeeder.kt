package com.madmaxlgndklr.resonanceguide.data.local

import android.content.SharedPreferences
import androidx.core.content.edit
import androidx.room.withTransaction
import com.madmaxlgndklr.resonanceguide.data.model.Guide
import com.madmaxlgndklr.resonanceguide.data.model.GuideDataSource

/**
 * Loads `assets/ff_resonance_guide.json` into the content tables.
 *
 * Runs once per dataset version: [Guide.meta.datasetVersion] is recorded after
 * a successful seed, and a newer version in the asset triggers a refresh. The
 * `progress` table is never touched, so check-offs survive a content update --
 * which works only because every id is derived from stable data rather than
 * list position.
 */
class GuideSeeder(
    private val database: GuideDatabase,
    private val dataSource: GuideDataSource,
    private val prefs: SharedPreferences,
) {
    private val guideDao = database.guideDao()

    suspend fun seedIfNeeded(): SeedResult {
        val guide = dataSource.load()
        val version = guide.meta.datasetVersion
        if (prefs.getString(KEY_DATASET_VERSION, null) == version) {
            return SeedResult.AlreadyCurrent(version)
        }
        write(guide)
        prefs.edit { putString(KEY_DATASET_VERSION, version) }
        return SeedResult.Seeded(version, guide.meta.coverage.gaps)
    }

    private suspend fun write(guide: Guide) = database.withTransaction {
        guideDao.upsertChapters(
            guide.chapters.map { chapter ->
                ChapterEntity(
                    id = chapter.id,
                    chapterNumber = chapter.chapterNumber,
                    title = chapter.title,
                    region = chapter.region,
                    recommendedLevel = chapter.recommendedLevel,
                    storyBosses = chapter.storyBosses,
                    optionalFights = chapter.optionalFights,
                    mapAsset = mapAssetFor(chapter.chapterNumber),
                )
            },
        )
        guideDao.upsertObjectives(
            guide.chapters.flatMap { chapter ->
                chapter.objectives.map { objective ->
                    ObjectiveEntity(
                        id = objective.id,
                        chapterId = chapter.id,
                        step = objective.step,
                        text = objective.text,
                        dungeonPuzzleSolution = objective.dungeonPuzzleSolution,
                    )
                }
            },
        )
        guideDao.upsertSideQuests(
            guide.sideQuests.map { quest ->
                SideQuestEntity(
                    id = quest.id,
                    chapterNumber = quest.chapter,
                    title = quest.title,
                    requester = quest.requester,
                    location = quest.location,
                    region = quest.region,
                    prerequisites = quest.prerequisites,
                    rewards = quest.rewards,
                    objectives = quest.objectives,
                )
            },
        )
        guideDao.upsertEnemies(
            guide.bestiary.map { enemy ->
                EnemyEntity(
                    id = enemy.id,
                    name = enemy.name,
                    enemyType = enemy.enemyType,
                    level = enemy.level,
                    hp = enemy.hp,
                    staggerGauge = enemy.staggerGauge,
                    weaknesses = enemy.weaknesses,
                    resistances = enemy.resistances,
                    drops = enemy.drops.map { it.name },
                    stealables = enemy.stealables.map { it.name },
                    traits = enemy.traits,
                    exp = enemy.exp,
                    gil = enemy.gil,
                    spriteAsset = spriteAsset("bosses", enemy.name),
                )
            },
        )
        guideDao.upsertOptionalBosses(
            guide.caveMasters.map { boss ->
                OptionalBossEntity(
                    id = boss.id,
                    name = boss.name,
                    kind = KIND_CAVE_MASTER,
                    level = boss.level,
                    hp = boss.hp,
                    staggerGauge = boss.staggerGauge,
                    location = boss.location,
                    weaknesses = boss.weaknesses,
                    resistances = emptyList(),
                    rewards = boss.rewards,
                    strategy = boss.strategy,
                    spriteAsset = spriteAsset("bosses", boss.name),
                )
            } + guide.espers.map { esper ->
                OptionalBossEntity(
                    id = esper.id,
                    name = esper.name,
                    kind = KIND_ESPER,
                    level = esper.trialLevel,
                    hp = esper.hpNormal,
                    staggerGauge = esper.staggerGauge,
                    location = esper.location,
                    weaknesses = esper.weaknesses,
                    resistances = esper.resistances,
                    rewards = listOfNotNull(esper.signatureMove),
                    strategy = esper.unlockMethod,
                    spriteAsset = spriteAsset("espers", esper.name),
                )
            },
        )
        guideDao.upsertVisions(
            guide.visions.map { vision ->
                VisionEntity(
                    id = vision.id,
                    name = vision.name,
                    origin = vision.origin,
                    role = vision.role,
                    damageType = vision.damageType,
                    resonanceSkill = vision.resonanceSkill,
                    elements = vision.elements,
                    chapter = vision.chapter,
                    method = vision.method,
                    unlockRoute = vision.unlockRoute,
                    spriteAsset = spriteAsset("characters", vision.name),
                )
            },
        )
        guideDao.upsertVisionSkills(
            guide.visions.flatMap { vision ->
                vision.skills.map { skill ->
                    VisionSkillEntity(
                        visionId = vision.id,
                        skillId = skill.id,
                        name = skill.name,
                        category = skill.category,
                        description = skill.description,
                        masteryRank = skill.masteryRank,
                        apCost = skill.apCost,
                        awakeningTier = skill.awakeningTier,
                    )
                }
            },
        )
        guideDao.upsertEquipment(
            guide.equipment.map { item ->
                EquipmentEntity(
                    id = item.id,
                    name = item.name,
                    slot = item.slot,
                    equipType = item.equipType,
                    physAtk = item.physAtk,
                    magAtk = item.magAtk,
                    defense = item.defense,
                    magDef = item.magDef,
                    speed = item.speed,
                    sanctity = item.sanctity,
                    effect = item.effect,
                    acquisitionMethod = item.acquisitionMethod,
                    sellPrice = item.sellPrice,
                )
            },
        )
        guideDao.upsertItems(
            guide.items.map { item ->
                ItemEntity(
                    id = item.id,
                    name = item.name,
                    category = item.category,
                    description = item.description,
                    acquisitionMethod = item.acquisitionMethod,
                )
            },
        )
        guideDao.upsertShards(
            guide.collectibles.tabletShards.map { shard ->
                TabletShardEntity(
                    id = shard.id,
                    number = shard.number,
                    chapter = shard.chapter,
                    region = shard.region,
                    exactLocationDescription = shard.exactLocationDescription,
                    missableWarning = shard.missableWarning,
                    notes = shard.notes,
                )
            },
        )
        guideDao.upsertOblivionTiers(
            guide.collectibles.oblivionPalaceTiers.map { tier ->
                OblivionTierEntity(
                    id = tier.id,
                    chamber = tier.chamber,
                    totalShards = tier.totalShards,
                    rewardName = tier.rewardName,
                    rewardDescription = tier.rewardDescription,
                    quantity = tier.quantity,
                )
            },
        )
    }

    companion object {
        const val KIND_CAVE_MASTER = "cave_master"
        const val KIND_ESPER = "esper"

        private const val KEY_DATASET_VERSION = "dataset_version"

        /**
         * Art is not bundled -- see `scripts/guide_pipeline/README.md`. These
         * are the paths the app looks for in the sideload directory or in the
         * assets packaged from `resonance.assetsDir`; when a file is absent,
         * `GuideImage` shows its placeholder.
         */
        private fun mapAssetFor(chapterNumber: Int) =
            "maps/chapter_%02d.webp".format(chapterNumber)

        private fun spriteAsset(folder: String, name: String): String {
            val slug = name.lowercase()
                .map { if (it.isLetterOrDigit()) it else '_' }
                .joinToString("")
                .trim('_')
                .replace(Regex("_+"), "_")
            return "sprites/$folder/$slug.webp"
        }
    }
}

sealed interface SeedResult {
    val datasetVersion: String

    data class AlreadyCurrent(override val datasetVersion: String) : SeedResult

    data class Seeded(
        override val datasetVersion: String,
        val gaps: List<String>,
    ) : SeedResult
}
