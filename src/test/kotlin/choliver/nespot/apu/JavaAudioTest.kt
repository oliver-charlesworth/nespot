package choliver.nespot.apu

import javax.sound.sampled.AudioFormat
import javax.sound.sampled.AudioSystem
import kotlin.math.roundToInt

const val SAMPLE_RATE_HZ = 44100f

fun main() {
  val audioFormat = AudioFormat(SAMPLE_RATE_HZ, 8, 1, true, true)
  val soundLine = AudioSystem.getSourceDataLine(audioFormat)
  soundLine.open(audioFormat)
  soundLine.start()

  val bufferSize = 2205

  soundLine.open(audioFormat, bufferSize)
  soundLine.start()

  val buffer = ByteArray(bufferSize)

  val triangle = TriangleGenerator(253) // This should be ~220Hz
  repeat(80) {
    val samples = triangle.generate(bufferSize)

    for (i in 0 until bufferSize) {
      buffer[i] = (samples[i] * 10).toByte()
    }
    soundLine.write(buffer, 0, bufferSize)
  }
}

// See http://wiki.nesdev.com/w/index.php/APU_Triangle
class TriangleGenerator(
  private val timer: Int    // 11-bit
) {
  private var clock = 0
  private var phase = 0
  private var residual = 0.0

  init {
    println("CLOCKS_PER_SAMPLE = ${CLOCKS_PER_SAMPLE}")
    println("RESIDUAL_CLOCKS_PER_SAMPLE = ${RESIDUAL_CLOCKS_PER_SAMPLE}")
  }

  fun generate(num: Int) = List(num) {
    clock -= INT_CLOCKS_PER_SAMPLE
    residual += RESIDUAL_CLOCKS_PER_SAMPLE
    if (residual >= 1.0) {
      clock--
      residual -= 1.0
    }

    // TODO - this doesn't work for very small timer values - need to advance multiple times
    if (clock <= 0) {
      clock += timer // Note *not* (timer - 1)
      phase = (phase + 1) % SEQUENCE.size
    }

    // TODO - should we do a weighted average?

    SEQUENCE[phase]
  }

  companion object {
    // TODO - what is the offset here?  Does it matter if there's a DC blocking filter?
    private val SEQUENCE = listOf(
      15, 14, 13, 12, 11, 10,  9,  8,  7,  6,  5,  4,  3,  2,  1,  0,
       0,  1,  2,  3,  4,  5,  6,  7,  8,  9, 10, 11, 12, 13, 14, 15
    )

    // See https://wiki.nesdev.com/w/index.php/Cycle_reference_chart
    private const val CPU_FREQ_HZ = (236.25e6 / 11) / 12

    private const val CLOCKS_PER_SAMPLE = CPU_FREQ_HZ.toDouble() / SAMPLE_RATE_HZ
    private const val INT_CLOCKS_PER_SAMPLE = CLOCKS_PER_SAMPLE.toInt()
    private const val RESIDUAL_CLOCKS_PER_SAMPLE = CLOCKS_PER_SAMPLE - INT_CLOCKS_PER_SAMPLE
  }
}
