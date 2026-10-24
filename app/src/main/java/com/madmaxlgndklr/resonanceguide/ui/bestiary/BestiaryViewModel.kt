package com.madmaxlgndklr.resonanceguide.ui.bestiary

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.madmaxlgndklr.resonanceguide.data.local.BossWithProgress
import com.madmaxlgndklr.resonanceguide.data.repository.GuideRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class BestiaryUiState(
    val query: String = "",
    val bosses: List<BossWithProgress> = emptyList(),
    val defeatedCount: Int = 0,
    val totalCount: Int = 0,
)

class BestiaryViewModel(private val repository: GuideRepository) : ViewModel() {

    private val query = MutableStateFlow("")

    val uiState: StateFlow<BestiaryUiState> =
        combine(repository.bosses(), query.asStateFlow()) { bosses, q ->
            val term = q.trim()
            val filtered = if (term.isEmpty()) bosses else bosses.filter { entry ->
                val b = entry.boss
                b.name.contains(term, ignoreCase = true) ||
                    b.location.contains(term, ignoreCase = true) ||
                    b.weaknesses.any { it.contains(term, ignoreCase = true) }
            }
            BestiaryUiState(
                query = q,
                bosses = filtered,
                defeatedCount = bosses.count { it.defeated },
                totalCount = bosses.size,
            )
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), BestiaryUiState())

    fun onQueryChange(value: String) {
        query.value = value
    }

    fun setDefeated(bossId: Int, defeated: Boolean) {
        viewModelScope.launch { repository.setBossDefeated(bossId, defeated) }
    }
}
