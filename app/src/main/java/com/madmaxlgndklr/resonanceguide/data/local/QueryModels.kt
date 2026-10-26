package com.madmaxlgndklr.resonanceguide.data.local

import androidx.room.Embedded

data class ChapterSummary(
    @Embedded val chapter: ChapterEntity,
    val totalObjectives: Int,
    val completedObjectives: Int,
    val totalShards: Int,
    val collectedShards: Int,
)

val ChapterSummary.isComplete: Boolean
    get() = totalObjectives > 0 && completedObjectives == totalObjectives

val ChapterSummary.progressFraction: Float
    get() = if (totalObjectives == 0) 0f else completedObjectives.toFloat() / totalObjectives

data class ObjectiveWithProgress(
    @Embedded val objective: ObjectiveEntity,
    val completed: Boolean,
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
