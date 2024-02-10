package fr.nextv.libmpv

import android.util.Log
import android.view.Surface
import fr.nextv.libmpv.MpvPlayerJni.Companion.attachAndroidSurface
import fr.nextv.libmpv.MpvPlayerJni.Companion.command
import fr.nextv.libmpv.MpvPlayerJni.Companion.detachAndroidSurface
import fr.nextv.libmpv.MpvPlayerJni.Companion.observeProperty
import fr.nextv.libmpv.MpvPlayerJni.Companion.setOption
import fr.nextv.libmpv.MpvPlayerJni.Companion.setProperty
import kotlinx.serialization.json.*
import kotlin.math.*
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

internal inline fun MpvPlayer.ifInitialized(block: MpvPlayer.() -> LibMpv.Result): LibMpv.Result {
  if (this.isReleased) {
    return LibMpv.Result.ErrorUninitialized
  }
  return block.invoke(this)
}

internal inline fun <T> MpvPlayer.ifInitialized(
  block: MpvPlayer.() -> T,
  orElse: () -> T,
): T {
  if (this.isReleased) {
    return orElse.invoke()
  }
  return block.invoke(this)
}

fun MpvPlayer.command(command: String): LibMpv.Result = ifInitialized { nativePlayer.command(command) }
fun MpvPlayer.command(parts: Array<String?>): LibMpv.Result = ifInitialized { nativePlayer.command(parts) }

fun MpvPlayer.play(): LibMpv.Result = ifInitialized { nativePlayer.command("set pause no") }
fun MpvPlayer.pause(): LibMpv.Result = ifInitialized { nativePlayer.command("set pause yes") }
fun MpvPlayer.playPause(): LibMpv.Result = ifInitialized { nativePlayer.command("cycle pause") }
fun MpvPlayer.stop(): LibMpv.Result = ifInitialized { nativePlayer.command("stop") }

fun MpvPlayer.setMediaSource(start: Duration? = null, end: Duration? = null, url: String): LibMpv.Result {
  return ifInitialized {
    nativePlayer.command(
      command = buildString {
        append("loadfile")
        append(" ")
        append(url)
        append(" replace")
        val args = buildList {
          if (start != null && start > Duration.ZERO) {
            this@buildList.add("start=${start.inWholeSeconds}")
          }
          if (end != null && end > Duration.ZERO && (start == null || end > start)) {
            this@buildList.add("end=${end.inWholeSeconds}")
          }
        }
        if (args.isNotEmpty()) {
          append(" ")
          append(args.joinToString(","))
        }
      }
    )
  }
}

fun MpvPlayer.onSurfaceSizeChanged(width: Int, height: Int): LibMpv.Result {
  return ifInitialized {
    nativePlayer.setProperty("android-surface-size", "${width}x${height}")
  }
}

fun MpvPlayer.onSurfaceCreated(surface: Surface) {
  ifInitialized {
    this.onSurfaceDestroyed()
    this.nativePlayer.attachAndroidSurface(surface)
    this.nativePlayer.setOption("force-window", "yes")
    this.nativePlayer.setProperty("vo", configuration.rendering.output.rawValue)
  }
}

fun MpvPlayer.onSurfaceDestroyed() {
  ifInitialized {
    this.nativePlayer.setProperty("vo", "null")
    this.nativePlayer.setOption("force-window", "no")
    this.nativePlayer.detachAndroidSurface()
  }
}

val MpvPlayer.isPlaying: Boolean
  get() = ifInitialized(
    block = { nativePlayer.getPropertyBoolean("pause")?.not() ?: false },
    orElse = { false },
  )

val MpvPlayer.isSeeking: Boolean
  get() = ifInitialized(
    block = { nativePlayer.getPropertyBoolean("seeking")?.not() ?: false },
    orElse = { false },
  )
val MpvPlayer.isSeekable: Boolean
  get() = ifInitialized(
    block = { nativePlayer.getPropertyBoolean("seekable")?.not() ?: false },
    orElse = { false },
  )

val MpvPlayer.playbackPosition: Duration
  get() = ifInitialized(
    block = { nativePlayer.getPropertyInt("time-pos/full")?.seconds ?: Duration.ZERO },
    orElse = { Duration.ZERO },
  )

val MpvPlayer.playbackDuration: Duration
  get() = ifInitialized(
    block = { nativePlayer.getPropertyInt("duration/full")?.seconds ?: Duration.ZERO },
    orElse = { Duration.ZERO },
  )

fun MpvPlayer.setOption(key: String, value: String): LibMpv.Result = ifInitialized { nativePlayer.setOption(key, value) }
fun MpvPlayer.setProperty(key: String, value: String): LibMpv.Result = ifInitialized { nativePlayer.setProperty(key, value) }
fun MpvPlayer.setProperty(key: String, value: Int): LibMpv.Result = ifInitialized { nativePlayer.setProperty(key, value) }
fun MpvPlayer.setProperty(key: String, value: Double): LibMpv.Result = ifInitialized { nativePlayer.setProperty(key, value) }
fun MpvPlayer.setProperty(key: String, value: Boolean): LibMpv.Result = ifInitialized { nativePlayer.setProperty(key, value) }

