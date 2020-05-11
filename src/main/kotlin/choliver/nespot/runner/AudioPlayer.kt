package choliver.nespot.runner

import choliver.nespot.SAMPLE_RATE_HZ
import javax.sound.sampled.AudioFormat
import javax.sound.sampled.AudioSystem
import kotlin.math.roundToInt

class AudioPlayer {
  private var first = true
  private var dc = 0f
  private val audioFormat = AudioFormat(SAMPLE_RATE_HZ.toFloat(), 16, 1, true, false)
  private val soundLine = AudioSystem.getSourceDataLine(audioFormat)
  private val work = ByteArray(WORK_BUFFER_SIZE * 2)

  private var prev: Long = 0

  fun start() {
    soundLine.open(audioFormat, LINE_BUFFER_SIZE * 2)
    soundLine.start()
  }

  fun play(buffer: FloatArray) {
    val now = System.currentTimeMillis()
    println("AudioPlayer::play (${now - prev} ms)")
    prev = now

    if (buffer.size > WORK_BUFFER_SIZE) {
      throw IllegalArgumentException("Max buffer is ${WORK_BUFFER_SIZE} samples")
    }

    if (first) {
      initDc(buffer)
    }

    transformBuffer(buffer)
    soundLine.write(work, 0, buffer.size * 2)

    first = false
  }

  private fun initDc(buffer: FloatArray) {
    dc = buffer.sum() / buffer.size
  }

  private fun transformBuffer(buffer: FloatArray) {
    buffer.forEachIndexed { idx, sample ->
      val converted = ((sample - dc) * LEVEL).roundToInt()
      dc = (DC_ALPHA * dc) + (1 - DC_ALPHA) * sample

      // This seems to be the opposite of little-endian
      work[idx * 2] = (converted shr 8).toByte()
      work[idx * 2 + 1] = converted.toByte()
    }
  }

  companion object {
    private const val WORK_BUFFER_LENGTH_MS = 1000
    private const val LINE_BUFFER_LENGTH_MS = 20
    private const val LEVEL = 100f
    private const val DC_ALPHA = 0.995f

    private const val WORK_BUFFER_SIZE = (SAMPLE_RATE_HZ * WORK_BUFFER_LENGTH_MS) / 1000
    private const val LINE_BUFFER_SIZE = (SAMPLE_RATE_HZ * LINE_BUFFER_LENGTH_MS) / 1000
  }
}
