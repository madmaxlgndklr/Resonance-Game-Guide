package com.madmaxlgndklr.resonanceguide.assets

import android.content.Context
import java.io.File

/**
 * Resolves a guide-relative asset path (e.g. `maps/chapter_01.webp`) to a Coil model.
 *
 * Lookup order:
 * 1. Sideloaded files in `Android/data/<package>/files/guide/`, pushed with
 *    `scripts/push-assets.sh`. Lets you swap art without rebuilding.
 * 2. APK assets packaged from the `resonance.assetsDir` Gradle property
 *    (default `/home/madmaxlgndklr/Git/Resonance-Assets`).
 */
object GuideAssets {
    const val SIDELOAD_DIR = "guide"
    private const val ANDROID_ASSET_PREFIX = "file:///android_asset/"

    fun resolve(context: Context, relativePath: String): Any {
        val path = relativePath.trimStart('/')
        val sideloaded = context.getExternalFilesDir(null)
            ?.let { File(it, "$SIDELOAD_DIR/$path") }
        return if (sideloaded?.isFile == true) sideloaded else ANDROID_ASSET_PREFIX + path
    }
}
