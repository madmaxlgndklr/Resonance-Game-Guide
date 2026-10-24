package com.madmaxlgndklr.resonanceguide.data.local

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/*
 * Guide content tables. These are (re)written from SeedData whenever the
 * bundled guide version changes, so they never hold user state; that lives in
 * [ProgressEntity] and survives content updates.
 */

@Entity(tableName = "chapters")
data class ChapterEntity(
    @PrimaryKey val id: Int,
    val number: Int,
    val title: String,
    val summary: String,
    /** Path relative to the guide asset root, e.g. `maps/chapter_01.webp`. */
    val mapAsset: String?,
)

@Entity(
    tableName = "quests",
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
data class QuestEntity(
    @PrimaryKey val id: Int,
    val chapterId: Int,
    val title: String,
    val description: String,
    val isMainStory: Boolean,
    val sortOrder: Int,
)

@Entity(
    tableName = "quest_steps",
    foreignKeys = [
        ForeignKey(
            entity = QuestEntity::class,
            parentColumns = ["id"],
            childColumns = ["questId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index("questId")],
)
data class QuestStepEntity(
    @PrimaryKey val id: Int,
    val questId: Int,
    val stepNumber: Int,
    val instruction: String,
    val hint: String? = null,
)

@Entity(tableName = "bosses", indices = [Index("chapterId")])
data class BossEntity(
    @PrimaryKey val id: Int,
    val name: String,
    val chapterId: Int?,
    val location: String,
    val level: Int,
    val hp: Long,
    val weaknesses: List<String>,
    val resistances: List<String>,
    val drops: List<String>,
    val strategy: String,
    val spriteAsset: String?,
)

@Entity(tableName = "items", indices = [Index("category")])
data class ItemEntity(
    @PrimaryKey val id: Int,
    val name: String,
    val category: String,
    val description: String,
    val location: String,
    val chapterId: Int?,
    val iconAsset: String?,
)

@Entity(tableName = "character_builds")
data class CharacterBuildEntity(
    @PrimaryKey val id: Int,
    val characterName: String,
    val buildName: String,
    val role: String,
    val summary: String,
    val skills: List<String>,
    val gear: List<String>,
    val spriteAsset: String?,
)

/** Values for [ProgressEntity.type]. Keep in sync with the literals in [GuideDao]. */
object ProgressType {
    const val QUEST_STEP = "QUEST_STEP"
    const val ITEM = "ITEM"
    const val BOSS = "BOSS"
}

/** A user's check-off state for one guide entry (quest step, item or boss). */
@Entity(tableName = "progress", primaryKeys = ["type", "entityId"])
data class ProgressEntity(
    val type: String,
    val entityId: Int,
    val completed: Boolean,
    val updatedAt: Long = System.currentTimeMillis(),
)
