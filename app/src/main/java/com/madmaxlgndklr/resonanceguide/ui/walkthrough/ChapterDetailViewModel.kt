package com.madmaxlgndklr.resonanceguide.ui.walkthrough

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import com.madmaxlgndklr.resonanceguide.data.local.ChapterEntity
import com.madmaxlgndklr.resonanceguide.data.local.DialogueChoiceEntity
import com.madmaxlgndklr.resonanceguide.data.local.ObjectiveWithProgress
import com.madmaxlgndklr.resonanceguide.data.local.ShardWithProgress
import com.madmaxlgndklr.resonanceguide.data.local.SideQuestWithProgress
import com.madmaxlgndklr.resonanceguide.data.local.WalkthroughAreaWithProgress
import com.madmaxlgndklr.resonanceguide.data.repository.GuideRepository
import com.madmaxlgndklr.resonanceguide.navigation.ChapterDetailRoute
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * One conversation and the options it offers, grouped for display.
 *
 * The table stores one row per option because each has its own id and its own
 * bonus. What the reader wants to see is the three of them together, so the
 * rows are regrouped here rather than in SQL.
 */
data class DialogueChoiceGroup(
    val speaker: String,
    val prompt: String?,
    val options: List<DialogueChoiceEntity>,
)

data class ChapterDetailUiState(
    val chapter: ChapterEntity? = null,
    val objectives: List<ObjectiveWithProgress> = emptyList(),
    val areas: List<WalkthroughAreaWithProgress> = emptyList(),
    val dialogueChoices: List<DialogueChoiceGroup> = emptyList(),
    val sideQuests: List<SideQuestWithProgress> = emptyList(),
    val shards: List<ShardWithProgress> = emptyList(),
) {
    val completedObjectives: Int get() = objectives.count { it.completed }
    val allObjectivesDone: Boolean
        get() = objectives.isNotEmpty() && completedObjectives == objectives.size
    val visitedAreas: Int get() = areas.count { it.visited }
    val allAreasVisited: Boolean
        get() = areas.isNotEmpty() && visitedAreas == areas.size
    val collectedShards: Int get() = shards.count { it.collected }

    /**
     * What the "mark everything" action in the app bar acts on.
     *
     * A chapter's areas are the longer, more useful list, so they are the
     * default. The prologue has one area and one objective, and older
     * datasets have no areas at all, so fall back to the objectives.
     */
    val bulkTarget: BulkTarget
        get() = if (areas.isNotEmpty()) BulkTarget.AREAS else BulkTarget.OBJECTIVES

    val bulkMarkAll: Boolean
        get() = when (bulkTarget) {
            BulkTarget.AREAS -> !allAreasVisited
            BulkTarget.OBJECTIVES -> !allObjectivesDone
        }
}

enum class BulkTarget { AREAS, OBJECTIVES }

@OptIn(ExperimentalCoroutinesApi::class)
class ChapterDetailViewModel(
    savedStateHandle: SavedStateHandle,
    private val repository: GuideRepository,
) : ViewModel() {

    private val chapterId = savedStateHandle.toRoute<ChapterDetailRoute>().chapterId

    private val chapter = repository.chapter(chapterId)

    // Side quests, shards and dialogue choices are keyed by chapter number
    // rather than id, so they wait on the chapter row before they can be
    // queried.
    private val chapterScoped = chapter.flatMapLatest { entity ->
        if (entity == null) {
            flowOf(ChapterScoped())
        } else {
            combine(
                repository.sideQuests(entity.chapterNumber),
                repository.shards(entity.chapterNumber),
                repository.dialogueChoices(entity.chapterNumber),
            ) { quests, shards, dialogue ->
                ChapterScoped(quests, shards, groupDialogue(dialogue))
            }
        }
    }

    val uiState: StateFlow<ChapterDetailUiState> = combine(
        chapter,
        repository.objectives(chapterId),
        repository.walkthroughAreas(chapterId),
        chapterScoped,
    ) { entity, objectives, areas, scoped ->
        ChapterDetailUiState(
            chapter = entity,
            objectives = objectives,
            areas = areas,
            dialogueChoices = scoped.dialogue,
            sideQuests = scoped.quests,
            shards = scoped.shards,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ChapterDetailUiState())

    fun setObjectiveCompleted(objectiveId: String, completed: Boolean) {
        viewModelScope.launch { repository.setObjectiveCompleted(objectiveId, completed) }
    }

    fun setAreaVisited(areaId: String, visited: Boolean) {
        viewModelScope.launch { repository.setAreaVisited(areaId, visited) }
    }

    fun setSideQuestCompleted(questId: String, completed: Boolean) {
        viewModelScope.launch { repository.setSideQuestCompleted(questId, completed) }
    }

    fun setShardCollected(shardId: String, collected: Boolean) {
        viewModelScope.launch { repository.setShardCollected(shardId, collected) }
    }

    /** Marks every row of [target] in this chapter, for the app bar action. */
    fun setAllCompleted(target: BulkTarget, completed: Boolean) {
        viewModelScope.launch {
            when (target) {
                BulkTarget.AREAS -> repository.setChapterAreasVisited(chapterId, completed)
                BulkTarget.OBJECTIVES ->
                    repository.setChapterObjectivesCompleted(chapterId, completed)
            }
        }
    }

    private data class ChapterScoped(
        val quests: List<SideQuestWithProgress> = emptyList(),
        val shards: List<ShardWithProgress> = emptyList(),
        val dialogue: List<DialogueChoiceGroup> = emptyList(),
    )

    private companion object {
        /**
         * Groups options by the conversation they belong to.
         *
         * The DAO orders by `prompt` then `id`, so rows of one conversation
         * arrive together and in a stable order. `groupBy` preserves the
         * order it meets keys in, so the groups come out in chapter order too.
         */
        fun groupDialogue(rows: List<DialogueChoiceEntity>): List<DialogueChoiceGroup> =
            rows.groupBy { it.speaker to it.prompt }
                .map { (key, options) ->
                    DialogueChoiceGroup(key.first, key.second, options)
                }
    }
}
