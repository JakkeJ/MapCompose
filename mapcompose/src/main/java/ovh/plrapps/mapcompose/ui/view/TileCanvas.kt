package ovh.plrapps.mapcompose.ui.view

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Paint
import android.graphics.Rect
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.asAndroidColorFilter
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.platform.LocalContext
import ovh.plrapps.mapcompose.R
import ovh.plrapps.mapcompose.core.*
import ovh.plrapps.mapcompose.ui.state.ZoomPanRotateState

@RequiresApi(Build.VERSION_CODES.N)
@Composable
internal fun TileCanvas(
    modifier: Modifier,
    zoomPRState: ZoomPanRotateState,
    visibleTilesResolver: VisibleTilesResolver,
    tileSize: Int,
    alphaTick: Float,
    colorFilterProvider: ColorFilterProvider?,
    tilesToRender: List<Tile>,
    isFilteringBitmap: () -> Boolean,
    isLoading: (row: Int, col: Int, zoom: Int) -> Boolean,
    loadingStates: Map<TileKey, Boolean>
) {
    val dest = remember { Rect() }
    val paint: Paint = remember {
        Paint().apply {
            isAntiAlias = false
        }
    }
    val context = LocalContext.current // Get the context
    val placeholderBitmap = remember {
        val options = BitmapFactory.Options()
        options.inPreferredConfig = Bitmap.Config.ARGB_8888  // Or a suitable config
        BitmapFactory.decodeResource(context.resources, R.drawable.placeholder, options)
    }
    val bitmap = remember {
        val options = BitmapFactory.Options()
        options.inPreferredConfig = Bitmap.Config.ARGB_8888  // Or a suitable config
        BitmapFactory.decodeResource(context.resources, R.drawable.placeholder, options)
    }


    Canvas(
        modifier = modifier
            .fillMaxSize()
    ) {
        withTransform({
            /* Geometric transformations seem to be applied in reversed order of declaration */
            rotate(
                degrees = zoomPRState.rotation,
                pivot = Offset(
                    x = zoomPRState.pivotX.toFloat(),
                    y = zoomPRState.pivotY.toFloat()
                )
            )
            translate(left = -zoomPRState.scrollX, top = -zoomPRState.scrollY)
            scale(scale = zoomPRState.scale, Offset.Zero)
        }) {
            paint.isFilterBitmap = isFilteringBitmap()
            for (tile in tilesToRender) {
                val isLoading = if (loadingStates is Map) {
                    loadingStates.getOrDefault(TileKey(tile.row, tile.col, tile.zoom), true)
                } else {
                    isLoading(tile.row, tile.col, tile.zoom)
                }

                if (isLoading) {
                    drawIntoCanvas {
                        it.nativeCanvas.drawBitmap(placeholderBitmap, null, dest, paint)
                    }
                } else {
                    val bitmap = tile.bitmap ?: continue
                    val scaleForLevel = visibleTilesResolver.getScaleForLevel(tile.zoom)
                        ?: continue
                    val tileScaled = (tileSize / scaleForLevel).toInt()
                    val l = tile.col * tileScaled
                    val t = tile.row * tileScaled
                    val r = l + tileScaled
                    val b = t + tileScaled
                    dest.set(l, t, r, b)

                    val colorFilter = colorFilterProvider?.getColorFilter(tile.row, tile.col, tile.zoom)

                    paint.alpha = (tile.alpha * 255).toInt()
                    paint.colorFilter = colorFilter?.asAndroidColorFilter()

                    drawIntoCanvas {
                        it.nativeCanvas.drawBitmap(bitmap, null, dest, paint)
                    }

                    /* If a tile isn't fully opaque, increase its alpha state by the alpha tick */
                    if (tile.alpha < 1f) {
                        tile.alpha = (tile.alpha + alphaTick).coerceAtMost(1f)
                    }
                }
            }
        }
    }
}