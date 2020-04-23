package choliver.nespot.runner

import choliver.nespot.apu.SAMPLE_RATE_HZ
import javax.sound.sampled.AudioFormat
import javax.sound.sampled.AudioSystem

class Audio(frameRateHz: Int) {
  private val bufferSize = (SAMPLE_RATE_HZ / frameRateHz)
  private val audioFormat = AudioFormat(SAMPLE_RATE_HZ.toFloat(), 8, 1, true, true)
  private val soundLine = AudioSystem.getSourceDataLine(audioFormat)
  val buffer = ByteArray(bufferSize)

  init {
    if ((bufferSize * frameRateHz) != SAMPLE_RATE_HZ) {
      throw IllegalArgumentException("Non-integer ratio between frame rate and sample rate")
    }
  }

  fun start() {
    soundLine.open(audioFormat, bufferSize)
    soundLine.start()
  }

  fun play() {
    soundLine.write(buffer, 0, bufferSize)
  }
}
