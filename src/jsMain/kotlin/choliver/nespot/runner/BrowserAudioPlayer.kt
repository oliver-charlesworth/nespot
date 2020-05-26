package choliver.nespot.runner

import AUDIO_BUFFER_AHEAD_SECONDS
import AUDIO_BUFFER_LENGTH_SECONDS
import choliver.nespot.browser.AudioContext
import org.khronos.webgl.Float32Array

class BrowserAudioPlayer {
  private val ctx = AudioContext()
  private var nextStartTimeSeconds: Double? = null
  val sampleRateHz = ctx.sampleRate.toInt()

  fun absorbChunk(samples: Float32Array) {
    (nextStartTimeSeconds ?: (ctx.currentTime + AUDIO_BUFFER_AHEAD_SECONDS)).let { scheduledTimeSeconds ->
      // Don't schedule in the past (only relevant if we're catching up)
      if (scheduledTimeSeconds > ctx.currentTime) {
        val target = ctx.createBuffer(1, samples.length, ctx.sampleRate)
        target.getChannelData(0).set(samples)

        with(ctx.createBufferSource()) {
          buffer = target
          connect(ctx.destination)
          start(scheduledTimeSeconds)
        }
      }
      nextStartTimeSeconds = scheduledTimeSeconds + AUDIO_BUFFER_LENGTH_SECONDS
    }
  }
}
