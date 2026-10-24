package com.madmaxlgndklr.resonanceguide.ui.walkthrough

import androidx.compose.foundation.ExperimentalFoundationApi
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.outlined.RadioButtonUnchecked
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.madmaxlgndklr.resonanceguide.R
import com.madmaxlgndklr.resonanceguide.data.local.ChapterEntity
import com.madmaxlgndklr.resonanceguide.data.local.QuestSummary
import com.madmaxlgndklr.resonanceguide.data.local.isComplete
import com.madmaxlgndklr.resonanceguide.data.local.progressFraction
import com.madmaxlgndklr.resonanceguide.ui.AppViewModelProvider
import com.madmaxlgndklr.resonanceguide.ui.components.plus

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun WalkthroughScreen(
    onQuestClick: (questId: Int) -> Unit,
    onOpenMap: (assetPath: String, title: String) -> Unit,
    viewModel: WalkthroughViewModel = viewModel(factory = AppViewModelProvider.Factory),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val scrollBehavior = TopAppBarDefaults.enterAlwaysScrollBehavior()

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(stringResource(R.string.tab_walkthrough))
                        Text(
                            stringResource(R.string.quests_progress, state.completedQuests, state.totalQuests),
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
            state.sections.forEach { section ->
                stickyHeader(key = "chapter-${section.chapter.id}", contentType = "chapter") {
                    ChapterHeader(section.chapter, onOpenMap)
                }
                items(
                    items = section.quests,
                    key = { "quest-${it.quest.id}" },
                    contentType = { "quest" },
                ) { summary ->
                    QuestCard(summary, onClick = { onQuestClick(summary.quest.id) })
                }
            }
        }
    }
}

@Composable
private fun ChapterHeader(
    chapter: ChapterEntity,
    onOpenMap: (assetPath: String, title: String) -> Unit,
) {
    Surface(color = MaterialTheme.colorScheme.background, modifier = Modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(vertical = 4.dp)) {
            Column(Modifier.weight(1f)) {
                Text(chapter.title, style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.secondary)
                Text(chapter.summary, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            chapter.mapAsset?.let { map ->
                IconButton(onClick = { onOpenMap(map, chapter.title) }) {
                    Icon(Icons.Filled.Map, contentDescription = stringResource(R.string.view_map))
                }
            }
        }
    }
}

@Composable
private fun QuestCard(summary: QuestSummary, onClick: () -> Unit) {
    ElevatedCard(onClick = onClick, modifier = Modifier.fillMaxWidth()) {
        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = if (summary.isComplete) Icons.Filled.CheckCircle else Icons.Outlined.RadioButtonUnchecked,
                contentDescription = null,
                tint = if (summary.isComplete) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline,
                modifier = Modifier.size(28.dp),
            )
            Spacer(Modifier.size(16.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    stringResource(if (summary.quest.isMainStory) R.string.main_story else R.string.side_quest),
                    style = MaterialTheme.typography.labelSmall,
                    color = if (summary.quest.isMainStory) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.tertiary,
                )
                Text(summary.quest.title, style = MaterialTheme.typography.titleMedium)
                Text(
                    summary.quest.description,
                    style = MaterialTheme.typography.bodyMedium,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.height(8.dp))
                LinearProgressIndicator(
                    progress = { summary.progressFraction },
                    modifier = Modifier.fillMaxWidth(),
                )
                Text(
                    stringResource(R.string.steps_progress, summary.completedSteps, summary.totalSteps),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 4.dp),
                )
            }
        }
    }
}
