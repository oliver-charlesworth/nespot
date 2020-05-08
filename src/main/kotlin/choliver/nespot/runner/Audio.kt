package choliver.nespot.runner

import choliver.nespot.SAMPLE_RATE_HZ
import javax.sound.sampled.AudioFormat
import javax.sound.sampled.AudioSystem
import kotlin.math.roundToInt

class Audio {
  private var iFrame = 0
  private var dc = 0f
  private val bufferSize = (SAMPLE_RATE_HZ * BUFFER_LENGTH_MS) / 1000
  private val audioFormat = AudioFormat(SAMPLE_RATE_HZ.toFloat(), 16, 1, true, false)
  private val soundLine = AudioSystem.getSourceDataLine(audioFormat)
  // TODO - optimize this mess
  private val _buffer = ByteArray(bufferSize * 2)
  val buffer = FloatArray(bufferSize)

  fun start() {
    soundLine.open(audioFormat, bufferSize * 4)
    soundLine.start()
  }

  fun play() {
    if (iFrame == 0) {
      initDc()
    }

    transformBuffer()
    soundLine.write(_buffer, 0, bufferSize * 2)

    iFrame++
  }

  private fun initDc() {
    dc = buffer.sum() / bufferSize
  }

  private fun transformBuffer() {
    for (i in 0 until bufferSize) {
      val sample = buffer[i]
      val converted = ((sample - dc) * LEVEL).roundToInt()
      dc = (DC_ALPHA * dc) + (1 - DC_ALPHA) * sample

      // This seems to be the opposite of little-endian
      _buffer[i * 2] = (converted shr 8).toByte()
      _buffer[i * 2 + 1] = converted.toByte()
    }
  }

  companion object {
    private const val BUFFER_LENGTH_MS = 10
    private const val LEVEL = 100f
    private const val DC_ALPHA = 0.995f
  }
}
