package com.madmaxlgndklr.resonanceguide.ui.builds

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.madmaxlgndklr.resonanceguide.data.local.CharacterBuildEntity
import com.madmaxlgndklr.resonanceguide.data.repository.GuideRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn

class BuildsViewModel(repository: GuideRepository) : ViewModel() {
    val builds: StateFlow<List<CharacterBuildEntity>> = repository.builds()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
}
