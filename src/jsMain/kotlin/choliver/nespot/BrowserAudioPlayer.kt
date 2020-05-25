package choliver.nespot

import org.khronos.webgl.Float32Array
import org.khronos.webgl.set

class BrowserAudioPlayer {
  private val ctx = AudioContext()
  private val bufferSize = (AUDIO_BUFFER_LENGTH_SECONDS * ctx.sampleRate).toInt()
  private var nextStartTimeSeconds: Double? = null
  private lateinit var target: AudioBuffer
  private lateinit var samples: Float32Array
  private var iSample = 0

  init {
    replaceBuffer()
  }

  val sampleRateHz = ctx.sampleRate.toInt()

  val sink = object : AudioSink {
    override fun put(sample: Float) {
      samples[iSample++] = sample
      if (iSample == bufferSize) {
        maybeScheduleChunk()
        replaceBuffer()
        iSample = 0
      }
    }
  }

  private fun maybeScheduleChunk() {
    (nextStartTimeSeconds ?: (ctx.currentTime + AUDIO_BUFFER_AHEAD_SECONDS)).let { scheduledTimeSeconds ->
      // Don't schedule in the past (only relevant if we're catching up)
      if (scheduledTimeSeconds > ctx.currentTime) {
        with(ctx.createBufferSource()) {
          buffer = target
          connect(ctx.destination)
          start(scheduledTimeSeconds)
        }
      }
      nextStartTimeSeconds = scheduledTimeSeconds + AUDIO_BUFFER_LENGTH_SECONDS
    }
  }

  private fun replaceBuffer() {
    target = ctx.createBuffer(1, bufferSize, ctx.sampleRate)
    samples = target.getChannelData(0)
  }

  companion object {
    private const val AUDIO_BUFFER_LENGTH_SECONDS = 0.025
    private const val AUDIO_BUFFER_AHEAD_SECONDS = 0.075
  }
}
