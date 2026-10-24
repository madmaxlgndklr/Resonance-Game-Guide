package com.madmaxlgndklr.resonanceguide.ui.walkthrough

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.madmaxlgndklr.resonanceguide.data.local.ChapterEntity
import com.madmaxlgndklr.resonanceguide.data.local.QuestSummary
import com.madmaxlgndklr.resonanceguide.data.local.isComplete
import com.madmaxlgndklr.resonanceguide.data.repository.GuideRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn

data class ChapterSection(
    val chapter: ChapterEntity,
    val quests: List<QuestSummary>,
)

data class WalkthroughUiState(
    val sections: List<ChapterSection> = emptyList(),
    val completedQuests: Int = 0,
    val totalQuests: Int = 0,
    val isLoading: Boolean = true,
)

class WalkthroughViewModel(repository: GuideRepository) : ViewModel() {

    val uiState: StateFlow<WalkthroughUiState> =
        combine(repository.chapters(), repository.questSummaries()) { chapters, quests ->
            val byChapter = quests.groupBy { it.quest.chapterId }
            WalkthroughUiState(
                sections = chapters.map { ChapterSection(it, byChapter[it.id].orEmpty()) },
                completedQuests = quests.count { it.isComplete },
                totalQuests = quests.size,
                isLoading = false,
            )
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), WalkthroughUiState())
}
