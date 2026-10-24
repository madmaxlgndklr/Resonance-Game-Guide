package com.madmaxlgndklr.resonanceguide.ui.walkthrough

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
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
import com.madmaxlgndklr.resonanceguide.data.local.QuestStepWithProgress
import com.madmaxlgndklr.resonanceguide.ui.AppViewModelProvider
import com.madmaxlgndklr.resonanceguide.ui.components.plus

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QuestDetailScreen(
    onBack: () -> Unit,
    onOpenMap: (assetPath: String, title: String) -> Unit,
    viewModel: QuestDetailViewModel = viewModel(factory = AppViewModelProvider.Factory),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            TopAppBar(
                title = { Text(state.quest?.title.orEmpty()) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.back))
                    }
                },
                actions = {
                    val markAll = !state.allComplete
                    IconButton(onClick = { viewModel.setAllCompleted(markAll) }) {
                        Icon(
                            if (markAll) Icons.Filled.DoneAll else Icons.Filled.RemoveDone,
                            contentDescription = stringResource(if (markAll) R.string.mark_all_done else R.string.mark_all_undone),
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
                    state.chapter?.let {
                        Text(it.title, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.secondary)
                    }
                    Text(state.quest?.description.orEmpty(), style = MaterialTheme.typography.bodyLarge)
                    Spacer(Modifier.height(8.dp))
                    Text(
                        stringResource(R.string.steps_progress, state.completedSteps, state.steps.size),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    val chapter = state.chapter
                    val map = chapter?.mapAsset
                    if (chapter != null && map != null) {
                        Spacer(Modifier.height(8.dp))
                        FilledTonalButton(onClick = { onOpenMap(map, chapter.title) }) {
                            Icon(Icons.Filled.Map, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.size(8.dp))
                            Text(stringResource(R.string.view_map))
                        }
                    }
                    Spacer(Modifier.height(8.dp))
                }
            }
            items(state.steps, key = { it.step.id }, contentType = { "step" }) { entry ->
                StepRow(entry, onToggle = { viewModel.setStepCompleted(entry.step.id, it) })
            }
        }
    }
}

@Composable
private fun StepRow(entry: QuestStepWithProgress, onToggle: (Boolean) -> Unit) {
    val hint = entry.step.hint
    ListItem(
        modifier = Modifier
            .fillMaxWidth()
            .toggleable(value = entry.completed, role = Role.Checkbox, onValueChange = onToggle),
        colors = ListItemDefaults.colors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
        leadingContent = { Checkbox(checked = entry.completed, onCheckedChange = null) },
        overlineContent = { Text("Step ${entry.step.stepNumber}") },
        headlineContent = {
            Text(
                entry.step.instruction,
                textDecoration = if (entry.completed) TextDecoration.LineThrough else null,
            )
        },
        supportingContent = if (hint != null) {
            { Text(hint) }
        } else {
            null
        },
    )
}
