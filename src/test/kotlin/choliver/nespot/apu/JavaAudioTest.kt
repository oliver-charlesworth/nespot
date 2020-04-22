package choliver.nespot.apu

import javax.sound.sampled.AudioFormat
import javax.sound.sampled.AudioSystem


fun main() {
  val audioFormat = AudioFormat(SAMPLE_RATE_HZ, 8, 1, true, true)
  val soundLine = AudioSystem.getSourceDataLine(audioFormat)
  soundLine.open(audioFormat)
  soundLine.start()

  val bufferSize = 2205

  soundLine.open(audioFormat, bufferSize)
  soundLine.start()

  val buffer = ByteArray(bufferSize)

  val triangle = TriangleGenerator(
    timer = 253,  // This should be ~220Hz
    linear = 127
  )
  repeat(80) {
    val samples = triangle.generate(bufferSize)

    for (i in 0 until bufferSize) {
      buffer[i] = (samples[i] * 10).toByte()
    }
    soundLine.write(buffer, 0, bufferSize)
  }
}

