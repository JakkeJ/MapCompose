package ovh.plrapps.mapcompose.viewmodels

import android.app.Application
import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.AndroidViewModel
import ovh.plrapps.mapcompose.api.shouldLoopScale
import ovh.plrapps.mapcompose.core.TileStreamProvider
import ovh.plrapps.mapcompose.ui.state.MapState
import java.io.InputStream

class RotationDemoViewModel(application: Application) : AndroidViewModel(application) {
    val appContext: Context by lazy {
        getApplication<Application>().applicationContext
    }
    val tileStreamProvider = object : TileStreamProvider {
        override suspend fun getTileStream(row: Int, col: Int, zoomLvl: Int): InputStream? {
            return try {
                appContext.assets?.open("tiles/mont_blanc/$zoomLvl/$row/$col.jpg")
            } catch (e: Exception) {
                null
            }
        }
    }

    val state: MapState by mutableStateOf(
        MapState(4, 4096, 4096, tileStreamProvider).also {
            it.shouldLoopScale = true
        }
    )
}