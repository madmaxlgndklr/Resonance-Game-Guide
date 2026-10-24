package com.madmaxlgndklr.resonanceguide.ui.bestiary

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.madmaxlgndklr.resonanceguide.R
import com.madmaxlgndklr.resonanceguide.data.local.BossWithProgress
import com.madmaxlgndklr.resonanceguide.ui.AppViewModelProvider
import com.madmaxlgndklr.resonanceguide.ui.components.EmptyState
import com.madmaxlgndklr.resonanceguide.ui.components.GuideImage
import com.madmaxlgndklr.resonanceguide.ui.components.LabeledChips
import com.madmaxlgndklr.resonanceguide.ui.components.plus
import java.text.NumberFormat

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BestiaryScreen(
    onOpenSprite: (assetPath: String, title: String) -> Unit,
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
                            stringResource(R.string.bosses_progress, state.defeatedCount, state.totalCount),
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
                OutlinedTextField(
                    value = state.query,
                    onValueChange = viewModel::onQueryChange,
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
                    placeholder = { Text(stringResource(R.string.search_bestiary)) },
                )
            }
            if (state.bosses.isEmpty()) {
                item(key = "empty") { EmptyState(Modifier.padding(top = 48.dp)) }
            }
            items(state.bosses, key = { it.boss.id }, contentType = { "boss" }) { entry ->
                BossCard(
                    entry = entry,
                    onToggleDefeated = { viewModel.setDefeated(entry.boss.id, it) },
                    onOpenSprite = onOpenSprite,
                )
            }
        }
    }
}

@Composable
private fun BossCard(
    entry: BossWithProgress,
    onToggleDefeated: (Boolean) -> Unit,
    onOpenSprite: (assetPath: String, title: String) -> Unit,
) {
    val boss = entry.boss
    var expanded by rememberSaveable(boss.id) { mutableStateOf(false) }
    val hp = NumberFormat.getIntegerInstance().format(boss.hp)

    ElevatedCard(
        onClick = { expanded = !expanded },
        modifier = Modifier
            .fillMaxWidth()
            .animateContentSize(),
    ) {
        Column(Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                GuideImage(
                    assetPath = boss.spriteAsset,
                    contentDescription = boss.name,
                    modifier = Modifier
                        .size(72.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .clickable(enabled = boss.spriteAsset != null) {
                            boss.spriteAsset?.let { onOpenSprite(it, boss.name) }
                        },
                )
                Spacer(Modifier.size(16.dp))
                Column(Modifier.weight(1f)) {
                    Text(boss.name, style = MaterialTheme.typography.titleMedium)
                    Text(boss.location, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text("Lv ${boss.level} · HP $hp", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.secondary)
                }
                FilterChip(
                    selected = entry.defeated,
                    onClick = { onToggleDefeated(!entry.defeated) },
                    label = { Text(stringResource(R.string.defeated)) },
                )
            }
            AnimatedVisibility(visible = expanded) {
                Column(Modifier.padding(top = 12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    LabeledChips(stringResource(R.string.weaknesses), boss.weaknesses)
                    LabeledChips(stringResource(R.string.resistances), boss.resistances)
                    LabeledChips(stringResource(R.string.drops), boss.drops)
                    Text(stringResource(R.string.strategy), style = MaterialTheme.typography.labelLarge)
                    Text(boss.strategy, style = MaterialTheme.typography.bodyMedium)
                }
            }
        }
    }
}
