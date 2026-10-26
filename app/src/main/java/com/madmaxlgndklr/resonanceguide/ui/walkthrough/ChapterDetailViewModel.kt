package com.madmaxlgndklr.resonanceguide.ui.walkthrough

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import com.madmaxlgndklr.resonanceguide.data.local.ChapterEntity
import com.madmaxlgndklr.resonanceguide.data.local.ObjectiveWithProgress
import com.madmaxlgndklr.resonanceguide.data.local.ShardWithProgress
import com.madmaxlgndklr.resonanceguide.data.local.SideQuestWithProgress
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

data class ChapterDetailUiState(
    val chapter: ChapterEntity? = null,
    val objectives: List<ObjectiveWithProgress> = emptyList(),
    val sideQuests: List<SideQuestWithProgress> = emptyList(),
    val shards: List<ShardWithProgress> = emptyList(),
) {
    val completedObjectives: Int get() = objectives.count { it.completed }
    val allObjectivesDone: Boolean
        get() = objectives.isNotEmpty() && completedObjectives == objectives.size
    val collectedShards: Int get() = shards.count { it.collected }
}

@OptIn(ExperimentalCoroutinesApi::class)
class ChapterDetailViewModel(
    savedStateHandle: SavedStateHandle,
    private val repository: GuideRepository,
) : ViewModel() {

    private val chapterId = savedStateHandle.toRoute<ChapterDetailRoute>().chapterId

    private val chapter = repository.chapter(chapterId)

    // Side quests and shards are keyed by chapter number rather than id, so
    // they wait on the chapter row before they can be queried.
    private val chapterScoped = chapter.flatMapLatest { entity ->
        if (entity == null) {
            flowOf(emptyList<SideQuestWithProgress>() to emptyList<ShardWithProgress>())
        } else {
            combine(
                repository.sideQuests(entity.chapterNumber),
                repository.shards(entity.chapterNumber),
            ) { quests, shards -> quests to shards }
        }
    }

    val uiState: StateFlow<ChapterDetailUiState> = combine(
        chapter,
        repository.objectives(chapterId),
        chapterScoped,
    ) { entity, objectives, (quests, shards) ->
        ChapterDetailUiState(entity, objectives, quests, shards)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ChapterDetailUiState())

    fun setObjectiveCompleted(objectiveId: String, completed: Boolean) {
        viewModelScope.launch { repository.setObjectiveCompleted(objectiveId, completed) }
    }

    fun setAllObjectivesCompleted(completed: Boolean) {
        viewModelScope.launch {
            repository.setChapterObjectivesCompleted(chapterId, completed)
        }
    }

    fun setSideQuestCompleted(questId: String, completed: Boolean) {
        viewModelScope.launch { repository.setSideQuestCompleted(questId, completed) }
    }

    fun setShardCollected(shardId: String, collected: Boolean) {
        viewModelScope.launch { repository.setShardCollected(shardId, collected) }
    }
}
