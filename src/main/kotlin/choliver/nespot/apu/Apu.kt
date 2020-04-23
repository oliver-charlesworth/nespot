package choliver.nespot.apu

import choliver.nespot.Data

class Apu(
  private val buffer: ByteArray
) {
  private val pulse1 = PulseGenerator()
  private val pulse2 = PulseGenerator()
  private val triangle = TriangleGenerator()

  fun writeReg(reg: Int, data: Data) {
    when (reg) {
      REG_SQ1_VOL -> {
        pulse1.dutyCycle = (data and 0xC0) ushr 6
        // TODO - halt
        // TODO - volume/envelope flag
        // TODO - envelope divider
        pulse1.volume = data and 0x0F
      }

      REG_SQ1_SWEEP -> {
        // TODO
      }

      REG_SQ1_LO -> {
        pulse1.timer = (pulse1.timer and 0x0700) or data
      }

      REG_SQ1_HI -> {
        pulse1.timer = (pulse1.timer and 0x00FF) or ((data and 0x07) shl 8)
        pulse1.length = (data and 0xF8) ushr 5
      }

      REG_SQ2_VOL -> {
        pulse2.dutyCycle = (data and 0xC0) ushr 6
        // TODO - halt
        // TODO - volume/envelope flag
        // TODO - envelope divider
        pulse2.volume = data and 0x0F
      }

      REG_SQ2_SWEEP -> {
        // TODO
      }

      REG_SQ2_LO -> {
        pulse2.timer = (pulse2.timer and 0x0700) or data
      }

      REG_SQ2_HI -> {
        pulse2.timer = (pulse2.timer and 0x00FF) or ((data and 0x07) shl 8)
        pulse2.length = (data and 0xF8) ushr 5
      }

      REG_TRI_LINEAR -> {
        // TODO - control flag
        triangle.linear = data and 0x7F
      }

      REG_TRI_LO -> {
        triangle.timer = (triangle.timer and 0x0700) or data
      }

      REG_TRI_HI -> {
        triangle.timer = (triangle.timer and 0x00FF) or ((data and 0x07) shl 8)
        triangle.length = (data and 0xF8) ushr 5
      }

      REG_NOISE_VOL -> {
        // TODO
      }

      REG_NOISE_LO -> {
        // TODO
      }

      REG_NOISE_HI -> {
        // TODO
      }

      REG_DMC_FREQ -> {
        // TODO
      }

      REG_DMC_RAW -> {
        // TODO
      }

      REG_DMC_START -> {
        // TODO
      }

      REG_DMC_LEN -> {
        // TODO
      }

      REG_SND_CHN -> {
        // TODO
      }

      REG_FRAME_COUNTER_CTRL -> {
        // TODO
      }
    }
  }

  fun generate() {
    val samplesPulse1 = pulse1.generate(buffer.size)
    val samplesPulse2 = pulse2.generate(buffer.size)
    val samplesTriangle = triangle.generate(buffer.size)
    for (i in buffer.indices) {
      buffer[i] = ((
        samplesPulse1[i] * 0.00752 +
          samplesPulse2[i] * 0.00752 +
          samplesTriangle[i] * 0.00851
        ) * 100).toByte()
    }
  }

  companion object {
    // See https://wiki.nesdev.com/w/index.php/2A03
    const val REG_SQ1_VOL = 0x00
    const val REG_SQ1_SWEEP = 0x01
    const val REG_SQ1_LO = 0x02
    const val REG_SQ1_HI = 0x03

    const val REG_SQ2_VOL = 0x04
    const val REG_SQ2_SWEEP = 0x05
    const val REG_SQ2_LO = 0x06
    const val REG_SQ2_HI = 0x07

    const val REG_TRI_LINEAR = 0x08
    const val REG_TRI_LO = 0x0A
    const val REG_TRI_HI = 0x0B

    const val REG_NOISE_VOL = 0x0C
    const val REG_NOISE_LO = 0x0E
    const val REG_NOISE_HI = 0x0F

    const val REG_DMC_FREQ = 0x10
    const val REG_DMC_RAW = 0x11
    const val REG_DMC_START = 0x12
    const val REG_DMC_LEN = 0x13

    const val REG_SND_CHN = 0x15
    const val REG_FRAME_COUNTER_CTRL = 0x17
  }
}
