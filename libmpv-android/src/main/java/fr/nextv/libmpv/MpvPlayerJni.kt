package fr.nextv.libmpv

import android.view.Surface
import androidx.annotation.Keep

@Keep
internal class MpvPlayerJni {

  private var currentSurface: Long = 0
  private val handle = LibMpv.createMpvHandleInternal()

  external fun initialize()

  external fun setOptionString(key: String, value: String)

  external fun setPropertyString(key: String, value: String)
  external fun setPropertyDouble(key: String, value: Double)
  external fun setPropertyBoolean(key: String, value: Boolean)
  external fun setPropertyInt(key: String, value: Int)

  external fun getPropertyString(key: String): String?
  external fun getPropertyDouble(key: String): Double?
  external fun getPropertyBoolean(key: String): Boolean?
  external fun getPropertyInt(key: String): Int?

  external fun observeProperty(key: String, format: Int)
  external fun unObserveProperties()

  external fun sendCommandString(jString: String)
  external fun sendCommand(command: Array<String?>)

  external fun attachSurface(javaSurface: Surface)
  external fun detachSurface()

  external fun destroy()

  external fun awaitNextEvent(): LibMpv.PlayerEvent

}


