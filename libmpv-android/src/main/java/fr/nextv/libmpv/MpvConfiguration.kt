package fr.nextv.libmpv

import java.util.UUID
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds

class MpvConfiguration(
  val playerName: String = UUID.randomUUID().toString(),
  val rendering: Rendering = VideoOutput.MediaCodecEmbed + HardwareDecoding.MediaCodec,
  val gpuContext: GpuContext = GpuContext.Android,
  val gpuApi: GpuApi = GpuApi.Auto,
  val audioOutputs: List<AudioOutput> = AudioOutput.entries.toList(),
  val demuxerMaxMb: Int = 24,
  val demuxerBackMaxMb: Int = 8,
  val otherOptions: Map<String, String> = emptyMap(),
  val hardwareCodecsWhiteList: List<String> = emptyList(),
  val eventPollRate: Duration = 200.milliseconds,
) {

  class Rendering internal constructor(val output: VideoOutput, val decoding: HardwareDecoding)

  enum class VideoOutput { MediaCodecEmbed, Gpu, GpuNext }
  enum class HardwareDecoding { MediaCodec, MediacodecCopy, Auto, Software }
  enum class GpuContext { Android, Auto }
  enum class GpuApi { Auto, OpenGl, Vulkan }
  enum class AudioOutput { AudioTrack, OpenSles }

}

internal val MpvConfiguration.VideoOutput.rawValue: String
  get() = when (this) {
    MpvConfiguration.VideoOutput.MediaCodecEmbed -> "mediacodec_embed"
    MpvConfiguration.VideoOutput.Gpu -> "gpu"
    MpvConfiguration.VideoOutput.GpuNext -> "gpu-next"
  }

internal val MpvConfiguration.HardwareDecoding.rawValue: String
  get() = when (this) {
    MpvConfiguration.HardwareDecoding.MediaCodec -> "mediacodec"
    MpvConfiguration.HardwareDecoding.MediacodecCopy -> "mediacodec-copy"
    MpvConfiguration.HardwareDecoding.Auto -> "auto"
    MpvConfiguration.HardwareDecoding.Software -> "no"
  }

internal val MpvConfiguration.AudioOutput.rawValue: String
  get() = when (this) {
    MpvConfiguration.AudioOutput.AudioTrack -> "ao"
    MpvConfiguration.AudioOutput.OpenSles -> "opensles"
  }

internal val MpvConfiguration.GpuContext.rawValue: String
  get() = when (this) {
    MpvConfiguration.GpuContext.Android -> "android"
    MpvConfiguration.GpuContext.Auto -> "auto"
  }

internal val MpvConfiguration.GpuApi.rawValue: String
  get() = when (this) {
    MpvConfiguration.GpuApi.Auto -> "auto"
    MpvConfiguration.GpuApi.OpenGl -> "opengl"
    MpvConfiguration.GpuApi.Vulkan -> "vulkan"
  }

operator fun MpvConfiguration.VideoOutput.plus(other: MpvConfiguration.HardwareDecoding): MpvConfiguration.Rendering {
  return MpvConfiguration.Rendering(this, other)
}