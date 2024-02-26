package com.example.libmpv

import android.view.SurfaceHolder
import android.view.SurfaceView
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsDraggedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import fr.nextv.libmpv.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds

private val properties = listOf(
  "track-list" to LibMpv.Format.String,
  "seeking" to LibMpv.Format.Flag,
  "time-pos/full" to LibMpv.Format.Int,
  "duration/full" to LibMpv.Format.Int,
  "pause" to LibMpv.Format.Flag,
  "idle" to LibMpv.Format.Flag,
)

@Composable
fun PlayerScreen(url: String) {
  val playerState = remember { PlayerState() }
  val player = remember {
    MpvPlayer(
      configuration = MpvConfiguration(
        eventPollRate = 100.milliseconds,
      )
    ).apply {
      properties.forEach {
        observeProperty(it.first, it.second)
      }
      this.registerEventListener { event ->
        when (event) {
          is LibMpv.Event.EndFile -> playerState.isPlayingFile = false
          is LibMpv.Event.StartFile -> playerState.isPlayingFile = true
          is LibMpv.Event.PropertyChange -> {
            when (event.name) {
              "seeking" -> {
                val value = event.value as? LibMpv.Value.MpvBoolean ?: return@registerEventListener
                playerState.isSeeking = value.value
              }

              "pause" -> {
                val value = event.value as? LibMpv.Value.MpvBoolean ?: return@registerEventListener
                playerState.isPaused = value.value
              }

              "idle" -> {
                val value = event.value as? LibMpv.Value.MpvBoolean ?: return@registerEventListener
                playerState.idle = value.value
              }

              "time-pos/full" -> {
                val value = event.value as? LibMpv.Value.MpvLong ?: return@registerEventListener
                playerState.position = value.value.seconds
              }

              "duration/full" -> {
                val value = event.value as? LibMpv.Value.MpvLong ?: return@registerEventListener
                playerState.duration = value.value.seconds
              }

              "track-list" -> {
                val stringValue = event.value as? LibMpv.Value.MpvString ?: return@registerEventListener
                playerState.tracks = parseTracks(stringValue.value).getOrElse { emptyList() }
              }
            }
          }

          else -> return@registerEventListener
        }
      }
    }
  }
  val lifecycle = LocalLifecycleOwner.current
  var showControls by remember { mutableStateOf(true) }

  Box(
    modifier = Modifier
      .fillMaxSize()
      .background(Color.Black)
      .clickable(
        interactionSource = remember { MutableInteractionSource() },
        indication = null,
        onClick = { showControls = !showControls },
      ),
    contentAlignment = Alignment.Center,
    content = {
      PlayerView(player = player, state = playerState)
      AnimatedVisibility(
        visible = showControls,
        enter = fadeIn(),
        exit = fadeOut(),
        content = {
          Box(
            modifier = Modifier
              .fillMaxSize()
              .padding(horizontal = 32.dp, vertical = 24.dp),
            content = {
              Controls(player = player, state = playerState)
            }
          )
        }
      )
    }
  )
  LaunchedEffect(url) {
    withContext(Dispatchers.Main) {
      player.setMediaSource(url = url)
    }
  }
  DisposableEffect(Unit) {
    val observer = LifecycleEventObserver { source, event ->
      when (event) {
        Lifecycle.Event.ON_CREATE -> return@LifecycleEventObserver
        Lifecycle.Event.ON_START -> return@LifecycleEventObserver
        Lifecycle.Event.ON_RESUME -> return@LifecycleEventObserver
        Lifecycle.Event.ON_PAUSE -> {
          player.pause()
        }

        Lifecycle.Event.ON_STOP -> return@LifecycleEventObserver
        Lifecycle.Event.ON_DESTROY -> return@LifecycleEventObserver
        Lifecycle.Event.ON_ANY -> return@LifecycleEventObserver
      }
    }
    lifecycle.lifecycle.addObserver(observer)
    onDispose {
      player.release()
      lifecycle.lifecycle.removeObserver(observer)
    }
  }
}


@Composable
fun BoxScope.Controls(
  modifier: Modifier = Modifier,
  player: MpvPlayer,
  state: PlayerState,
) {
  val interactionSource = remember { MutableInteractionSource() }
  val isUserSliding by interactionSource.collectIsDraggedAsState()
  var userValue by remember { mutableFloatStateOf(0f) }
  val sliderValue: Float by remember {
    derivedStateOf {
      if (isUserSliding) {
        userValue
      } else {
        state.position
          .div(
            other = state.duration.takeUnless { it <= Duration.ZERO } ?: 1.seconds
          )
          .toFloat()
      }

    }
  }
  LaunchedEffect(isUserSliding) {
    if (isUserSliding) {
      userValue = state.position
        .div(
          other = state.duration.takeUnless { it <= Duration.ZERO } ?: 1.seconds
        )
        .toFloat()
    }
  }
  Row(
    modifier = Modifier
      .align(Alignment.Center)
      .fillMaxWidth(),
    horizontalArrangement = Arrangement.SpaceEvenly,
    verticalAlignment = Alignment.CenterVertically,
  ) {
    SimpleIconButton(vector = Icons.Rounded.FastRewind) {
      player.rewindBy(position = 10.seconds, fast = true)
    }
    SimpleIconButton(vector = if (state.isPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow) {
      player.playPause()
    }
    SimpleIconButton(vector = Icons.Rounded.FastForward) {
      player.forwardBy(position = 10.seconds, fast = true)
    }
  }
  Slider(
    modifier = modifier
      .align(Alignment.BottomCenter)
      .fillMaxWidth(),
    interactionSource = interactionSource,
    value = sliderValue,
    onValueChange = {
      userValue = it
    },
    onValueChangeFinished = {
      val target = state.duration.times(userValue.toDouble())
      player.seekTo(position = target, fast = true)
    }
  )
}

@Composable
fun PlayerView(
  modifier: Modifier = Modifier,
  player: MpvPlayer,
  state: PlayerState,
) {
  val aspectRatio by remember {
    derivedStateOf {
      state.tracks
        .filterIsInstance<LibMpv.Track.Video>()
        .firstOrNull { it.selected }
        ?.aspectRatio
        ?: 16f.div(9f)
    }
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
  AndroidView(
    modifier = modifier
      .aspectRatio(aspectRatio),
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

@Composable
fun SimpleIconButton(
  modifier: Modifier = Modifier,
  vector: ImageVector,
  onClick: () -> Unit,
) {
  IconButton(
    modifier = modifier,
    content = {
      Icon(
        modifier = Modifier.size(32.dp),
        imageVector = vector,
        contentDescription = null,
      )
    },
    onClick = onClick,
  )
}
