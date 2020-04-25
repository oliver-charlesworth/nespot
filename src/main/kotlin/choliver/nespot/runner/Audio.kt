package choliver.nespot.runner

import choliver.nespot.apu.SAMPLE_RATE_HZ
import javax.sound.sampled.AudioFormat
import javax.sound.sampled.AudioSystem

class Audio(frameRateHz: Int) {
  private val bufferSize = (SAMPLE_RATE_HZ / frameRateHz)
  private val audioFormat = AudioFormat(SAMPLE_RATE_HZ.toFloat(), 16, 1, true, false)
  private val soundLine = AudioSystem.getSourceDataLine(audioFormat)
  val buffer = ByteArray(bufferSize * 2)

  init {
    if ((bufferSize * frameRateHz) != SAMPLE_RATE_HZ) {
      throw IllegalArgumentException("Non-integer ratio between frame rate and sample rate")
    }
  }

  fun start() {
    soundLine.open(audioFormat, bufferSize * 4)
    soundLine.start()
  }

  fun play() {
    soundLine.write(buffer, 0, bufferSize * 2)
  }
}
