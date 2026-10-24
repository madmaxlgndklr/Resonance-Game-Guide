package com.madmaxlgndklr.resonanceguide.ui.viewer

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import com.madmaxlgndklr.resonanceguide.R
import com.madmaxlgndklr.resonanceguide.ui.components.GuideImage
import com.madmaxlgndklr.resonanceguide.ui.components.ImmersiveMode

private const val MIN_SCALE = 1f
private const val MAX_SCALE = 8f
private const val DOUBLE_TAP_SCALE = 2.5f

/**
 * Full-screen map/sprite viewer. System bars are hidden for the lifetime of
 * this screen; tap toggles the overlay, pinch or double-tap zooms.
 */
@Composable
fun ImageViewerScreen(
    assetPath: String,
    title: String,
    onBack: () -> Unit,
) {
    ImmersiveMode()

    var showOverlay by remember { mutableStateOf(true) }
    var scale by remember { mutableFloatStateOf(MIN_SCALE) }
    var offset by remember { mutableStateOf(Offset.Zero) }
    var viewport by remember { mutableStateOf(IntSize.Zero) }

    fun clamp(candidate: Offset, atScale: Float): Offset {
        val maxX = viewport.width * (atScale - 1f) / 2f
        val maxY = viewport.height * (atScale - 1f) / 2f
        return Offset(candidate.x.coerceIn(-maxX, maxX), candidate.y.coerceIn(-maxY, maxY))
    }

    Box(
        Modifier
            .fillMaxSize()
            .background(Color.Black)
            .onSizeChanged { viewport = it }
            .pointerInput(Unit) {
                detectTapGestures(
                    onTap = { showOverlay = !showOverlay },
                    onDoubleTap = { tap ->
                        if (scale > MIN_SCALE) {
                            scale = MIN_SCALE
                            offset = Offset.Zero
                        } else {
                            val center = Offset(viewport.width / 2f, viewport.height / 2f)
                            scale = DOUBLE_TAP_SCALE
                            // Keep the tapped point under the finger.
                            offset = clamp((center - tap) * (DOUBLE_TAP_SCALE - 1f), DOUBLE_TAP_SCALE)
                        }
                    },
                )
            }
            .pointerInput(Unit) {
                detectTransformGestures { centroid, pan, zoom, _ ->
                    val newScale = (scale * zoom).coerceIn(MIN_SCALE, MAX_SCALE)
                    val center = Offset(viewport.width / 2f, viewport.height / 2f)
                    val fromCenter = centroid - center
                    // Zoom around the gesture centroid rather than the screen centre.
                    val newOffset = (offset - fromCenter) * (newScale / scale) + fromCenter + pan
                    scale = newScale
                    offset = clamp(newOffset, newScale)
                }
            },
    ) {
        GuideImage(
            assetPath = assetPath,
            contentDescription = title,
            contentScale = ContentScale.Fit,
            fullResolution = true,
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer {
                    scaleX = scale
                    scaleY = scale
                    translationX = offset.x
                    translationY = offset.y
                },
        )

        AnimatedVisibility(
            visible = showOverlay,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier.align(Alignment.TopCenter),
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Brush.verticalGradient(listOf(Color.Black.copy(alpha = 0.7f), Color.Transparent)))
                    // Bars are hidden, but keep controls clear of the camera cutout.
                    .windowInsetsPadding(WindowInsets.safeDrawing)
                    .padding(horizontal = 4.dp, vertical = 8.dp),
            ) {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.back), tint = Color.White)
                }
                Text(title, style = MaterialTheme.typography.titleMedium, color = Color.White)
            }
        }
    }
}
