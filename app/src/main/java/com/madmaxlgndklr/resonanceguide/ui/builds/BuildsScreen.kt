package com.madmaxlgndklr.resonanceguide.ui.builds

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material3.AssistChip
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
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
import com.madmaxlgndklr.resonanceguide.data.local.CharacterBuildEntity
import com.madmaxlgndklr.resonanceguide.ui.AppViewModelProvider
import com.madmaxlgndklr.resonanceguide.ui.components.GuideImage
import com.madmaxlgndklr.resonanceguide.ui.components.LabeledChips
import com.madmaxlgndklr.resonanceguide.ui.components.plus

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BuildsScreen(
    onOpenSprite: (assetPath: String, title: String) -> Unit,
    viewModel: BuildsViewModel = viewModel(factory = AppViewModelProvider.Factory),
) {
    val builds by viewModel.builds.collectAsStateWithLifecycle()
    val scrollBehavior = TopAppBarDefaults.enterAlwaysScrollBehavior()

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            TopAppBar(title = { Text(stringResource(R.string.tab_builds)) }, scrollBehavior = scrollBehavior)
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
            items(builds, key = { it.id }, contentType = { "build" }) { build ->
                BuildCard(build, onOpenSprite)
            }
        }
    }
}

@Composable
private fun BuildCard(
    build: CharacterBuildEntity,
    onOpenSprite: (assetPath: String, title: String) -> Unit,
) {
    ElevatedCard(Modifier.fillMaxWidth()) {
        GuideImage(
            assetPath = build.spriteAsset,
            contentDescription = build.characterName,
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(16f / 9f)
                .clickable(enabled = build.spriteAsset != null) {
                    build.spriteAsset?.let { onOpenSprite(it, build.characterName) }
                },
        )
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(build.characterName, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.secondary)
                    Text(build.buildName, style = MaterialTheme.typography.titleLarge)
                }
                Spacer(Modifier.size(8.dp))
                AssistChip(onClick = {}, label = { Text(build.role) })
            }
            Text(build.summary, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            LabeledChips(stringResource(R.string.skills), build.skills)
            LabeledChips(stringResource(R.string.gear), build.gear)
        }
    }
}
