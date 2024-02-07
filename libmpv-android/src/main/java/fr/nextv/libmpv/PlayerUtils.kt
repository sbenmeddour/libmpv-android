package fr.nextv.libmpv

import android.util.Log
import android.view.Surface
import fr.nextv.libmpv.MpvPlayerJni.Companion.attachAndroidSurface
import fr.nextv.libmpv.MpvPlayerJni.Companion.command
import fr.nextv.libmpv.MpvPlayerJni.Companion.detachAndroidSurface
import fr.nextv.libmpv.MpvPlayerJni.Companion.observeProperty
import fr.nextv.libmpv.MpvPlayerJni.Companion.setOption
import fr.nextv.libmpv.MpvPlayerJni.Companion.setProperty
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.float
import kotlinx.serialization.json.int
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonPrimitive
import kotlin.math.max
import kotlin.math.min
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

public fun MpvPlayer.command(command: String) = nativePlayer.command(command)
public fun MpvPlayer.command(parts: Array<String?>) = nativePlayer.command(parts)

public fun MpvPlayer.play() = nativePlayer.command("set pause no")
public fun MpvPlayer.pause() = nativePlayer.command("set pause yes")
public fun MpvPlayer.playPause() = nativePlayer.command("cycle pause")
public fun MpvPlayer.stop() = nativePlayer.command("stop")

public fun MpvPlayer.setMediaSource(start: Duration, end: Duration, url: String): LibMpv.Result {
  return nativePlayer.command(
    command = buildString {
      append("loadfile")
      append(" ")
      append(url)
      append(" replace")
      val args = buildList {
        if (start > Duration.ZERO) {
          this@buildList.add("start=${start.inWholeSeconds}")
        }
        if (end > Duration.ZERO && end > start) {
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

public fun MpvPlayer.onSurfaceSizeChanged(width: Int, height: Int): LibMpv.Result {
  return this.nativePlayer.setProperty("android-surface-size", "${width}x${height}")
}

public fun MpvPlayer.onSurfaceCreated(surface: Surface) {
  this.onSurfaceDestroyed()
  this.nativePlayer.attachAndroidSurface(surface)
  this.nativePlayer.setOption("force-window", "yes")
  this.nativePlayer.setProperty("vo", configuration.rendering.output.rawValue)
}

public fun MpvPlayer.onSurfaceDestroyed() {
  this.nativePlayer.setProperty("vo", "null")
  this.nativePlayer.setOption("force-window", "no")
  this.nativePlayer.detachAndroidSurface()
}

public val MpvPlayer.isPlaying: Boolean
  get() = nativePlayer.getPropertyBoolean("pause")?.not() ?: false

public val MpvPlayer.isSeeking: Boolean
  get() = nativePlayer.getPropertyBoolean("seeking") ?: false

public val MpvPlayer.isSeekable: Boolean
  get() = nativePlayer.getPropertyBoolean("seekable") ?: false

public val MpvPlayer.playbackPosition: Duration
  get() = nativePlayer.getPropertyInt("time-pos")?.seconds ?: Duration.ZERO

public val MpvPlayer.playbackDuration: Duration
  get() = nativePlayer.getPropertyInt("duration")?.seconds ?: Duration.ZERO

fun MpvPlayer.setOption(key: String, value: String) = nativePlayer.setOption(key, value)
fun MpvPlayer.setProperty(key: String, value: String) = nativePlayer.setProperty(key, value)
fun MpvPlayer.setProperty(key: String, value: Int) = nativePlayer.setProperty(key, value)
fun MpvPlayer.setProperty(key: String, value: Double) = nativePlayer.setProperty(key, value)
fun MpvPlayer.setProperty(key: String, value: Boolean) = nativePlayer.setProperty(key, value)

public fun MpvPlayer.observeProperty(key: String, format: LibMpv.Format) = this.nativePlayer.observeProperty(key, format)

public fun Boolean?.asTripleState(): LibMpv.TripleState = when (this) {
  true -> LibMpv.TripleState.Yes
  false -> LibMpv.TripleState.No
  null -> LibMpv.TripleState.Unknown
}

public val LibMpv.Track.Video.aspectRatio: Float?
  get() {
    val largest = max(width, height)
    val smallest = min(width, height)
    if (smallest <= 0) {
      return null
    }
    return largest.toFloat() / smallest.toFloat()
  }

public val MpvPlayer.tracks: List<LibMpv.Track>
  get() = nativePlayer.getPropertyString("track-list")
    .orEmpty()
    .let(::parseTracks)
    .getOrElse { emptyList() }

public fun parseTracks(tracks: String): Result<List<LibMpv.Track>> = runCatching {
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
        val codecDescription = jsonObject["codec-description"]?.jsonPrimitive?.contentOrNull.orEmpty()
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

public fun MpvPlayer.setVideoTrack(track: LibMpv.Track.Video?) = nativePlayer.command("set vid ${track?.id ?: "no"}")
public fun MpvPlayer.setAudioTrack(track: LibMpv.Track.Audio?) = nativePlayer.command("set aid ${track?.id ?: "no"}")
public fun MpvPlayer.setSubtitlesTrack(track: LibMpv.Track.Text?) = nativePlayer.command("set sid ${track?.id ?: "no"}")

public fun MpvPlayer.seekTo(position: Duration, fast: Boolean) = nativePlayer.command(
  command = buildString {
    append("seek ${position.inWholeSeconds} absolute")
    if (fast) {
      append("+keyframe")
    } else {
      append("+exact")
    }
  }
)

public fun MpvPlayer.forwardBy(position: Duration, fast: Boolean) = nativePlayer.command(
  command = buildString {
    append("seek ${position.inWholeSeconds} relative")
    if (fast) {
      append("+keyframe")
    } else {
      append("+exact")
    }
  }
)

public fun MpvPlayer.rewindBy(position: Duration, fast: Boolean) = nativePlayer.command(
  command = buildString {
    append("seek -${position.inWholeSeconds} relative")
    if (fast) {
      append("+keyframe")
    } else {
      append("+exact")
    }
  }
)