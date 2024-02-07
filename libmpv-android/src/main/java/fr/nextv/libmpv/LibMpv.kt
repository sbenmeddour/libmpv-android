package fr.nextv.libmpv

object LibMpv {

  internal const val TAG = "LibMpv"

  private var initialized = false

  fun initializeLibMpv() {
    if (initialized) return
    try {
      System.loadLibrary("mpv")
      System.loadLibrary("mpv-android")
      initializeMpvEngine()
    } finally {
      initialized = true
    }
  }

  private external fun initializeMpvEngine()
  private external fun createMpvHandle(): Long

  internal fun createMpvHandleInternal() = createMpvHandle()

  enum class Format(val nativeValue: kotlin.Int) {
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

  enum class Event(val nativeValue: Int) {
    None(0),
    ShutDown(1),
    LogMessage(2),
    GetPropertyReply(3),
    SetPropertyReply(4),
    CommandReply(5),
    StartFile(6),
    EndFile(7),
    FileLoaded(8),
    Idle(11),
    ClientMessage(16),
    VideoReconfig(17),
    AudioReconfig(18),
    Seek(20),
    Restart(21),
    PropertyChange(22),
    QueueOverflow(24),
    EventHook(25);

    internal companion object {
      val cache = entries.associateBy { it.nativeValue }
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

    data class Video(
      override val id: Int,
      override val selected: Boolean,
      override val codec: String,
      override val codecDescription: String,
      override val default: TripleState,
      val height: Int,
      val width: Int,
      val fps: Float,
    ) : Track

    data class Audio(
      override val id: Int,
      override val selected: Boolean,
      override val codec: String,
      override val codecDescription: String,
      override val default: TripleState,
      val name: String,
    ) : Track

    data class Text(
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

  data class PlayerEvent internal constructor(private val eventOrdinal: Int, val data: EventData) {

    val event: Event get() = Event.cache.getOrElse(eventOrdinal) { Event.None }

    override fun toString() = "PlayerEvent(data=$data, event=$event)"

  }

  sealed interface EventData

  data class Property(val name: String, val value: Value) : EventData

  sealed interface Value : EventData {
    data object MpvNone : Value
    @JvmInline value class MpvDouble(val value: Float) : Value
    @JvmInline value class MpvLong(val value: Long) : Value
    @JvmInline value class MpvString(val value: String) : Value
    @JvmInline value class MpvBoolean(val value: Boolean) : Value
  }

}