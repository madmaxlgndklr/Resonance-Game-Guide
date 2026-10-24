package com.madmaxlgndklr.resonanceguide.data.local

import androidx.room.Embedded

data class QuestSummary(
    @Embedded val quest: QuestEntity,
    val totalSteps: Int,
    val completedSteps: Int,
)

val QuestSummary.isComplete: Boolean
    get() = totalSteps > 0 && completedSteps == totalSteps

val QuestSummary.progressFraction: Float
    get() = if (totalSteps == 0) 0f else completedSteps.toFloat() / totalSteps

data class QuestStepWithProgress(
    @Embedded val step: QuestStepEntity,
    val completed: Boolean,
)

data class BossWithProgress(
    @Embedded val boss: BossEntity,
    val defeated: Boolean,
)

data class ItemWithProgress(
    @Embedded val item: ItemEntity,
    val collected: Boolean,
)
