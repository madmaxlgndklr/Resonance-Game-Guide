package com.madmaxlgndklr.resonanceguide.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import coil3.compose.AsyncImage
import coil3.request.ImageRequest
import com.madmaxlgndklr.resonanceguide.R
import com.madmaxlgndklr.resonanceguide.assets.GuideAssets

/**
 * Upper bound on the decoded size of full-screen maps: sharp when pinch-zoomed
 * on a QHD+ panel without decoding a multi-hundred-MB bitmap for huge sources.
 */
private const val MAX_VIEWER_DIMENSION_PX = 4096

/**
 * Loads a guide image (map or sprite) from local storage via Coil.
 *
 * @param fullResolution decode up to [MAX_VIEWER_DIMENSION_PX] instead of the
 *   layout size, for the zoomable viewer.
 */
@Composable
fun GuideImage(
    assetPath: String?,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    contentScale: ContentScale = ContentScale.Crop,
    fullResolution: Boolean = false,
) {
    val context = LocalContext.current
    val placeholder = painterResource(R.drawable.ic_image_placeholder)
    val request = remember(assetPath, fullResolution) {
        ImageRequest.Builder(context)
            .data(assetPath?.let { GuideAssets.resolve(context, it) })
            .apply { if (fullResolution) size(MAX_VIEWER_DIMENSION_PX) }
            .build()
    }
    AsyncImage(
        model = request,
        contentDescription = contentDescription,
        modifier = modifier,
        contentScale = contentScale,
        placeholder = placeholder,
        error = placeholder,
        fallback = placeholder,
    )
}
