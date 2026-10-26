package com.madmaxlgndklr.resonanceguide.ui.walkthrough

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
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.madmaxlgndklr.resonanceguide.R
import com.madmaxlgndklr.resonanceguide.data.local.ChapterSummary
import com.madmaxlgndklr.resonanceguide.data.local.isComplete
import com.madmaxlgndklr.resonanceguide.data.local.progressFraction
import com.madmaxlgndklr.resonanceguide.ui.AppViewModelProvider
import com.madmaxlgndklr.resonanceguide.ui.components.EmptyState
import com.madmaxlgndklr.resonanceguide.ui.components.plus

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WalkthroughScreen(
    onChapterClick: (chapterId: String) -> Unit,
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
                            stringResource(
                                R.string.chapters_progress,
                                state.completedChapters,
                                state.chapters.size,
                            ) + "  ·  " + stringResource(
                                R.string.shards_progress,
                                state.collectedShards,
                                state.totalShards,
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
        if (!state.isLoading && state.chapters.isEmpty()) {
            EmptyState(Modifier.padding(padding))
            return@Scaffold
        }
        LazyColumn(
            contentPadding = padding.plus(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            items(
                items = state.chapters,
                key = { it.chapter.id },
                contentType = { "chapter" },
            ) { summary ->
                ChapterCard(
                    summary = summary,
                    onClick = { onChapterClick(summary.chapter.id) },
                    onOpenMap = onOpenMap,
                )
            }
        }
    }
}

@Composable
private fun ChapterCard(
    summary: ChapterSummary,
    onClick: () -> Unit,
    onOpenMap: (assetPath: String, title: String) -> Unit,
) {
    val chapter = summary.chapter
    ElevatedCard(onClick = onClick, modifier = Modifier.fillMaxWidth()) {
        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = if (summary.isComplete) {
                    Icons.Filled.CheckCircle
                } else {
                    Icons.Outlined.RadioButtonUnchecked
                },
                contentDescription = null,
                tint = if (summary.isComplete) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.outline
                },
                modifier = Modifier.size(28.dp),
            )
            Spacer(Modifier.size(16.dp))
            Column(Modifier.weight(1f)) {
                Text(chapter.title, style = MaterialTheme.typography.titleMedium)
                chapter.region?.let {
                    Text(
                        it,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                chapter.recommendedLevel?.let {
                    Text(
                        "Lv $it",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.secondary,
                    )
                }
                Spacer(Modifier.height(8.dp))
                LinearProgressIndicator(
                    progress = { summary.progressFraction },
                    modifier = Modifier.fillMaxWidth(),
                )
                Text(
                    stringResource(
                        R.string.objectives_progress,
                        summary.completedObjectives,
                        summary.totalObjectives,
                    ) + "  ·  " + stringResource(
                        R.string.shards_progress,
                        summary.collectedShards,
                        summary.totalShards,
                    ),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 4.dp),
                )
            }
            chapter.mapAsset?.let { map ->
                IconButton(onClick = { onOpenMap(map, chapter.title) }) {
                    Icon(Icons.Filled.Map, contentDescription = stringResource(R.string.view_map))
                }
            }
        }
    }
}
