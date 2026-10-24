package com.madmaxlgndklr.resonanceguide.ui.items

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.madmaxlgndklr.resonanceguide.data.local.ItemWithProgress
import com.madmaxlgndklr.resonanceguide.data.repository.GuideRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class ItemsUiState(
    val categories: List<String> = emptyList(),
    /** `null` means all categories. */
    val selectedCategory: String? = null,
    val hideCollected: Boolean = false,
    val items: List<ItemWithProgress> = emptyList(),
    val collectedCount: Int = 0,
    val totalCount: Int = 0,
)

private data class ItemFilters(val category: String? = null, val hideCollected: Boolean = false)

class ItemsViewModel(private val repository: GuideRepository) : ViewModel() {

    private val filters = MutableStateFlow(ItemFilters())

    val uiState: StateFlow<ItemsUiState> =
        combine(repository.items(), filters) { items, f ->
            ItemsUiState(
                categories = items.map { it.item.category }.distinct(),
                selectedCategory = f.category,
                hideCollected = f.hideCollected,
                items = items.filter { entry ->
                    (f.category == null || entry.item.category == f.category) &&
                        !(f.hideCollected && entry.collected)
                },
                collectedCount = items.count { it.collected },
                totalCount = items.size,
            )
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ItemsUiState())

    fun selectCategory(category: String?) = filters.update { it.copy(category = category) }

    fun toggleHideCollected() = filters.update { it.copy(hideCollected = !it.hideCollected) }

    fun setCollected(itemId: Int, collected: Boolean) {
        viewModelScope.launch { repository.setItemCollected(itemId, collected) }
    }
}
