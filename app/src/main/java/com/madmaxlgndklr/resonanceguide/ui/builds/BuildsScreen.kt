package com.madmaxlgndklr.resonanceguide.ui.builds

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material3.AssistChip
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
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
import com.madmaxlgndklr.resonanceguide.data.local.VisionSkillEntity
import com.madmaxlgndklr.resonanceguide.ui.AppViewModelProvider
import com.madmaxlgndklr.resonanceguide.ui.components.LabeledChips
import com.madmaxlgndklr.resonanceguide.ui.components.plus

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BuildsScreen(
    viewModel: BuildsViewModel = viewModel(factory = AppViewModelProvider.Factory),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val scrollBehavior = TopAppBarDefaults.enterAlwaysScrollBehavior()

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(stringResource(R.string.tab_builds))
                        Text(
                            stringResource(
                                R.string.visions_progress,
                                state.unlockedCount,
                                state.visions.size,
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
        // One column in portrait; the adaptive grid fills landscape and the
        // unfolded width without stretching cards.
        LazyVerticalGrid(
            columns = GridCells.Adaptive(minSize = 340.dp),
            contentPadding = padding.plus(16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            items(state.visions, key = { it.vision.vision.id }, contentType = { "vision" }) { card ->
                VisionCardView(card) { unlocked ->
                    viewModel.setUnlocked(card.vision.vision.id, unlocked)
                }
            }
        }
    }
}

@Composable
private fun VisionCardView(card: VisionCard, onToggleUnlocked: (Boolean) -> Unit) {
    val vision = card.vision.vision
    var expanded by rememberSaveable(vision.id) { mutableStateOf(false) }

    ElevatedCard(
        onClick = { expanded = !expanded },
        modifier = Modifier
            .fillMaxWidth()
            .animateContentSize(),
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    vision.origin?.let {
                        Text(
                            it,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    Text(vision.name, style = MaterialTheme.typography.titleLarge)
                    Text(
                        listOfNotNull(vision.role, vision.damageType).joinToString(" · "),
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.secondary,
                    )
                }
                Spacer(Modifier.size(8.dp))
                FilterChip(
                    selected = card.vision.unlocked,
                    onClick = { onToggleUnlocked(!card.vision.unlocked) },
                    label = { Text(stringResource(R.string.unlocked)) },
                )
            }

            vision.resonanceSkill?.let {
                AssistChip(onClick = {}, label = { Text(it) })
            }
            LabeledChips("Elements", vision.elements)

            vision.unlockRoute?.let {
                Text(
                    stringResource(R.string.unlock_route),
                    style = MaterialTheme.typography.labelLarge,
                )
                Text(
                    it,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            if (card.skills.isNotEmpty()) {
                AnimatedVisibility(visible = expanded) {
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(
                            stringResource(R.string.skills),
                            style = MaterialTheme.typography.labelLarge,
                        )
                        card.skills.forEach { SkillRow(it) }
                    }
                }
                if (!expanded) {
                    Text(
                        "${card.skills.size} ${stringResource(R.string.skills).lowercase()}",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}

@Composable
private fun SkillRow(skill: VisionSkillEntity) {
    Column(Modifier.padding(bottom = 2.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(skill.name, style = MaterialTheme.typography.titleSmall, modifier = Modifier.weight(1f))
            Text(
                buildList {
                    skill.masteryRank?.let { add(stringResource(R.string.mastery_rank, it)) }
                    skill.apCost?.let { add(stringResource(R.string.ap_cost, it)) }
                    skill.awakeningTier?.let { add(stringResource(R.string.awakening_tier, it)) }
                    if (isEmpty()) add(stringResource(R.string.innate_skill))
                }.joinToString(" · "),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.secondary,
            )
        }
        skill.description?.let {
            Text(
                it,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
