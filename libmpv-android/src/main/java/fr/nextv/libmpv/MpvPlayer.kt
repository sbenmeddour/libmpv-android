package fr.nextv.libmpv

import android.util.Log
import fr.nextv.libmpv.MpvPlayerJni.Companion.initializeNativePlayer
import fr.nextv.libmpv.MpvPlayerJni.Companion.setOption
import fr.nextv.libmpv.MpvPlayerJni.Companion.stopObserveProperties
import kotlinx.coroutines.CoroutineName
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.cancel
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import java.util.concurrent.Executors
import kotlin.math.log

class MpvPlayer(val configuration: MpvConfiguration) {

  private val eventThread = Executors.newFixedThreadPool(4).asCoroutineDispatcher()
  private val scope = CoroutineScope(context = CoroutineName("Mpv player poll scope(${configuration.playerName})"))

  internal val nativePlayer = MpvPlayerJni()

  fun interface EventListener {
    fun onEvent(event: LibMpv.Event)
  }

  private val listeners = hashSetOf<EventListener>()

  internal var isReleased = false

  init {
    with (nativePlayer) {
      setOption("idle", "yes")
      setOption("force-window", "no")
      setOption("vo", "null")
      setOption("gpu-context", configuration.gpuContext.rawValue)
      setOption("gpu-api", configuration.gpuApi.rawValue)
      setOption("hwdec", configuration.rendering.decoding.rawValue)
      setOption("ao", configuration.audioOutputs.map(MpvConfiguration.AudioOutput::rawValue).joinToString(separator = ","))
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

  fun release() {
    eventPollTask.cancel()
    eventThread.cancel()
    scope.cancel()
  }

  fun registerEventListener(listener: EventListener) {
    if (isReleased) {
      return
    }
    listeners.add(listener)
  }

  fun unregisterEventListener(listener: EventListener) {
    if (isReleased) {
      return
    }
    listeners.remove(listener)
  }

  private fun onEvent(event: LibMpv.Event) {
    if (isReleased) {
      return
    }
    for (listener in listeners) {
      listener.onEvent(event)
    }
  }

  private val threadName = CoroutineName("Mpv player poll thread (${configuration.playerName})")

  private suspend fun MpvPlayerJni.awaitNextEventSuspend(): LibMpv.Event {
    return suspendCancellableCoroutine { continuation ->
      continuation.invokeOnCancellation {
        nativePlayer.cancelCurrentAwaitNextEvent()
      }
      val result = runCatching { this.awaitNextEvent() }
      continuation.resumeWith(result)
    }
  }

  private val eventPollTask = scope.launch(eventThread + threadName) {
    while (isActive) {
      try {
        val playerEvent = try { nativePlayer.awaitNextEventSuspend() } catch (error: Throwable) {
          Log.e(LibMpv.TAG, "MpvPlayer.awaitNextEventSuspend failed", error)
          continue
        }
        if (playerEvent == LibMpv.Event.SimpleEvent.None) {
          continue
        }
        withContext(Dispatchers.Main) {
          this@MpvPlayer.onEvent(playerEvent)
        }
      } finally {
        if (isActive) {
          delay(configuration.eventPollRate)
        }
      }
    }
  }

  init {
    eventPollTask.invokeOnCompletion {
      listeners.clear()
      nativePlayer.stopObserveProperties()
      onSurfaceDestroyed()
      isReleased = true
      nativePlayer.destroy()
    }
  }

}

