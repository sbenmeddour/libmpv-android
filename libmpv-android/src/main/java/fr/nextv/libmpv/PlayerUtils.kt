package fr.nextv.libmpv

import android.view.Surface
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

public fun MpvPlayer.play() = nativePlayer.sendCommandString("set pause no") //nativePlayer.sendCommand(arrayOf("set", "pause", "no"))
public fun MpvPlayer.pause() = nativePlayer.sendCommandString("set pause yes") // nativePlayer.sendCommand(arrayOf("set", "pause", "yes"))
public fun MpvPlayer.playPause() = nativePlayer.sendCommandString("cycle pause") //nativePlayer.sendCommand(arrayOf("cycle", "pause"))
public fun MpvPlayer.stop() = nativePlayer.sendCommandString("stop")

public fun MpvPlayer.setMediaSource(start: Duration, end: Duration, url: String) {
  nativePlayer.sendCommand(
    command = buildList {
      add("loadfile")
      add(url)
      if (start > Duration.ZERO) {
        add("--start")
        add(start.inWholeMilliseconds.toString())
      }
      if (end > Duration.ZERO && end > start) {
        add("--end")
        add(end.inWholeMilliseconds.toString())
      }
    }.toTypedArray()
  )
}

public fun MpvPlayer.onSurfaceSizeChanged(width: Int, height: Int) {
  this.nativePlayer.setPropertyString("android-surface-size", "${width}x${height}")
}

public fun MpvPlayer.onSurfaceCreated(surface: Surface) {
  this.onSurfaceDestroyed()
  this.nativePlayer.attachSurface(surface)
  this.nativePlayer.setOptionString("force-window", "yes")
  this.nativePlayer.setPropertyString("vo", configuration.rendering.output.rawValue)
}

public fun MpvPlayer.onSurfaceDestroyed() {
  this.nativePlayer.setPropertyString("vo", "null")
  this.nativePlayer.setOptionString("force-window", "no")
  this.nativePlayer.detachSurface()
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

public fun MpvPlayer.setProperty(property: LibMpv.Property) = when (property.value) {
  is LibMpv.Value.MpvBoolean -> nativePlayer.setPropertyBoolean(property.name, property.value.value!!)
  is LibMpv.Value.MpvDouble -> nativePlayer.setPropertyDouble(property.name, property.value.value!!)
  is LibMpv.Value.MpvInt -> nativePlayer.setPropertyInt(property.name, property.value.value!!)
  LibMpv.Value.MpvNone -> error("Invalid argument")
  is LibMpv.Value.MpvString -> nativePlayer.setPropertyString(property.name, property.value.value!!)
}

public fun MpvPlayer.observeProperty(key: String, format: LibMpv.Format) = this.nativePlayer.observeProperty(key, format.nativeValue)
