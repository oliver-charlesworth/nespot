package choliver.nespot

import org.khronos.webgl.Float32Array
import org.khronos.webgl.set

class BrowserAudioPlayer {
  private val ctx = AudioContext()
  private val bufferSize = (AUDIO_BUFFER_LENGTH_SECONDS * ctx.sampleRate).toInt()

  private var audioBase: Double? = null
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
        scheduleChunk()
        replaceBuffer()
        iSample = 0
      }
    }
  }

  private fun scheduleChunk() {
    with(ctx.createBufferSource()) {
      buffer = target
      connect(ctx.destination)
      (audioBase ?: (ctx.currentTime + AUDIO_BUFFER_AHEAD_SECONDS)).let { base ->
        start(base)
        audioBase = base + AUDIO_BUFFER_LENGTH_SECONDS
      }
    }
  }

  private fun replaceBuffer() {
    target = ctx.createBuffer(1, bufferSize, ctx.sampleRate)
    samples = target.getChannelData(0)
  }

  companion object {
    private const val AUDIO_BUFFER_LENGTH_SECONDS = 0.01
    private const val AUDIO_BUFFER_AHEAD_SECONDS = 0.03
  }
}
