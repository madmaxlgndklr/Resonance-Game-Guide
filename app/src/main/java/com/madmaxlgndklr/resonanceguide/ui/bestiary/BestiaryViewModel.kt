package com.madmaxlgndklr.resonanceguide.ui.bestiary

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.madmaxlgndklr.resonanceguide.data.local.EnemyWithProgress
import com.madmaxlgndklr.resonanceguide.data.local.OptionalBossWithProgress
import com.madmaxlgndklr.resonanceguide.data.repository.GuideRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

enum class BestiaryFilter { All, Bosses, OptionalBosses }

data class BestiaryUiState(
    val query: String = "",
    val filter: BestiaryFilter = BestiaryFilter.All,
    val enemies: List<EnemyWithProgress> = emptyList(),
    val optionalBosses: List<OptionalBossWithProgress> = emptyList(),
    val defeatedCount: Int = 0,
    val totalCount: Int = 0,
)

private data class Filters(val query: String = "", val filter: BestiaryFilter = BestiaryFilter.All)

class BestiaryViewModel(private val repository: GuideRepository) : ViewModel() {

    private val filters = MutableStateFlow(Filters())

    val uiState: StateFlow<BestiaryUiState> = combine(
        repository.enemies(),
        repository.optionalBosses(),
        filters,
    ) { enemies, bosses, f ->
        val term = f.query.trim()
        val showEnemies = f.filter != BestiaryFilter.OptionalBosses
        val showBosses = f.filter != BestiaryFilter.Bosses

        BestiaryUiState(
            query = f.query,
            filter = f.filter,
            enemies = if (!showEnemies) emptyList() else enemies.filter { entry ->
                (f.filter != BestiaryFilter.Bosses ||
                    entry.enemy.enemyType.equals("boss", ignoreCase = true)) &&
                    entry.matches(term)
            },
            optionalBosses = if (!showBosses) emptyList() else bosses.filter {
                it.matches(term)
            },
            defeatedCount = enemies.count { it.defeated } + bosses.count { it.defeated },
            totalCount = enemies.size + bosses.size,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), BestiaryUiState())

    fun onQueryChange(value: String) = filters.update { it.copy(query = value) }

    fun selectFilter(filter: BestiaryFilter) = filters.update { it.copy(filter = filter) }

    fun setDefeated(enemyId: String, defeated: Boolean) {
        viewModelScope.launch { repository.setEnemyDefeated(enemyId, defeated) }
    }

    fun setOptionalBossDefeated(bossId: String, defeated: Boolean) {
        viewModelScope.launch { repository.setOptionalBossDefeated(bossId, defeated) }
    }
}

private fun EnemyWithProgress.matches(term: String): Boolean =
    term.isEmpty() ||
        enemy.name.contains(term, ignoreCase = true) ||
        enemy.weaknesses.any { it.contains(term, ignoreCase = true) } ||
        enemy.drops.any { it.contains(term, ignoreCase = true) } ||
        enemy.stealables.any { it.contains(term, ignoreCase = true) }

private fun OptionalBossWithProgress.matches(term: String): Boolean =
    term.isEmpty() ||
        boss.name.contains(term, ignoreCase = true) ||
        boss.location?.contains(term, ignoreCase = true) == true ||
        boss.weaknesses.any { it.contains(term, ignoreCase = true) }
