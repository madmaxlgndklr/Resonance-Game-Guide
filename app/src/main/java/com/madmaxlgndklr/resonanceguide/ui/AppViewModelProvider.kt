package com.madmaxlgndklr.resonanceguide.ui

import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.viewmodel.CreationExtras
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.madmaxlgndklr.resonanceguide.ResonanceGuideApplication
import com.madmaxlgndklr.resonanceguide.ui.bestiary.BestiaryViewModel
import com.madmaxlgndklr.resonanceguide.ui.builds.BuildsViewModel
import com.madmaxlgndklr.resonanceguide.ui.items.ItemsViewModel
import com.madmaxlgndklr.resonanceguide.ui.walkthrough.QuestDetailViewModel
import com.madmaxlgndklr.resonanceguide.ui.walkthrough.WalkthroughViewModel

object AppViewModelProvider {
    val Factory = viewModelFactory {
        initializer { WalkthroughViewModel(repository()) }
        initializer { QuestDetailViewModel(createSavedStateHandle(), repository()) }
        initializer { BestiaryViewModel(repository()) }
        initializer { BuildsViewModel(repository()) }
        initializer { ItemsViewModel(repository()) }
    }

    private fun CreationExtras.repository() =
        (this[APPLICATION_KEY] as ResonanceGuideApplication).container.guideRepository
}
