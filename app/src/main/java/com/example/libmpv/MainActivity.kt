package com.example.libmpv

import android.os.Bundle
import android.view.SurfaceHolder
import android.view.SurfaceView
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.viewinterop.AndroidView
import fr.nextv.libmpv.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import kotlin.time.Duration

class MainActivity : ComponentActivity() {

  init {
    LibMpvJni.initializeLibMpv()
  }

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    val properties = listOf(
      "track-list" to MpvEntities.Format.String,
      "seeking" to MpvEntities.Format.Flag,
      "time-pos/full" to MpvEntities.Format.Int,
      "duration/full" to MpvEntities.Format.Int,
    )
    setContent {
      val player = remember {
        MpvPlayer(configuration = MpvConfiguration()).apply {
          properties.forEach {
            observeProperty(it.first, it.second)
          }
          this.registerEventListener {
            println("it.property?.value = ${it.property?.value}")
          }
        }
      }

      DisposableEffect(Unit) {
        onDispose { player.release() }
      }

      val callback = remember {
        object : SurfaceHolder.Callback {
          override fun surfaceChanged(holder: SurfaceHolder, format: Int, width: Int, height: Int) = player.onSurfaceSizeChanged(width, height)
          override fun surfaceCreated(holder: SurfaceHolder) = player.onSurfaceCreated(holder.surface)
          override fun surfaceDestroyed(holder: SurfaceHolder) = player.onSurfaceDestroyed()
        }
      }
      LaunchedEffect(Unit) {
        delay(3000)
        println("Setting media source")
        withContext(Dispatchers.Main) {
          val url = "https://storage.googleapis.com/exoplayer-test-media-1/gen-3/screens/dash-vod-single-segment/video-avc-baseline-480.mp4"
          player.setMediaSource(start = Duration.ZERO, end = Duration.ZERO, url = url)
        }
      }
      AndroidView(
        factory = {
          SurfaceView(it).apply {
            holder.addCallback(callback)
          }
        },
        onRelease = {
          it.holder.removeCallback(callback)
        }
      )
    }
  }
}
