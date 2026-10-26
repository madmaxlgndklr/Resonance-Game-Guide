package com.madmaxlgndklr.resonanceguide.ui.items

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.selection.toggleable
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.madmaxlgndklr.resonanceguide.R
import com.madmaxlgndklr.resonanceguide.data.local.EquipmentWithProgress
import com.madmaxlgndklr.resonanceguide.data.local.ItemWithProgress
import com.madmaxlgndklr.resonanceguide.ui.AppViewModelProvider
import com.madmaxlgndklr.resonanceguide.ui.components.EmptyState
import com.madmaxlgndklr.resonanceguide.ui.components.SectionHeader
import com.madmaxlgndklr.resonanceguide.ui.components.plus

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ItemsScreen(
    viewModel: ItemsViewModel = viewModel(factory = AppViewModelProvider.Factory),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val scrollBehavior = TopAppBarDefaults.enterAlwaysScrollBehavior()

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(stringResource(R.string.tab_items))
                        Text(
                            stringResource(
                                R.string.collected_progress,
                                state.collectedCount,
                                state.totalCount,
                            ),
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                },
                actions = {
                    IconButton(onClick = viewModel::toggleHideCollected) {
                        Icon(
                            if (state.hideCollected) {
                                Icons.Filled.VisibilityOff
                            } else {
                                Icons.Filled.Visibility
                            },
                            contentDescription = stringResource(
                                if (state.hideCollected) {
                                    R.string.show_collected
                                } else {
                                    R.string.hide_collected
                                },
                            ),
                        )
                    }
                },
                scrollBehavior = scrollBehavior,
            )
        },
    ) { padding ->
        LazyColumn(
            contentPadding = padding.plus(16.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            item(key = "filters", contentType = "filters") {
                CategoryFilters(
                    categories = state.categories,
                    selected = state.selectedCategory,
                    onSelect = viewModel::selectCategory,
                )
            }

            if (state.items.isNotEmpty()) {
                item(key = "items_header") {
                    SectionHeader(stringResource(R.string.items_header))
                }
                items(state.items, key = { it.item.id }, contentType = { "item" }) { entry ->
                    ItemRow(entry) { viewModel.setItemCollected(entry.item.id, it) }
                }
            }

            if (state.equipment.isNotEmpty()) {
                item(key = "equipment_header") {
                    SectionHeader(stringResource(R.string.equipment_header))
                }
                items(state.equipment, key = { it.equipment.id }, contentType = { "equipment" }) { entry ->
                    EquipmentRow(entry) { viewModel.setEquipmentCollected(entry.equipment.id, it) }
                }
            }

            if (state.items.isEmpty() && state.equipment.isEmpty()) {
                item(key = "empty") { EmptyState(Modifier.padding(top = 48.dp)) }
            }
        }
    }
}

@Composable
private fun CategoryFilters(
    categories: List<String>,
    selected: String?,
    onSelect: (String?) -> Unit,
) {
    LazyRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        contentPadding = PaddingValues(bottom = 8.dp),
    ) {
        item(key = "all") {
            FilterChip(
                selected = selected == null,
                onClick = { onSelect(null) },
                label = { Text(stringResource(R.string.all_categories)) },
            )
        }
        items(categories, key = { it }) { category ->
            FilterChip(
                selected = selected == category,
                onClick = { onSelect(category) },
                label = { Text(category.replaceFirstChar { it.uppercase() }) },
            )
        }
    }
}

@Composable
private fun ItemRow(entry: ItemWithProgress, onToggle: (Boolean) -> Unit) {
    val item = entry.item
    ListItem(
        modifier = Modifier
            .fillMaxWidth()
            .toggleable(value = entry.collected, role = Role.Checkbox, onValueChange = onToggle),
        colors = ListItemDefaults.colors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
        ),
        leadingContent = { Checkbox(checked = entry.collected, onCheckedChange = null) },
        overlineContent = item.category?.let { category -> { Text(category) } },
        headlineContent = { Text(item.name) },
        supportingContent = {
            Column {
                item.description?.let { Text(it) }
                item.acquisitionMethod?.let {
                    Text(it, color = MaterialTheme.colorScheme.secondary)
                }
            }
        },
    )
}

@Composable
private fun EquipmentRow(entry: EquipmentWithProgress, onToggle: (Boolean) -> Unit) {
    val equipment = entry.equipment
    val stats = listOfNotNull(
        equipment.physAtk?.let { "ATK $it" },
        equipment.magAtk?.let { "MATK $it" },
        equipment.defense?.let { "DEF $it" },
        equipment.magDef?.let { "MDEF $it" },
        equipment.sanctity?.let { "SNC $it" },
        equipment.speed?.let { "SPD $it" },
    ).joinToString(" · ")

    ListItem(
        modifier = Modifier
            .fillMaxWidth()
            .toggleable(value = entry.collected, role = Role.Checkbox, onValueChange = onToggle),
        colors = ListItemDefaults.colors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
        ),
        leadingContent = { Checkbox(checked = entry.collected, onCheckedChange = null) },
        overlineContent = { Text(equipment.equipType ?: equipment.slot) },
        headlineContent = { Text(equipment.name) },
        supportingContent = {
            Column {
                if (stats.isNotEmpty()) {
                    Text(stats, color = MaterialTheme.colorScheme.secondary)
                }
                equipment.effect?.let { Text(it) }
                equipment.acquisitionMethod?.let {
                    Text(
                        stringResource(R.string.acquisition) + ": " + it,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        },
    )
}
