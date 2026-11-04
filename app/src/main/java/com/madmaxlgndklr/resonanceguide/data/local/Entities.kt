package com.madmaxlgndklr.resonanceguide.data.local

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/*
 * Content tables, mirroring assets/ff_resonance_guide.json.
 *
 * Primary keys are the dataset's own snake_case ids (`shard_001`,
 * `vis_cloud`, `ch_01_chapter_1_tailing_the_truth`). They are stable by
 * construction -- derived from a record's name and the game's internal
 * catalog number, never from list position -- so [ProgressEntity] rows keyed
 * by them survive a dataset rebuild.
 *
 * These tables are rewritten wholesale whenever the dataset version changes.
 * User state lives only in [ProgressEntity].
 */

@Entity(tableName = "chapters")
data class ChapterEntity(
    @PrimaryKey val id: String,
    val chapterNumber: Int,
    val title: String,
    val region: String?,
    val recommendedLevel: String?,
    val storyBosses: List<String>,
    val optionalFights: List<String>,
    /** Path relative to the guide asset root, e.g. `maps/chapter_01.webp`. */
    val mapAsset: String?,
)

@Entity(
    tableName = "objectives",
    foreignKeys = [
        ForeignKey(
            entity = ChapterEntity::class,
            parentColumns = ["id"],
            childColumns = ["chapterId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index("chapterId")],
)
data class ObjectiveEntity(
    @PrimaryKey val id: String,
    val chapterId: String,
    val step: Int,
    val text: String,
    val dungeonPuzzleSolution: String?,
)

/**
 * One stop on a chapter's route: a town, a dungeon, a world-map leg.
 *
 * [ObjectiveEntity] holds the game's own quest-log strings. This holds the
 * step-by-step notes that sit between them -- what to do here, what the place
 * holds, what is waiting at the end -- and is what the walkthrough tab shows
 * under each chapter.
 *
 * [shardCount] is how many Tablet Shards the area holds and nothing more. The
 * 99 shards are numbered differently by different guides, and
 * [TabletShardEntity.number] follows one of them, so an area never quotes a
 * number; `validate_json.py` fails the build if one creeps in.
 */
@Entity(
    tableName = "walkthrough_areas",
    foreignKeys = [
        ForeignKey(
            entity = ChapterEntity::class,
            parentColumns = ["id"],
            childColumns = ["chapterId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index("chapterId")],
)
data class WalkthroughAreaEntity(
    @PrimaryKey val id: String,
    val chapterId: String,
    val sortOrder: Int,
    val name: String,
    /** `town`, `dungeon`, `world_map`, `event`, `optional`, `colosseum` or `interlude`. */
    val kind: String?,
    val summary: String?,
    val steps: List<String>,
    val chests: List<String>,
    val bosses: List<String>,
    val shardCount: Int,
    val sideQuests: List<String>,
    val notableSteals: List<String>,
)

/**
 * A conversation that permanently raises one character's stat, and the option
 * that raises it.
 *
 * One row per option, three rows per conversation, so the app can show all
 * three side by side -- the point of the table is choosing between them, not
 * recording which one the game offered.
 */
@Entity(tableName = "dialogue_choices", indices = [Index("chapterNumber")])
data class DialogueChoiceEntity(
    @PrimaryKey val id: String,
    val chapterNumber: Int?,
    /** The character whose stat the bonus applies to. */
    val speaker: String,
    /** Where in the chapter the conversation happens. */
    val prompt: String?,
    /** The option as the game words it. */
    val choice: String?,
    /** What picking it grants, e.g. `Strength +3`. */
    val bonus: String?,
)

@Entity(tableName = "side_quests", indices = [Index("chapterNumber")])
data class SideQuestEntity(
    @PrimaryKey val id: String,
    val chapterNumber: Int?,
    val title: String,
    val requester: String?,
    val location: String?,
    val region: String?,
    val prerequisites: String?,
    val rewards: List<String>,
    val objectives: List<String>,
)

@Entity(tableName = "enemies", indices = [Index("enemyType"), Index("name")])
data class EnemyEntity(
    @PrimaryKey val id: String,
    val name: String,
    val enemyType: String?,
    val level: Int?,
    val hp: Int?,
    val staggerGauge: Int?,
    val weaknesses: List<String>,
    val resistances: List<String>,
    val drops: List<String>,
    val stealables: List<String>,
    val traits: String?,
    val exp: Int?,
    val gil: Int?,
    val spriteAsset: String?,
)

/** Cave Masters and Espers: the optional bosses with hand-written strategies. */
@Entity(tableName = "optional_bosses", indices = [Index("kind")])
data class OptionalBossEntity(
    @PrimaryKey val id: String,
    val name: String,
    /** `cave_master` or `esper`. */
    val kind: String,
    val level: Int?,
    val hp: Int?,
    val staggerGauge: Int?,
    val location: String?,
    val weaknesses: List<String>,
    val resistances: List<String>,
    val rewards: List<String>,
    val strategy: String?,
    val spriteAsset: String?,
)

@Entity(tableName = "visions", indices = [Index("role")])
data class VisionEntity(
    @PrimaryKey val id: String,
    val name: String,
    val origin: String?,
    val role: String?,
    val damageType: String?,
    val resonanceSkill: String?,
    val elements: List<String>,
    val chapter: Int?,
    val method: String?,
    val unlockRoute: String?,
    val spriteAsset: String?,
)

@Entity(
    tableName = "vision_skills",
    foreignKeys = [
        ForeignKey(
            entity = VisionEntity::class,
            parentColumns = ["id"],
            childColumns = ["visionId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    // visionId is not unique on its own and skillId repeats across visions,
    // so the row key is the pair.
    primaryKeys = ["visionId", "skillId"],
    indices = [Index("visionId")],
)
data class VisionSkillEntity(
    val visionId: String,
    val skillId: String,
    val name: String,
    val category: String?,
    val description: String?,
    val masteryRank: Int?,
    val apCost: Int?,
    val awakeningTier: Int?,
)

@Entity(tableName = "equipment", indices = [Index("slot"), Index("name")])
data class EquipmentEntity(
    @PrimaryKey val id: String,
    val name: String,
    val slot: String,
    val equipType: String?,
    val physAtk: Int?,
    val magAtk: Int?,
    val defense: Int?,
    val magDef: Int?,
    val speed: Int?,
    val sanctity: Int?,
    val effect: String?,
    val acquisitionMethod: String?,
    val sellPrice: Int?,
)

@Entity(tableName = "items", indices = [Index("category")])
data class ItemEntity(
    @PrimaryKey val id: String,
    val name: String,
    val category: String?,
    val description: String?,
    val acquisitionMethod: String?,
)

@Entity(tableName = "tablet_shards", indices = [Index("chapter")])
data class TabletShardEntity(
    @PrimaryKey val id: String,
    val number: Int,
    val chapter: Int?,
    val region: String?,
    val exactLocationDescription: String?,
    val missableWarning: String?,
    val notes: String?,
)

@Entity(tableName = "oblivion_tiers", indices = [Index("totalShards")])
data class OblivionTierEntity(
    @PrimaryKey val id: String,
    val chamber: String,
    val totalShards: Int,
    val rewardName: String,
    val rewardDescription: String?,
    val quantity: Int?,
)

/** Values for [ProgressEntity.type]. Keep in sync with the literals in the DAOs. */
object ProgressType {
    const val OBJECTIVE = "OBJECTIVE"
    const val AREA = "AREA"
    const val SIDE_QUEST = "SIDE_QUEST"
    const val ENEMY = "ENEMY"
    const val OPTIONAL_BOSS = "OPTIONAL_BOSS"
    const val VISION = "VISION"
    const val EQUIPMENT = "EQUIPMENT"
    const val ITEM = "ITEM"
    const val SHARD = "SHARD"
}

/**
 * A user's check-off state for one guide record.
 *
 * [entityId] is the dataset id, so this table is the one thing a dataset
 * rebuild must never touch.
 */
@Entity(tableName = "progress", primaryKeys = ["type", "entityId"])
data class ProgressEntity(
    val type: String,
    val entityId: String,
    val completed: Boolean,
    val updatedAt: Long = System.currentTimeMillis(),
)
