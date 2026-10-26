package com.madmaxlgndklr.resonanceguide.ui.builds

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.madmaxlgndklr.resonanceguide.data.local.VisionSkillEntity
import com.madmaxlgndklr.resonanceguide.data.local.VisionWithProgress
import com.madmaxlgndklr.resonanceguide.data.repository.GuideRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class VisionCard(
    val vision: VisionWithProgress,
    val skills: List<VisionSkillEntity>,
)

data class BuildsUiState(
    val visions: List<VisionCard> = emptyList(),
    val unlockedCount: Int = 0,
)

class BuildsViewModel(private val repository: GuideRepository) : ViewModel() {

    val uiState: StateFlow<BuildsUiState> = combine(
        repository.visions(),
        repository.visionSkills(),
    ) { visions, skills ->
        val byVision = skills.groupBy { it.visionId }
        BuildsUiState(
            visions = visions.map { VisionCard(it, byVision[it.vision.id].orEmpty()) },
            unlockedCount = visions.count { it.unlocked },
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), BuildsUiState())

    fun setUnlocked(visionId: String, unlocked: Boolean) {
        viewModelScope.launch { repository.setVisionUnlocked(visionId, unlocked) }
    }
}
