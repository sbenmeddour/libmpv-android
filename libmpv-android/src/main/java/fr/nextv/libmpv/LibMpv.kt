package fr.nextv.libmpv

object LibMpv {

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
    ClientMessage(16),
    VideoReconfig(17),
    AudioReconfig(18),
    Seek(20),
    Restart(21),
    PropertyChange(22),
    QueueOverflow(24),
    EventHook(25),
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

  enum class TripleState { Yes, No, Unknown }

  sealed interface Track {

    val id: String
    val selected: Boolean
    val codec: String
    val codecDescription: String
    val default: TripleState

    data class Video(
      override val id: String,
      override val selected: Boolean,
      override val codec: String,
      override val codecDescription: String,
      override val default: TripleState,
      val height: Int,
      val width: Int,
      val fps: Float,
    ) : Track

    data class Audio(
      override val id: String,
      override val selected: Boolean,
      override val codec: String,
      override val codecDescription: String,
      override val default: TripleState,
      val name: String,
    ) : Track

    data class Text(
      override val id: String,
      override val selected: Boolean,
      override val codec: String,
      override val codecDescription: String,
      override val default: TripleState,
      val forced: TripleState,
      val name: String,
    ) : Track

  }

  data class PlayerEvent(val event: Event, val data: EventData)

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