package com.madmaxlgndklr.resonanceguide.data.local

import androidx.room.Embedded

data class ChapterSummary(
    @Embedded val chapter: ChapterEntity,
    val totalObjectives: Int,
    val completedObjectives: Int,
    val totalAreas: Int,
    val visitedAreas: Int,
    val totalShards: Int,
    val collectedShards: Int,
)

/**
 * The list the chapter card's progress is measured against.
 *
 * Areas are the chapter's real route -- twenty-odd stops against a handful of
 * quest-log lines -- so they are what progress means once they exist. A
 * dataset built before they did has none, and falls back to the objectives.
 */
val ChapterSummary.tracked: Int
    get() = if (totalAreas > 0) totalAreas else totalObjectives

val ChapterSummary.trackedDone: Int
    get() = if (totalAreas > 0) visitedAreas else completedObjectives

val ChapterSummary.isComplete: Boolean
    get() = tracked > 0 && trackedDone == tracked

val ChapterSummary.progressFraction: Float
    get() = if (tracked == 0) 0f else trackedDone.toFloat() / tracked

data class ObjectiveWithProgress(
    @Embedded val objective: ObjectiveEntity,
    val completed: Boolean,
)

/**
 * An area and whether the user has ticked it off.
 *
 * "Visited" rather than "completed": an area is a place on the route, and the
 * tick means you are done with it, including its chests.
 */
data class WalkthroughAreaWithProgress(
    @Embedded val area: WalkthroughAreaEntity,
    val visited: Boolean,
)

data class SideQuestWithProgress(
    @Embedded val quest: SideQuestEntity,
    val completed: Boolean,
)

data class EnemyWithProgress(
    @Embedded val enemy: EnemyEntity,
    val defeated: Boolean,
)

data class OptionalBossWithProgress(
    @Embedded val boss: OptionalBossEntity,
    val defeated: Boolean,
)

data class EquipmentWithProgress(
    @Embedded val equipment: EquipmentEntity,
    val collected: Boolean,
)

data class ItemWithProgress(
    @Embedded val item: ItemEntity,
    val collected: Boolean,
)

data class ShardWithProgress(
    @Embedded val shard: TabletShardEntity,
    val collected: Boolean,
)

data class VisionWithProgress(
    @Embedded val vision: VisionEntity,
    val unlocked: Boolean,
)
