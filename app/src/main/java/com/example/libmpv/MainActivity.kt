package com.example.libmpv

import android.os.Bundle
import android.view.SurfaceHolder
import android.view.SurfaceView
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import fr.nextv.libmpv.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import java.io.File
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

class MainActivity : ComponentActivity() {

  init {
    LibMpv.initializeLibMpv()
  }

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    val properties = listOf(
      "track-list" to LibMpv.Format.String,
      "seeking" to LibMpv.Format.Flag,
      "time-pos/full" to LibMpv.Format.Int,
      "duration/full" to LibMpv.Format.Int,
    )
    setContent {
      var tracks by remember {
        mutableStateOf(emptyList<LibMpv.Track>())
      }
      val player = remember {
        MpvPlayer(
          configuration = MpvConfiguration()
        ).apply {
          properties.forEach {
            observeProperty(it.first, it.second)
          }
          this.registerEventListener {
            if (it.event == LibMpv.Event.PropertyChange) {
              val data = it.data as? LibMpv.Property ?: return@registerEventListener
              when (data.name) {
                "track-list" -> {
                  val stringValue = data.value as LibMpv.Value.MpvString
                  tracks = parseTracks(stringValue.value).getOrElse { emptyList() }
                }
              }
            }
            println("it = ${it}")
          }
        }
      }

      val aspectRatio by remember {
        derivedStateOf {
          tracks.filterIsInstance<LibMpv.Track.Video>()
            .firstOrNull { it.selected }
            ?.aspectRatio
            ?: 16f.div(9f)
        }
      }

      DisposableEffect(Unit) {
        onDispose { player.release() }
      }

      val callback = remember {
        object : SurfaceHolder.Callback {
          override fun surfaceChanged(holder: SurfaceHolder, format: Int, width: Int, height: Int) {
            player.onSurfaceSizeChanged(width, height)
          }
          override fun surfaceCreated(holder: SurfaceHolder) = player.onSurfaceCreated(holder.surface)
          override fun surfaceDestroyed(holder: SurfaceHolder) = player.onSurfaceDestroyed()
        }
      }
      LaunchedEffect(Unit) {
        delay(1000)
        println("Setting media source")
        withContext(Dispatchers.Main) {
          val url = "https://github.com/ietf-wg-cellar/matroska-test-files/raw/master/test_files/test5.mkv"
          val result = player.setMediaSource(start = 10.seconds, end = 20.seconds, url = url)
          println("result = ${result.name}")
        }
      }
      Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center,
        content = {
          AndroidView(
            modifier = Modifier.aspectRatio(aspectRatio),
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
      )
    }
  }
}
