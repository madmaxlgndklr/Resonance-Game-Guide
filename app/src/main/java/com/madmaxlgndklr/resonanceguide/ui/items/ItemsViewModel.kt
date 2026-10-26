package com.madmaxlgndklr.resonanceguide.ui.items

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.madmaxlgndklr.resonanceguide.data.local.EquipmentWithProgress
import com.madmaxlgndklr.resonanceguide.data.local.ItemWithProgress
import com.madmaxlgndklr.resonanceguide.data.repository.GuideRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** The Items tab shows consumables and equipment side by side, filtered by category. */
data class ItemsUiState(
    val categories: List<String> = emptyList(),
    /** `null` means all categories. */
    val selectedCategory: String? = null,
    val hideCollected: Boolean = false,
    val items: List<ItemWithProgress> = emptyList(),
    val equipment: List<EquipmentWithProgress> = emptyList(),
    val collectedCount: Int = 0,
    val totalCount: Int = 0,
)

private data class Filters(val category: String? = null, val hideCollected: Boolean = false)

class ItemsViewModel(private val repository: GuideRepository) : ViewModel() {

    private val filters = MutableStateFlow(Filters())

    val uiState: StateFlow<ItemsUiState> = combine(
        repository.items(),
        repository.equipment(),
        filters,
    ) { items, equipment, f ->
        val equipmentCategories = equipment.map { it.equipment.slot }.distinct().sorted()
        val itemCategories = items.mapNotNull { it.item.category }.distinct().sorted()
        val visibleItems = items.filter { entry ->
            (f.category == null || entry.item.category == f.category) &&
                !(f.hideCollected && entry.collected)
        }
        val visibleEquipment = equipment.filter { entry ->
            (f.category == null || entry.equipment.slot == f.category) &&
                !(f.hideCollected && entry.collected)
        }
        ItemsUiState(
            categories = itemCategories + equipmentCategories,
            selectedCategory = f.category,
            hideCollected = f.hideCollected,
            items = visibleItems,
            equipment = visibleEquipment,
            collectedCount = items.count { it.collected } + equipment.count { it.collected },
            totalCount = items.size + equipment.size,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ItemsUiState())

    fun selectCategory(category: String?) = filters.update { it.copy(category = category) }

    fun toggleHideCollected() = filters.update { it.copy(hideCollected = !it.hideCollected) }

    fun setItemCollected(itemId: String, collected: Boolean) {
        viewModelScope.launch { repository.setItemCollected(itemId, collected) }
    }

    fun setEquipmentCollected(equipmentId: String, collected: Boolean) {
        viewModelScope.launch { repository.setEquipmentCollected(equipmentId, collected) }
    }
}
