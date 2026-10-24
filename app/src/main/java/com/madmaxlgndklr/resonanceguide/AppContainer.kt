package com.madmaxlgndklr.resonanceguide

import android.content.Context
import com.madmaxlgndklr.resonanceguide.data.local.GuideDatabase
import com.madmaxlgndklr.resonanceguide.data.repository.GuideRepository

/** Manual dependency container; one instance lives on [ResonanceGuideApplication]. */
class AppContainer(context: Context) {
    private val database = GuideDatabase.build(context)

    val guideRepository = GuideRepository(
        database = database,
        prefs = context.getSharedPreferences("guide_meta", Context.MODE_PRIVATE),
    )
}
