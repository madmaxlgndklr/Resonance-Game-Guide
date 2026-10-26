package com.madmaxlgndklr.resonanceguide.ui.walkthrough

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.madmaxlgndklr.resonanceguide.data.local.ChapterSummary
import com.madmaxlgndklr.resonanceguide.data.local.isComplete
import com.madmaxlgndklr.resonanceguide.data.repository.GuideRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

data class WalkthroughUiState(
    val chapters: List<ChapterSummary> = emptyList(),
    val completedChapters: Int = 0,
    val collectedShards: Int = 0,
    val totalShards: Int = 0,
    val isLoading: Boolean = true,
)

class WalkthroughViewModel(repository: GuideRepository) : ViewModel() {

    val uiState: StateFlow<WalkthroughUiState> =
        repository.chapterSummaries()
            .map { chapters ->
                WalkthroughUiState(
                    chapters = chapters,
                    completedChapters = chapters.count { it.isComplete },
                    collectedShards = chapters.sumOf { it.collectedShards },
                    totalShards = chapters.sumOf { it.totalShards },
                    isLoading = false,
                )
            }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), WalkthroughUiState())
}
