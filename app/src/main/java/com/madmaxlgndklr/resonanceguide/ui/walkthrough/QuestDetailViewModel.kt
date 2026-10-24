package com.madmaxlgndklr.resonanceguide.ui.walkthrough

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import com.madmaxlgndklr.resonanceguide.data.local.ChapterEntity
import com.madmaxlgndklr.resonanceguide.data.local.QuestEntity
import com.madmaxlgndklr.resonanceguide.data.local.QuestStepWithProgress
import com.madmaxlgndklr.resonanceguide.data.repository.GuideRepository
import com.madmaxlgndklr.resonanceguide.navigation.QuestDetailRoute
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class QuestDetailUiState(
    val quest: QuestEntity? = null,
    val chapter: ChapterEntity? = null,
    val steps: List<QuestStepWithProgress> = emptyList(),
) {
    val completedSteps: Int get() = steps.count { it.completed }
    val allComplete: Boolean get() = steps.isNotEmpty() && completedSteps == steps.size
}

@OptIn(ExperimentalCoroutinesApi::class)
class QuestDetailViewModel(
    savedStateHandle: SavedStateHandle,
    private val repository: GuideRepository,
) : ViewModel() {

    private val questId = savedStateHandle.toRoute<QuestDetailRoute>().questId

    private val quest = repository.quest(questId)
    private val chapter = quest.flatMapLatest { q ->
        if (q == null) flowOf(null) else repository.chapter(q.chapterId)
    }

    val uiState: StateFlow<QuestDetailUiState> =
        combine(quest, chapter, repository.steps(questId)) { q, c, steps ->
            QuestDetailUiState(q, c, steps)
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), QuestDetailUiState())

    fun setStepCompleted(stepId: Int, completed: Boolean) {
        viewModelScope.launch { repository.setStepCompleted(stepId, completed) }
    }

    fun setAllCompleted(completed: Boolean) {
        viewModelScope.launch { repository.setQuestCompleted(questId, completed) }
    }
}
