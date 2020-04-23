package choliver.nespot.apu

import choliver.nespot.Data

class Apu(
  private val buffer: ByteArray
) {
  private val triangle = TriangleGenerator()

  fun writeReg(reg: Int, data: Data) {
    when (reg) {
      REG_SQ1_VOL -> {

      }

      REG_SQ1_SWEEP -> {

      }

      REG_SQ1_LO -> {

      }

      REG_SQ1_HI -> {

      }

      REG_SQ2_VOL -> {

      }

      REG_SQ2_SWEEP -> {

      }

      REG_SQ2_LO -> {

      }

      REG_SQ2_HI -> {

      }

      REG_TRI_LINEAR -> {
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

      }

      REG_NOISE_LO -> {

      }

      REG_NOISE_HI -> {

      }

      REG_DMC_FREQ -> {

      }

      REG_DMC_RAW -> {

      }

      REG_DMC_START -> {

      }

      REG_DMC_LEN -> {

      }

      REG_SND_CHN -> {

      }

      REG_FRAME_COUNTER_CTRL -> {
        // TODO
      }
    }
  }

  fun generate() {
    val samples = triangle.generate(buffer.size)
    for (i in buffer.indices) {
      buffer[i] = (samples[i] * 10).toByte()
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
