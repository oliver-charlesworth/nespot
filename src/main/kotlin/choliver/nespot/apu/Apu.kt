package choliver.nespot.apu

import choliver.nespot.Data
import choliver.nespot.Memory
import choliver.nespot.apu.DmcSynth.Companion.DMC_RATE_TABLE
import choliver.nespot.apu.Sequencer.Mode.FIVE_STEP
import choliver.nespot.apu.Sequencer.Mode.FOUR_STEP
import choliver.nespot.isBitSet
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sqrt

// TODO - interrupts
// TODO - read status register
class Apu(
  private val buffer: ByteArray,
  memory: Memory
) {
  private val sequencer = Sequencer(
    cyclesPerSample = CYCLES_PER_SAMPLE,
    frameSequencerFourStepPeriodCycles = FRAME_SEQUENCER_4_STEP_PERIOD_CYCLES,
    frameSequencerFiveStepPeriodCycles = FRAME_SEQUENCER_5_STEP_PERIOD_CYCLES
  )

  private val pulse1 = PulseSynth(cyclesPerSample = CYCLES_PER_SAMPLE)
  private val pulse2 = PulseSynth(cyclesPerSample = CYCLES_PER_SAMPLE)
  private val triangle = TriangleSynth(cyclesPerSample = CYCLES_PER_SAMPLE)
  private val noise = NoiseSynth(cyclesPerSample = CYCLES_PER_SAMPLE)
  private val dmc = DmcSynth(cyclesPerSample = CYCLES_PER_SAMPLE, memory = memory)

  private var pulse1Enabled = false
  private var pulse2Enabled = false
  private var triangleEnabled = false
  private var noiseEnabled = false
  private var dmcEnabled = false

  private val alpha: Double
  private var state: Double = 0.0

  init {
    val omega = 2 * PI * 14e3 / SAMPLE_RATE_HZ
    alpha = cos(omega) - 1 + sqrt(cos(omega) * cos(omega) - 4 * cos(omega) + 3)
  }

  fun writeReg(reg: Int, data: Data) {
    when (reg) {
      in REG_PULSE1_RANGE -> pulse1.writeReg(reg - REG_PULSE1_RANGE.first, data)
      in REG_PULSE2_RANGE -> pulse2.writeReg(reg - REG_PULSE2_RANGE.first, data)
      in REG_TRI_RANGE -> triangle.writeReg(reg - REG_TRI_RANGE.first, data)
      in REG_NOISE_RANGE -> noise.writeReg(reg - REG_NOISE_RANGE.first, data)
      in REG_DMC_RANGE -> dmc.writeReg(reg - REG_DMC_RANGE.first, data)

      REG_SND_CHN -> {
        if (!data.isBitSet(4)) { }  // TODO - mess with DMC
        if (!data.isBitSet(3)) { noise.length = 0 }
        if (!data.isBitSet(2)) { triangle.length = 0 }
        if (!data.isBitSet(1)) { pulse2.length = 0 }
        if (!data.isBitSet(0)) { pulse1.length = 0 }
        dmcEnabled = data.isBitSet(4)
        noiseEnabled = data.isBitSet(3)
        triangleEnabled = data.isBitSet(2)
        pulse2Enabled = data.isBitSet(1)
        pulse1Enabled = data.isBitSet(0)
      }

      REG_FRAME_COUNTER_CTRL -> {
        sequencer.mode = if (data.isBitSet(7)) FIVE_STEP else FOUR_STEP
        // TODO - IRQ inhibit
      }
    }
  }

  private fun PulseSynth.writeReg(reg: Int, data: Data) {
    when (reg) {
      0 -> {
        dutyCycle = (data and 0xC0) shr 6
        volume = data and 0x0F
        // TODO - halt
        // TODO - volume/envelope flag
        // TODO - envelope divider
      }

      1 -> {
        // TODO - sweep
      }

      2 -> timer = (timer and 0x0700) or data

      3 -> {
        timer = (timer and 0x00FF) or ((data and 0x07) shl 8)
        length = extractLength(data)
      }
    }
  }

  private fun TriangleSynth.writeReg(reg: Int, data: Data) {
    when (reg) {
      0 -> {
        // TODO - control flag
        linear = data and 0x7F
      }

      2 -> timer = (timer and 0x0700) or data

      3 -> {
        timer = (timer and 0x00FF) or ((data and 0x07) shl 8)
        length = extractLength(data)
      }
    }
  }

  private fun NoiseSynth.writeReg(reg: Int, data: Data) {
    when (reg) {
      0 -> {
        volume = data and 0x0F
        // TODO - halt
        // TODO - volume/envelope flag
      }

      2 -> {
        mode = (data and 0x80) shr 7
        period = data and 0x0F
      }

      3 -> length = extractLength(data)
    }
  }

  // See http://wiki.nesdev.com/w/index.php/APU_DMC
  private fun DmcSynth.writeReg(reg: Int, data: Data) {
    when (reg) {
      0 -> {
        // TODO - IRQ enabled
        loop = data.isBitSet(6)
        periodCycles = DMC_RATE_TABLE[data and 0x0F].toRational()
      }
      1 -> level = data and 0x7F
      2 -> address = 0xC000 + (data * 64)
      3 -> length = (data * 16) + 1
    }
  }

  private fun extractLength(data: Data) = LENGTH_TABLE[(data and 0xF8) shr 3]

  fun generate() {
    for (i in buffer.indices step 2) {
      val ticks = sequencer.take()

      // See http://wiki.nesdev.com/w/index.php/APU_Mixer
      // I don't believe the "non-linear" mixing is worth it.
      val mixed = 0 +
        (if (pulse1Enabled) 0.00752 else 0.0) * pulse1.take(ticks) +
        (if (pulse2Enabled) 0.00752 else 0.0) * pulse2.take(ticks) +
        (if (triangleEnabled) 0.00851 else 0.0) * triangle.take(ticks) +
        (if (noiseEnabled) 0.00494 else 0.0) * noise.take(ticks) +
        (if (dmcEnabled) 0.00335 else 0.0) * dmc.take(ticks)

      // TODO - validate this filter
      val filtered = alpha * mixed + (1 - alpha) * state
      state = filtered
      val rounded = (filtered * 100).toInt()

      // This seems to be the opposite of little-endian
      buffer[i] = (rounded shr 8).toByte()
      buffer[i + 1] = rounded.toByte()
    }
  }

  companion object {
    // See https://wiki.nesdev.com/w/index.php/2A03
    private val REG_PULSE1_RANGE = 0x00..0x03
    private val REG_PULSE2_RANGE = 0x04..0x07
    private val REG_TRI_RANGE = 0x08..0x0B
    private val REG_NOISE_RANGE = 0x0C..0x0F
    private val REG_DMC_RANGE = 0x10..0x13
    private const val REG_SND_CHN = 0x15
    private const val REG_FRAME_COUNTER_CTRL = 0x17
  }
}
