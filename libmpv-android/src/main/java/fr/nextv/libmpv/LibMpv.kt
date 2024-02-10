package fr.nextv.libmpv

import androidx.annotation.Keep

@Keep
object LibMpv {

  internal const val TAG = "mpv-android"

  private var initialized = false

  fun initializeLibMpv() {
    if (initialized) return
    try {
      System.loadLibrary("mpv")
      System.loadLibrary("mpv-android")
    } finally {
      initialized = true
    }
  }

  private external fun createMpvHandle(): Long

  internal fun createMpvHandleInternal() = createMpvHandle()

  enum class Format(internal val nativeValue: kotlin.Int) {
    None(0),
    String(1),
    OsdString(2),
    Flag(3),
    Int(4),
    Double(5),
    Node(6),
    NodeArray(7),
    NodeMap(8),
    ByteArray(9),
  }

  enum class EndFileReason(internal val nativeValue: Int) {
    EndOfFile(0),
    Stop(2),
    Quit(3),
    Error(4),
    Redirect(5),
  }


  sealed interface Event {

    data class PropertyChange private constructor(val name: String, val value: Value) : Event
    data class LogMessage private constructor(val level: Int, val prefix: String, val text: String) : Event
    data class StartFile private constructor(val playlistEntryId: Int) : Event
    data class EndFile private constructor(private val nativeReason: Int, private val nativeError: Int, val entryId: Int, val insertId: Int) : Event {

      val reason: EndFileReason
        get() = EndFileReason.entries
          .firstOrNull { it.nativeValue == nativeReason }
          ?: EndFileReason.EndOfFile

      val error: Result
        get() = Result.cache[nativeError]


    }

    enum class SimpleEvent : Event {

      CommandReply,
      GetPropertyReply,
      SetPropertyReply,
      None,
      Shutdown,
      FileLoaded,
      Idle,
      Tick,
      Seek,
      VideoReconfig,
      AudioReconfig,
      PlaybackRestart,
      QueueOverflow,
      ClientMessage,
      Hook;

      companion object {

        @JvmStatic
        val Event.nativeCode: Int
          get() = when (this) {
            None -> 0
            Shutdown -> 1
            is LogMessage -> 2
            GetPropertyReply -> 3
            SetPropertyReply -> 4
            CommandReply -> 5
            is EndFile -> 7
            is StartFile -> 6
            FileLoaded -> 8
            Idle -> 11
            Tick -> 14
            ClientMessage -> 16
            VideoReconfig -> 17
            AudioReconfig -> 18
            Seek -> 20
            PlaybackRestart -> 21
            is PropertyChange -> 22
            QueueOverflow -> 24
            Hook -> 25
          }

        @JvmStatic
        private val cache = SimpleEvent.entries.associateBy { it.nativeCode }

        @JvmStatic
        private fun fromNativeCode(value: Int): SimpleEvent {
          return cache[value] ?: None
        }
      }

    }
  }

  enum class LogLevel(val nativeValue: Int) {
    None(0),
    Fatal(10),
    Error(20),
    Warn(30),
    Info(40),
    Verbose(50),
    Debug(60),
    Trace(70),
  }

  enum class Result(internal val nativeCode: Int) {
    Success(0),
    ErrorEventQueueFull(-1),
    ErrorNoMem(-2),
    ErrorUninitialized(-3),
    ErrorInvalidParameter(-4),
    ErrorOptionNotFound(-5),
    ErrorOptionFormat(-6),
    ErrorOptionError(-7),
    ErrorPropertyNotFound(-8),
    ErrorPropertyFormat(-9),
    ErrorPropertyUnavailable(-10),
    ErrorPropertyError(-11),
    ErrorCommand(-12),
    ErrorLoadingFailed(-13),
    ErrorAoInitFailed(-14),
    ErrorVoInitFailed(-15),
    ErrorNothingToPlay(-16),
    ErrorUnknownFormat(-17),
    ErrorUnsupported(-18),
    ErrorNotImplemented(-19),
    ErrorGeneric(-20);

    interface Cache {
      operator fun get(nativeCode: Int): Result
    }

    companion object {
      val cache = object : Cache {
        private val _cache = entries.associateBy { it.nativeCode }
        override fun get(nativeCode: Int) = _cache.getOrElse(nativeCode) { ErrorGeneric }
      }
    }

  }

  enum class TripleState { Yes, No, Unknown }

  sealed interface Track {

    val id: Int
    val selected: Boolean
    val codec: String
    val codecDescription: String
    val default: TripleState

    data class Video internal constructor(
      override val id: Int,
      override val selected: Boolean,
      override val codec: String,
      override val codecDescription: String,
      override val default: TripleState,
      val height: Int,
      val width: Int,
      val fps: Float,
    ) : Track

    data class Audio internal constructor(
      override val id: Int,
      override val selected: Boolean,
      override val codec: String,
      override val codecDescription: String,
      override val default: TripleState,
      val name: String,
      val channelCount: Int,
      val channelLayout: String?,
    ) : Track

    data class Text internal constructor(
      override val id: Int,
      override val selected: Boolean,
      override val codec: String,
      override val codecDescription: String,
      override val default: TripleState,
      val forced: TripleState,
      val name: String,
      val lang: String?,
    ) : Track

  }

  sealed interface Value {
    data object MpvNone : Value
    @JvmInline
    value class MpvDouble private constructor(val value: Float) : Value
    @JvmInline
    value class MpvLong private constructor(val value: Long) : Value
    @JvmInline
    value class MpvString private constructor(val value: String) : Value
    @JvmInline
    value class MpvBoolean private constructor(val value: Boolean) : Value
  }

}