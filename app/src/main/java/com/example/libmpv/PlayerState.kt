package com.example.libmpv

import androidx.compose.runtime.*
import fr.nextv.libmpv.LibMpv
import kotlin.time.Duration

@Stable
class PlayerState {
  var duration by mutableStateOf(Duration.ZERO)
  var position by mutableStateOf(Duration.ZERO)
  var isSeeking by mutableStateOf(false)

  var isPaused by mutableStateOf(false)
  var idle by mutableStateOf(false)

  var tracks by mutableStateOf(emptyList<LibMpv.Track>())

  val isPlaying by derivedStateOf { !isPaused && isPlayingFile }

  var isPlayingFile by mutableStateOf(false)

}