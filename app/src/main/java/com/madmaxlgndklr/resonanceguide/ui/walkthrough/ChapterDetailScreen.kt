package com.madmaxlgndklr.resonanceguide.ui.walkthrough

import android.content.Intent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
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
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.DoneAll
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.RemoveDone
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.madmaxlgndklr.resonanceguide.R
import com.madmaxlgndklr.resonanceguide.data.local.ObjectiveWithProgress
import com.madmaxlgndklr.resonanceguide.data.local.ShardWithProgress
import com.madmaxlgndklr.resonanceguide.data.local.SideQuestWithProgress
import com.madmaxlgndklr.resonanceguide.data.local.WalkthroughAreaWithProgress
import com.madmaxlgndklr.resonanceguide.ui.AppViewModelProvider
import com.madmaxlgndklr.resonanceguide.ui.components.LabeledChips
import com.madmaxlgndklr.resonanceguide.ui.components.SectionHeader
import com.madmaxlgndklr.resonanceguide.ui.components.plus

/** Where the route notes came from, shown as a credit under the step list. */
private const val SOURCE_GUIDE_URL =
    "https://www.rpgsite.net/guide/21486-final-fantasy-resonance-walkthrough-100-items-" +
        "all-achievements-tablet-shards-step-by-step-guide"

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChapterDetailScreen(
    onBack: () -> Unit,
    onOpenMap: (assetPath: String, title: String) -> Unit,
    viewModel: ChapterDetailViewModel = viewModel(factory = AppViewModelProvider.Factory),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
    val context = LocalContext.current

    // Which area cards are open. Held here rather than per-row so that
    // scrolling a card out of view and back does not collapse it.
    val expanded = remember { mutableStateMapOf<String, Boolean>() }

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
                    val target = state.bulkTarget
                    val markAll = state.bulkMarkAll
                    IconButton(onClick = { viewModel.setAllCompleted(target, markAll) }) {
                        Icon(
                            if (markAll) Icons.Filled.DoneAll else Icons.Filled.RemoveDone,
                            contentDescription = stringResource(
                                when (target) {
                                    BulkTarget.AREAS ->
                                        if (markAll) {
                                            R.string.mark_all_areas_done
                                        } else {
                                            R.string.mark_all_areas_undone
                                        }
                                    BulkTarget.OBJECTIVES ->
                                        if (markAll) {
                                            R.string.mark_all_done
                                        } else {
                                            R.string.mark_all_undone
                                        }
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
            item(key = "header", contentType = "header") {
                ChapterHeader(state, onOpenMap)
            }

            if (state.areas.isNotEmpty()) {
                item(key = "areas_header") {
                    SectionHeader(stringResource(R.string.walkthrough_areas))
                }
                items(state.areas, key = { it.area.id }, contentType = { "area" }) { entry ->
                    AreaCard(
                        entry = entry,
                        expanded = expanded[entry.area.id] ?: false,
                        onExpandToggle = {
                            expanded[entry.area.id] = !(expanded[entry.area.id] ?: false)
                        },
                        onVisitedChange = { viewModel.setAreaVisited(entry.area.id, it) },
                    )
                }
                item(key = "source_credit") {
                    SourceCredit(
                        onOpen = {
                            context.startActivity(
                                Intent(Intent.ACTION_VIEW, SOURCE_GUIDE_URL.toUri()),
                            )
                        },
                    )
                }
            }

            if (state.dialogueChoices.isNotEmpty()) {
                item(key = "dialogue_header") {
                    Column {
                        SectionHeader(stringResource(R.string.dialogue_choices))
                        Text(
                            stringResource(R.string.dialogue_choices_hint),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
                items(
                    state.dialogueChoices,
                    key = { it.options.first().id },
                    contentType = { "dialogue" },
                ) { group ->
                    DialogueChoiceCard(group)
                }
            }

            if (state.objectives.isNotEmpty()) {
                item(key = "objectives_header") {
                    SectionHeader(stringResource(R.string.quest_log))
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
private fun ChapterHeader(
    state: ChapterDetailUiState,
    onOpenMap: (assetPath: String, title: String) -> Unit,
) {
    Column {
        state.chapter?.region?.let {
            Text(
                it,
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.secondary,
            )
        }
        // Areas lead the progress line because they lead the screen; the
        // quest-log count follows, and drops out on a dataset without areas.
        val progress = buildList {
            if (state.areas.isNotEmpty()) {
                add(
                    stringResource(
                        R.string.areas_progress,
                        state.visitedAreas,
                        state.areas.size,
                    ),
                )
            }
            if (state.objectives.isNotEmpty()) {
                add(
                    stringResource(
                        R.string.objectives_progress,
                        state.completedObjectives,
                        state.objectives.size,
                    ),
                )
            }
            add(
                stringResource(
                    R.string.shards_progress,
                    state.collectedShards,
                    state.shards.size,
                ),
            )
        }
        Text(
            progress.joinToString("  ·  "),
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

/**
 * One stop on the route, collapsed to its summary until tapped.
 *
 * The checkbox and the expand toggle are deliberately separate targets: a
 * reader following along taps the card to read the steps far more often than
 * they tick it off, and merging the two would make every read a write.
 */
@Composable
private fun AreaCard(
    entry: WalkthroughAreaWithProgress,
    expanded: Boolean,
    onExpandToggle: () -> Unit,
    onVisitedChange: (Boolean) -> Unit,
) {
    val area = entry.area
    ElevatedCard(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .clickable(onClick = onExpandToggle)
                .padding(start = 4.dp, end = 4.dp, top = 4.dp, bottom = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Checkbox(
                checked = entry.visited,
                onCheckedChange = onVisitedChange,
            )
            Column(Modifier.weight(1f).padding(vertical = 8.dp)) {
                Text(
                    "${area.sortOrder}. ${area.name}",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    textDecoration = if (entry.visited) TextDecoration.LineThrough else null,
                )
                area.kind?.let { kind ->
                    Text(
                        kind.replace('_', ' '),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.secondary,
                    )
                }
                if (area.shardCount > 0) {
                    Text(
                        pluralStringResource(
                            R.plurals.shards_here,
                            area.shardCount,
                            area.shardCount,
                        ),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.tertiary,
                    )
                }
            }
            Icon(
                if (expanded) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore,
                contentDescription = null,
            )
        }
        AnimatedVisibility(visible = expanded) {
            Column(
                Modifier.padding(start = 16.dp, end = 16.dp, bottom = 16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                area.summary?.let {
                    Text(it, style = MaterialTheme.typography.bodyMedium)
                }
                if (area.steps.isNotEmpty()) {
                    HorizontalDivider()
                    area.steps.forEach { step ->
                        Row {
                            Text(
                                "•  ",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.secondary,
                            )
                            Text(step, style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                }
                LabeledChips(stringResource(R.string.bosses), area.bosses)
                LabeledChips(stringResource(R.string.side_quests), area.sideQuests)
                if (area.chests.isNotEmpty()) {
                    HorizontalDivider()
                    Text(
                        stringResource(R.string.chests),
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    area.chests.forEach { chest ->
                        Text("—  $chest", style = MaterialTheme.typography.bodySmall)
                    }
                }
                if (area.notableSteals.isNotEmpty()) {
                    HorizontalDivider()
                    Text(
                        stringResource(R.string.steals),
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    area.notableSteals.forEach { steal ->
                        Text("—  $steal", style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
        }
    }
}

/** One conversation, with all three options side by side and their bonuses. */
@Composable
private fun DialogueChoiceCard(group: DialogueChoiceGroup) {
    ElevatedCard(modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(
                group.speaker,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
            )
            group.prompt?.let {
                Text(
                    it,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            group.options.forEach { option ->
                HorizontalDivider()
                option.bonus?.let {
                    Text(
                        it,
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.tertiary,
                    )
                }
                option.choice?.let {
                    Text("“$it”", style = MaterialTheme.typography.bodyMedium)
                }
            }
        }
    }
}

/** Credits the walkthrough the route notes were written from. */
@Composable
private fun SourceCredit(onOpen: () -> Unit) {
    Column(Modifier.padding(top = 12.dp)) {
        Text(
            stringResource(R.string.source_credit, stringResource(R.string.source_name)),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        TextButton(onClick = onOpen) {
            Text(stringResource(R.string.open_source_guide))
            Spacer(Modifier.size(8.dp))
            Icon(
                Icons.AutoMirrored.Filled.OpenInNew,
                contentDescription = null,
                modifier = Modifier.size(16.dp),
            )
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
