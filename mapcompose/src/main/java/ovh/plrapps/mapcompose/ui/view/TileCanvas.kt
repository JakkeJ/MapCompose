package ovh.plrapps.mapcompose.ui.view

import android.graphics.Paint
import android.graphics.Rect
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
import ovh.plrapps.mapcompose.core.ColorFilterProvider
import ovh.plrapps.mapcompose.core.Tile
import ovh.plrapps.mapcompose.core.VisibleTilesResolver
import ovh.plrapps.mapcompose.ui.state.ZoomPanRotateState

@Composable
internal fun TileCanvas(
    modifier: Modifier,
    zoomPRState: ZoomPanRotateState,
    visibleTilesResolver: VisibleTilesResolver,
    tileSize: Int,
    alphaTick: Float,
    colorFilterProvider: ColorFilterProvider?,
    tilesToRender: List<Tile>,
    isBitmapFilteringEnabled: Boolean
) {
    val dest = remember { Rect() }
    val paint: Paint = remember {
        Paint().apply {
            isAntiAlias = false
        }
    }

    Canvas(
        modifier = modifier
            .fillMaxSize()
    ) {
        withTransform({
            /* Geometric transformations seem to be applied in reversed order of declaration */
            translate(left = -zoomPRState.scrollX, top = -zoomPRState.scrollY)
            rotate(
                degrees = zoomPRState.rotation,
                pivot = Offset(
                    x = zoomPRState.centroidX.toFloat() * zoomPRState.fullWidth * zoomPRState.scale,
                    y = zoomPRState.centroidY.toFloat() * zoomPRState.fullHeight * zoomPRState.scale
                )
            )
            scale(scale = zoomPRState.scale, Offset.Zero)
        }) {
            for (tile in tilesToRender) {
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
                paint.isFilterBitmap = isBitmapFilteringEnabled

                drawIntoCanvas {
                    it.nativeCanvas.drawBitmap(tile.bitmap, null, dest, paint)
                }

                /* If a tile isn't fully opaque, increase its alpha state by the alpha tick */
                if (tile.alpha < 1f) {
                    tile.alpha = (tile.alpha + alphaTick).coerceAtMost(1f)
                }
            }
        }
    }
}