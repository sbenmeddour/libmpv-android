package fr.nextv.libmpv

import android.view.Surface
import androidx.annotation.Keep

@Keep
internal class MpvPlayerJni {

  private var currentSurface: Long = 0
  private val handle = LibMpv.createMpvHandleInternal()

  private external fun initialize(): Int

  private external fun setOptionString(key: String, value: String): Int

  private external fun setPropertyString(key: String, value: String): Int
  private external fun setPropertyDouble(key: String, value: Double): Int
  private external fun setPropertyBoolean(key: String, value: Boolean): Int
  private external fun setPropertyInt(key: String, value: Int): Int

  external fun getPropertyString(key: String): String?
  external fun getPropertyDouble(key: String): Double?
  external fun getPropertyBoolean(key: String): Boolean?
  external fun getPropertyInt(key: String): Int?

  private external fun observeProperty(key: String, format: Int): Int
  private external fun unObserveProperties(): Int

  private external fun sendCommandString(jString: String): Int
  private external fun sendCommand(command: Array<String?>): Int

  private external fun attachSurface(javaSurface: Surface): Int
  private external fun detachSurface(): Int

  external fun destroy()

  external fun awaitNextEvent(): LibMpv.Event

  external fun cancelCurrentAwaitNextEvent()

  companion object {

    internal fun MpvPlayerJni.initializeNativePlayer() = LibMpv.Result.cache[initialize()]

    internal fun MpvPlayerJni.setOption(key: String, value: String) = LibMpv.Result.cache[setOptionString(key, value)]

    internal fun MpvPlayerJni.setProperty(key: String, value: String) = LibMpv.Result.cache[setPropertyString(key, value)]
    internal fun MpvPlayerJni.setProperty(key: String, value: Int) = LibMpv.Result.cache[setPropertyInt(key, value)]
    internal fun MpvPlayerJni.setProperty(key: String, value: Double) = LibMpv.Result.cache[setPropertyDouble(key, value)]
    internal fun MpvPlayerJni.setProperty(key: String, value: Boolean) = LibMpv.Result.cache[setPropertyBoolean(key, value)]

    internal fun MpvPlayerJni.command(command: String) = LibMpv.Result.cache[sendCommandString(command)]
    internal fun MpvPlayerJni.command(parts: Array<String?>) = LibMpv.Result.cache[sendCommand(parts)]

    internal fun MpvPlayerJni.attachAndroidSurface(surface: Surface) = LibMpv.Result.cache[attachSurface(surface)]
    internal fun MpvPlayerJni.detachAndroidSurface() = LibMpv.Result.cache[detachSurface()]

    internal fun MpvPlayerJni.observeProperty(name: String, format: LibMpv.Format) = LibMpv.Result.cache[observeProperty(name, format.nativeValue)]
    internal fun MpvPlayerJni.stopObserveProperties() = LibMpv.Result.cache[unObserveProperties()]

  }

}
