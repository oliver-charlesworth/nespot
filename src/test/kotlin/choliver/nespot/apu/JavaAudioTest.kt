package choliver.nespot.apu

import choliver.nespot.runner.Audio
import javax.sound.sampled.AudioFormat
import javax.sound.sampled.AudioSystem
import kotlin.system.measureTimeMillis


fun main() {
  val audio = Audio(frameRateHz = 20)
  val triangle = TriangleGenerator(
//    timer = 253,  // This should be ~220Hz
    linear = 127,
    length = 0
  )

  audio.start()
  repeat(80) {
    val samples = triangle.generate(audio.buffer.size)

    for (i in audio.buffer.indices) {
      audio.buffer[i] = (samples[i] * 10).toByte()
    }
    audio.play()
  }
}

