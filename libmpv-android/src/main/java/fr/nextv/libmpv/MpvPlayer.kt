package fr.nextv.libmpv

import fr.nextv.libmpv.MpvPlayerJni.Companion.destroyNativePlayer
import fr.nextv.libmpv.MpvPlayerJni.Companion.initializeNativePlayer
import fr.nextv.libmpv.MpvPlayerJni.Companion.setOption
import fr.nextv.libmpv.MpvPlayerJni.Companion.stopObserveProperties
import kotlinx.coroutines.CoroutineName
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.concurrent.Executors

public class MpvPlayer(public val configuration: MpvConfiguration) {

  private val eventThread = Executors.newFixedThreadPool(1).asCoroutineDispatcher()
  private val scope = CoroutineScope(context = eventThread + CoroutineName("MPV EVENT POLLING SCOPE"))

  internal val nativePlayer = MpvPlayerJni()

  public fun interface EventListener {
    fun onEvent(event: LibMpv.PlayerEvent)
  }

  private val listeners = hashSetOf<EventListener>()

  init {
    with (nativePlayer) {
      setOption("idle", "yes")
      setOption("force-window", "no")
      setOption("vo", "null")
      setOption("gpu-context", configuration.gpuContext.rawValue)
      setOption("gpu-api", configuration.gpuApi.rawValue)
      setOption("hwdec", configuration.rendering.decoding.rawValue)
      setOption("ao", configuration.audioOutputs.joinToString(","))
      setOption("demuxer-max-bytes", configuration.demuxerMaxMb.times(1024).times(1024).toString())
      setOption("demuxer-max-back-bytes", configuration.demuxerBackMaxMb.times(1024).times(1024).toString())
      for (option in configuration.otherOptions) {
        setOption(option.key, option.value)
      }
      if (configuration.hardwareCodecsWhiteList.isNotEmpty()) {
        setOption("hwdec-codecs", configuration.hardwareCodecsWhiteList.joinToString(","))
      }
      initializeNativePlayer()
    }
  }

  public fun release() {
    eventPollTask.cancel()
    scope.cancel()
    eventThread.close()
    listeners.clear()
    onSurfaceDestroyed()
    nativePlayer.destroyNativePlayer()
  }

  public fun registerEventListener(listener: EventListener) = listeners.add(listener)

  public fun unregisterEventListener(listener: EventListener) = listeners.remove(listener)

  private fun onEvent(event: LibMpv.PlayerEvent) {
    for (listener in listeners) {
      listener.onEvent(event)
    }
  }

  private val eventPollTask = scope.launch {
    try {
      while (isActive) {
        try {
          val playerEvent = try { nativePlayer.awaitNextEvent() } catch (error: Throwable) {
            continue
          }
          if (playerEvent.event == LibMpv.Event.None) {
            continue
          }
          withContext(Dispatchers.Main) {
            this@MpvPlayer.onEvent(playerEvent)
          }
        } finally {
          delay(200)
        }
      }
    } finally {
      nativePlayer.stopObserveProperties()
    }
  }

}

