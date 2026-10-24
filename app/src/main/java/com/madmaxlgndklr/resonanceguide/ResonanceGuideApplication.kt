package com.madmaxlgndklr.resonanceguide

import android.app.Application
import coil3.ImageLoader
import coil3.PlatformContext
import coil3.SingletonImageLoader
import coil3.memory.MemoryCache
import coil3.request.crossfade
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class ResonanceGuideApplication : Application(), SingletonImageLoader.Factory {

    lateinit var container: AppContainer
        private set

    private val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
        applicationScope.launch { container.guideRepository.seedIfNeeded() }
    }

    // All art is local (bundled assets or sideloaded files), so no network
    // fetcher or disk cache is configured. A generous memory cache keeps
    // decoded sprites around while flinging lists on the 120 Hz display.
    override fun newImageLoader(context: PlatformContext): ImageLoader =
        ImageLoader.Builder(context)
            .crossfade(true)
            .memoryCache {
                MemoryCache.Builder()
                    .maxSizePercent(context, 0.25)
                    .build()
            }
            .build()
}