fun MpvPlayer.observeProperty(key: String, format: LibMpv.Format): LibMpv.Result = ifInitialized { this.nativePlayer.observeProperty(key, format) }

fun Boolean?.asTripleState(): LibMpv.TripleState = when (this) {
  true -> LibMpv.TripleState.Yes
  false -> LibMpv.TripleState.No
  null -> LibMpv.TripleState.Unknown
}

val LibMpv.Track.Video.aspectRatio: Float?
  get() {
    val largest = max(width, height)
    val smallest = min(width, height)
    if (smallest <= 0) {
      return null
    }
    return largest.toFloat() / smallest.toFloat()
  }

val MpvPlayer.tracks: List<LibMpv.Track>
  get() = ifInitialized(
    block = {
      nativePlayer.getPropertyString("track-list")
        .orEmpty()
        .let(::parseTracks)
        .getOrElse { emptyList() }
    },
    orElse = { emptyList() },
  )

fun parseTracks(tracks: String): Result<List<LibMpv.Track>> = runCatching {
  if (tracks.isBlank()) {
    return@runCatching emptyList<LibMpv.Track>()
  }
  Json.parseToJsonElement(tracks)
    .jsonArray
    .filterIsInstance<JsonObject>()
    .mapNotNull { jsonObject ->
      try {
        val type = jsonObject["type"]!!.jsonPrimitive.content
        val id = jsonObject["id"]!!.jsonPrimitive.int
        val selected = jsonObject["selected"]!!.jsonPrimitive.booleanOrNull ?: false
        val codec = jsonObject["codec"]?.jsonPrimitive?.contentOrNull ?: "Unknown"
        val name = jsonObject["title"]?.jsonPrimitive?.contentOrNull ?: "Unknown"
        val default = jsonObject["default"]?.jsonPrimitive?.booleanOrNull.asTripleState()
        val codecDescription = jsonObject["decoder-desc"]?.jsonPrimitive?.contentOrNull.orEmpty()
        when (type) {
          "video" -> {
            LibMpv.Track.Video(
              id = id,
              selected = selected,
              codec = codec,
              codecDescription = codecDescription,
              default = default,
              height = jsonObject["demux-h"]?.jsonPrimitive?.int ?: -1,
              width = jsonObject["demux-w"]?.jsonPrimitive?.int ?: -1,
              fps = jsonObject["demux-fps"]?.jsonPrimitive?.float ?: -1f,
            )
          }

          "audio" -> {
            LibMpv.Track.Audio(
              id = id,
              selected = selected,
              codec = codec,
              codecDescription = codecDescription,
              default = default,
              name = name,
              channelCount = runCatching {
                jsonObject["audio-channels"]!!.jsonPrimitive.int
              }.recoverCatching {
                jsonObject["demux-channel-count"]!!.jsonPrimitive.int
              }.getOrElse {
                0
              },
              channelLayout = jsonObject["demux-channels"]?.jsonPrimitive?.contentOrNull,
            )
          }

          "sub" -> {
            LibMpv.Track.Text(
              id = id,
              selected = selected,
              codec = codec,
              codecDescription = codecDescription,
              default = default,
              forced = jsonObject["forced"]?.jsonPrimitive?.booleanOrNull.asTripleState(),
              name = name,
              lang = jsonObject["lang"]?.jsonPrimitive?.contentOrNull.orEmpty(),
            )
          }

          else -> throw IllegalArgumentException("Unknown track type: $type")
        }
      } catch (error: Throwable) {
        Log.e(LibMpv.TAG, "Failed to parse track [$jsonObject]", error)
        return@mapNotNull null
      }
    }
}

fun MpvPlayer.setVideoTrack(track: LibMpv.Track.Video?): LibMpv.Result = command("set vid ${track?.id ?: "no"}")
fun MpvPlayer.setAudioTrack(track: LibMpv.Track.Audio?): LibMpv.Result = command("set aid ${track?.id ?: "no"}")
fun MpvPlayer.setSubtitlesTrack(track: LibMpv.Track.Text?): LibMpv.Result = command("set sid ${track?.id ?: "no"}")

fun MpvPlayer.seekTo(position: Duration, fast: Boolean): LibMpv.Result = command(
  command = buildString {
    append("seek ${position.inWholeSeconds.absoluteValue} absolute")
    if (fast) {
      append("+keyframes")
    } else {
      append("+exact")
    }
  }
)

fun MpvPlayer.forwardBy(position: Duration, fast: Boolean): LibMpv.Result = command(
  command = buildString {
    append("seek ${position.inWholeSeconds.absoluteValue} relative")
    if (fast) {
      append("+keyframes")
    } else {
      append("+exact")
    }
  }
)

fun MpvPlayer.rewindBy(position: Duration, fast: Boolean): LibMpv.Result = command(
  command = buildString {
    append("seek -${position.inWholeSeconds.absoluteValue} relative")
    if (fast) {
      append("+keyframes")
    } else {
      append("+exact")
    }
  }
)