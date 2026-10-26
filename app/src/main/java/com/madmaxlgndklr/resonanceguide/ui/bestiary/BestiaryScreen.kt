package com.madmaxlgndklr.resonanceguide.ui.bestiary

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.madmaxlgndklr.resonanceguide.R
import com.madmaxlgndklr.resonanceguide.data.local.EnemyWithProgress
import com.madmaxlgndklr.resonanceguide.data.local.OptionalBossWithProgress
import com.madmaxlgndklr.resonanceguide.ui.AppViewModelProvider
import com.madmaxlgndklr.resonanceguide.ui.components.EmptyState
import com.madmaxlgndklr.resonanceguide.ui.components.LabeledChips
import com.madmaxlgndklr.resonanceguide.ui.components.SectionHeader
import com.madmaxlgndklr.resonanceguide.ui.components.plus
import java.text.NumberFormat

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BestiaryScreen(
    viewModel: BestiaryViewModel = viewModel(factory = AppViewModelProvider.Factory),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val scrollBehavior = TopAppBarDefaults.enterAlwaysScrollBehavior()

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(stringResource(R.string.tab_bestiary))
                        Text(
                            stringResource(
                                R.string.defeated_progress,
                                state.defeatedCount,
                                state.totalCount,
                            ),
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                },
                scrollBehavior = scrollBehavior,
            )
        },
    ) { padding ->
        LazyColumn(
            contentPadding = padding.plus(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item(key = "search", contentType = "search") {
                Column {
                    OutlinedTextField(
                        value = state.query,
                        onValueChange = viewModel::onQueryChange,
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
                        placeholder = { Text(stringResource(R.string.search_bestiary)) },
                    )
                    Spacer(Modifier.size(8.dp))
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(BestiaryFilter.entries, key = { it.name }) { filter ->
                            FilterChip(
                                selected = state.filter == filter,
                                onClick = { viewModel.selectFilter(filter) },
                                label = { Text(stringResource(filter.labelRes())) },
                            )
                        }
                    }
                }
            }

            if (state.optionalBosses.isNotEmpty()) {
                item(key = "optional_header") {
                    SectionHeader(stringResource(R.string.filter_optional))
                }
                items(state.optionalBosses, key = { it.boss.id }, contentType = { "optional" }) {
                    OptionalBossCard(it) { defeated ->
                        viewModel.setOptionalBossDefeated(it.boss.id, defeated)
                    }
                }
            }

            if (state.enemies.isNotEmpty()) {
                item(key = "enemies_header") {
                    SectionHeader(stringResource(R.string.tab_bestiary))
                }
                items(state.enemies, key = { it.enemy.id }, contentType = { "enemy" }) {
                    EnemyCard(it) { defeated ->
                        viewModel.setDefeated(it.enemy.id, defeated)
                    }
                }
            }

            if (state.enemies.isEmpty() && state.optionalBosses.isEmpty()) {
                item(key = "empty") { EmptyState(Modifier.padding(top = 48.dp)) }
            }
        }
    }
}

private fun BestiaryFilter.labelRes() = when (this) {
    BestiaryFilter.All -> R.string.filter_all
    BestiaryFilter.Bosses -> R.string.filter_bosses
    BestiaryFilter.OptionalBosses -> R.string.filter_optional
}

private fun format(value: Int?) = value?.let { NumberFormat.getIntegerInstance().format(it) }

@Composable
private fun EnemyCard(entry: EnemyWithProgress, onToggleDefeated: (Boolean) -> Unit) {
    val enemy = entry.enemy
    var expanded by rememberSaveable(enemy.id) { mutableStateOf(false) }

    ElevatedCard(
        onClick = { expanded = !expanded },
        modifier = Modifier
            .fillMaxWidth()
            .animateContentSize(),
    ) {
        Column(Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(enemy.name, style = MaterialTheme.typography.titleMedium)
                    Text(
                        listOfNotNull(
                            enemy.level?.let { "Lv $it" },
                            format(enemy.hp)?.let { "HP $it" },
                            enemy.staggerGauge?.let { "BP $it" },
                        ).joinToString(" · "),
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.secondary,
                    )
                }
                FilterChip(
                    selected = entry.defeated,
                    onClick = { onToggleDefeated(!entry.defeated) },
                    label = { Text(stringResource(R.string.defeated)) },
                )
            }
            AnimatedVisibility(visible = expanded) {
                Column(
                    Modifier.padding(top = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    LabeledChips(stringResource(R.string.weaknesses), enemy.weaknesses)
                    LabeledChips(stringResource(R.string.resistances), enemy.resistances)
                    LabeledChips(stringResource(R.string.drops), enemy.drops)
                    LabeledChips(stringResource(R.string.steals), enemy.stealables)
                    enemy.traits?.let {
                        Text(stringResource(R.string.traits), style = MaterialTheme.typography.labelLarge)
                        Text(it, style = MaterialTheme.typography.bodyMedium)
                    }
                    Text(
                        listOfNotNull(
                            enemy.exp?.let { "EXP $it" },
                            enemy.gil?.let { "Gil $it" },
                        ).joinToString(" · "),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}

@Composable
private fun OptionalBossCard(
    entry: OptionalBossWithProgress,
    onToggleDefeated: (Boolean) -> Unit,
) {
    val boss = entry.boss
    var expanded by rememberSaveable(boss.id) { mutableStateOf(false) }

    ElevatedCard(
        onClick = { expanded = !expanded },
        modifier = Modifier
            .fillMaxWidth()
            .animateContentSize(),
    ) {
        Column(Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(boss.name, style = MaterialTheme.typography.titleMedium)
                    boss.location?.let {
                        Text(
                            it,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    Text(
                        listOfNotNull(
                            boss.level?.let { "Lv $it" },
                            format(boss.hp)?.let { "HP $it" },
                            boss.staggerGauge?.let { "BP $it" },
                        ).joinToString(" · "),
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.secondary,
                    )
                }
                FilterChip(
                    selected = entry.defeated,
                    onClick = { onToggleDefeated(!entry.defeated) },
                    label = { Text(stringResource(R.string.defeated)) },
                )
            }
            AnimatedVisibility(visible = expanded) {
                Column(
                    Modifier.padding(top = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    LabeledChips(stringResource(R.string.weaknesses), boss.weaknesses)
                    LabeledChips(stringResource(R.string.resistances), boss.resistances)
                    LabeledChips(stringResource(R.string.rewards), boss.rewards)
                    boss.strategy?.let {
                        Text(
                            stringResource(R.string.strategy),
                            style = MaterialTheme.typography.labelLarge,
                        )
                        Text(it, style = MaterialTheme.typography.bodyMedium)
                    }
                }
            }
        }
    }
}
