package com.madmaxlgndklr.resonanceguide.ui.walkthrough

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.selection.toggleable
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.DoneAll
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.RemoveDone
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
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
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.madmaxlgndklr.resonanceguide.R
import com.madmaxlgndklr.resonanceguide.data.local.ObjectiveWithProgress
import com.madmaxlgndklr.resonanceguide.data.local.ShardWithProgress
import com.madmaxlgndklr.resonanceguide.data.local.SideQuestWithProgress
import com.madmaxlgndklr.resonanceguide.ui.AppViewModelProvider
import com.madmaxlgndklr.resonanceguide.ui.components.SectionHeader
import com.madmaxlgndklr.resonanceguide.ui.components.plus

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChapterDetailScreen(
    onBack: () -> Unit,
    onOpenMap: (assetPath: String, title: String) -> Unit,
    viewModel: ChapterDetailViewModel = viewModel(factory = AppViewModelProvider.Factory),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            TopAppBar(
                title = { Text(state.chapter?.title.orEmpty()) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.back),
                        )
                    }
                },
                actions = {
                    val markAll = !state.allObjectivesDone
                    IconButton(onClick = { viewModel.setAllObjectivesCompleted(markAll) }) {
                        Icon(
                            if (markAll) Icons.Filled.DoneAll else Icons.Filled.RemoveDone,
                            contentDescription = stringResource(
                                if (markAll) R.string.mark_all_done else R.string.mark_all_undone,
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
            item(key = "header", contentType = "header") {
                Column {
                    state.chapter?.region?.let {
                        Text(
                            it,
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.secondary,
                        )
                    }
                    Text(
                        stringResource(
                            R.string.objectives_progress,
                            state.completedObjectives,
                            state.objectives.size,
                        ) + "  ·  " + stringResource(
                            R.string.shards_progress,
                            state.collectedShards,
                            state.shards.size,
                        ),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    val chapter = state.chapter
                    val map = chapter?.mapAsset
                    if (chapter != null && map != null) {
                        Spacer(Modifier.height(8.dp))
                        FilledTonalButton(onClick = { onOpenMap(map, chapter.title) }) {
                            Icon(
                                Icons.Filled.Map,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp),
                            )
                            Spacer(Modifier.size(8.dp))
                            Text(stringResource(R.string.view_map))
                        }
                    }
                }
            }

            if (state.objectives.isNotEmpty()) {
                item(key = "objectives_header") {
                    SectionHeader(stringResource(R.string.objectives))
                }
                items(state.objectives, key = { it.objective.id }, contentType = { "objective" }) {
                    ObjectiveRow(it) { done ->
                        viewModel.setObjectiveCompleted(it.objective.id, done)
                    }
                }
            }

            if (state.sideQuests.isNotEmpty()) {
                item(key = "quests_header") {
                    SectionHeader(stringResource(R.string.side_quests))
                }
                items(state.sideQuests, key = { it.quest.id }, contentType = { "quest" }) {
                    SideQuestRow(it) { done ->
                        viewModel.setSideQuestCompleted(it.quest.id, done)
                    }
                }
            }

            if (state.shards.isNotEmpty()) {
                item(key = "shards_header") {
                    SectionHeader(stringResource(R.string.tablet_shards))
                }
                items(state.shards, key = { it.shard.id }, contentType = { "shard" }) {
                    ShardRow(it) { done ->
                        viewModel.setShardCollected(it.shard.id, done)
                    }
                }
            }
        }
    }
}

@Composable
private fun ObjectiveRow(entry: ObjectiveWithProgress, onToggle: (Boolean) -> Unit) {
    val puzzle = entry.objective.dungeonPuzzleSolution
    ListItem(
        modifier = Modifier
            .fillMaxWidth()
            .toggleable(value = entry.completed, role = Role.Checkbox, onValueChange = onToggle),
        colors = ListItemDefaults.colors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
        ),
        leadingContent = { Checkbox(checked = entry.completed, onCheckedChange = null) },
        overlineContent = { Text("Step ${entry.objective.step}") },
        headlineContent = {
            Text(
                entry.objective.text,
                textDecoration = if (entry.completed) TextDecoration.LineThrough else null,
            )
        },
        supportingContent = if (puzzle != null) {
            { Text(puzzle) }
        } else {
            null
        },
    )
}

@Composable
private fun SideQuestRow(entry: SideQuestWithProgress, onToggle: (Boolean) -> Unit) {
    val quest = entry.quest
    ListItem(
        modifier = Modifier
            .fillMaxWidth()
            .toggleable(value = entry.completed, role = Role.Checkbox, onValueChange = onToggle),
        colors = ListItemDefaults.colors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
        ),
        leadingContent = { Checkbox(checked = entry.completed, onCheckedChange = null) },
        overlineContent = (quest.region ?: quest.location)?.let { place -> { Text(place) } },
        headlineContent = {
            Text(
                quest.title,
                textDecoration = if (entry.completed) TextDecoration.LineThrough else null,
            )
        },
        supportingContent = if (quest.rewards.isNotEmpty()) {
            {
                Text(
                    stringResource(R.string.rewards) + ": " + quest.rewards.joinToString(", "),
                    color = MaterialTheme.colorScheme.secondary,
                )
            }
        } else {
            null
        },
    )
}

@Composable
private fun ShardRow(entry: ShardWithProgress, onToggle: (Boolean) -> Unit) {
    val shard = entry.shard
    ListItem(
        modifier = Modifier
            .fillMaxWidth()
            .toggleable(value = entry.collected, role = Role.Checkbox, onValueChange = onToggle),
        colors = ListItemDefaults.colors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
        ),
        leadingContent = { Checkbox(checked = entry.collected, onCheckedChange = null) },
        overlineContent = { Text("#${shard.number}  ${shard.region.orEmpty()}") },
        headlineContent = {
            Text(
                shard.exactLocationDescription.orEmpty(),
                textDecoration = if (entry.collected) TextDecoration.LineThrough else null,
            )
        },
        supportingContent = shard.missableWarning?.let { warning ->
            { Text(warning, color = MaterialTheme.colorScheme.error) }
        },
    )
}
