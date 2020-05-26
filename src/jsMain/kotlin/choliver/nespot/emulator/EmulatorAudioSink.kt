package choliver.nespot.emulator

import AUDIO_BUFFER_LENGTH_SECONDS
import choliver.nespot.AudioSink
import choliver.nespot.MSG_AUDIO_CHUNK
import org.khronos.webgl.Float32Array
import org.khronos.webgl.set

class EmulatorAudioSink(sampleRateHz: Int) : AudioSink {
  private val bufferSize = (AUDIO_BUFFER_LENGTH_SECONDS * sampleRateHz).toInt()
  private lateinit var raw: Float32Array
  private var i = 0

  init {
    reset()
  }

  override fun put(sample: Float) {
    raw[i++] = sample

    if (i == bufferSize) {
      postAudioChunk()
      reset()
      i = 0
    }
  }

  private fun postAudioChunk() {
    self.postMessage(arrayOf(MSG_AUDIO_CHUNK, raw.buffer), transfer = arrayOf(raw.buffer))
  }

  private fun reset() {
    i = 0
    raw = Float32Array(bufferSize)
  }
}
