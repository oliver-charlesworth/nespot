package choliver.nespot.ui

import choliver.nespot.nes.AudioSink
import java.io.Closeable
import javax.sound.sampled.AudioFormat
import javax.sound.sampled.AudioSystem
import kotlin.math.roundToInt

class AudioPlayer : Closeable {
  private val audioFormat = AudioFormat(SAMPLE_RATE_HZ.toFloat(), 16, 1, true, false)
  private val soundLine = AudioSystem.getSourceDataLine(audioFormat)
  private val buffer = FloatArray(BUFFER_SIZE)
  private val work = ByteArray(BUFFER_SIZE * 2)

  val sink = object : AudioSink {
    private var idx = 0

    override val sampleRateHz = SAMPLE_RATE_HZ

    override fun put(sample: Float) {
      buffer[idx++] = sample
      if (idx == BUFFER_SIZE) {
        idx = 0
        play(buffer)
      }
    }
  }

  fun start() {
    soundLine.open(audioFormat, LINE_BUFFER_SIZE * 2)
    soundLine.start()
  }

  override fun close() {
    soundLine.close()
  }

  private fun play(buffer: FloatArray) {
    transformBuffer(buffer)
    soundLine.write(work, 0, buffer.size * 2)
  }

  private fun transformBuffer(buffer: FloatArray) {
    buffer.forEachIndexed { idx, sample ->
      val converted = (sample * LEVEL).roundToInt()

      // This seems to be the opposite of little-endian
      work[idx * 2] = (converted shr 8).toByte()
      work[idx * 2 + 1] = converted.toByte()
    }
  }

  companion object {
    private const val SAMPLE_RATE_HZ = 44100
    private const val LINE_BUFFER_LENGTH_MS = 20
    private const val BUFFER_LENGTH_MS = 10
    private const val LEVEL = 100f

    private const val LINE_BUFFER_SIZE = (SAMPLE_RATE_HZ * LINE_BUFFER_LENGTH_MS) / 1000
    private const val BUFFER_SIZE = (SAMPLE_RATE_HZ * BUFFER_LENGTH_MS) / 1000
  }
}
