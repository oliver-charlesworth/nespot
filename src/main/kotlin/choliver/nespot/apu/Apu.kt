package choliver.nespot.apu

import choliver.nespot.Data
import choliver.nespot.Memory
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
  private data class SynthContext<S : Synth>(
    val synth: S,
    val level: Double,
    val regs: MutableList<Data> = mutableListOf(0x00, 0x00, 0x00, 0x00),
    var enabled: Boolean = false
  )

  private val sequencer = Sequencer()
  private val pulse1 = SynthContext(PulseSynth(), 0.00752)
  private val pulse2 = SynthContext(PulseSynth(), 0.00752)
  private val triangle = SynthContext(TriangleSynth(), 0.00851)
  private val noise = SynthContext(NoiseSynth(), 0.00494)
  private val dmc = SynthContext(DmcSynth(memory = memory), 0.00335)

  private val alpha: Double
  private var state: Double = 0.0

  init {
    val omega = 2 * PI * 14e3 / SAMPLE_RATE_HZ
    alpha = cos(omega) - 1 + sqrt(cos(omega) * cos(omega) - 4 * cos(omega) + 3)
  }

  fun writeReg(reg: Int, data: Data) {
    when (reg) {
      in REG_PULSE1_RANGE -> pulse1.updatePulse(reg - REG_PULSE1_RANGE.first, data)
      in REG_PULSE2_RANGE -> pulse2.updatePulse(reg - REG_PULSE2_RANGE.first, data)
      in REG_TRI_RANGE -> triangle.updateTriangle(reg - REG_TRI_RANGE.first, data)
      in REG_NOISE_RANGE -> noise.updateNoise(reg - REG_NOISE_RANGE.first, data)
      in REG_DMC_RANGE -> dmc.updateDmc(reg - REG_DMC_RANGE.first, data)

      REG_SND_CHN -> {
        dmc.setStatus(data.isBitSet(4))
        noise.setStatus(data.isBitSet(3))
        triangle.setStatus(data.isBitSet(2))
        pulse2.setStatus(data.isBitSet(1))
        pulse1.setStatus(data.isBitSet(0))
      }

      REG_FRAME_COUNTER_CTRL -> {
        sequencer.mode = if (data.isBitSet(7)) FIVE_STEP else FOUR_STEP
        // TODO - IRQ inhibit
      }
    }
  }

  // See http://wiki.nesdev.com/w/index.php/APU_Pulse
  private fun SynthContext<PulseSynth>.updatePulse(idx: Int, data: Data) {
    fun extractPeriodCycles() = (extractTimer(regs) + 1) * 2 // APU clock rather than CPU clock

    regs[idx] = data
    when (idx) {
      0 -> {
        synth.dutyCycle = (data and 0xC0) shr 6
        synth.envLoop = data.isBitSet(5)
        synth.directEnvMode = data.isBitSet(4)
        synth.envParam = data and 0x0F
      }

      1 -> {
        synth.sweepEnabled = data.isBitSet(7)
        synth.sweepDivider = ((data and 0x70) shr 4) + 1
        synth.sweepNegate = data.isBitSet(3)
        synth.sweepShift = data and 0x07
      }

      2 -> synth.periodCycles = extractPeriodCycles()

      3 -> {
        synth.periodCycles = extractPeriodCycles()
        synth.length = extractLength(regs)
      }
    }
  }

  // See http://wiki.nesdev.com/w/index.php/APU_Triangle
  private fun SynthContext<TriangleSynth>.updateTriangle(idx: Int, data: Data) {
    fun extractPeriodCycles() = (extractTimer(regs) + 1).toRational()

    regs[idx] = data
    when (idx) {
      0 -> {
        // TODO - control flag
        synth.linear = data and 0x7F
      }

      2 -> synth.periodCycles = extractPeriodCycles()

      3 -> {
        synth.periodCycles = extractPeriodCycles()
        synth.length = extractLength(regs)
      }
    }
  }

  // See https://wiki.nesdev.com/w/index.php/APU_Noise
  private fun SynthContext<NoiseSynth>.updateNoise(idx: Int, data: Data) {
    regs[idx] = data
    when (idx) {
      0 -> {
        synth.volume = data and 0x0F
        // TODO - halt
        // TODO - volume/envelope flag
      }

      2 -> {
        synth.mode = (data and 0x80) shr 7
        synth.periodCycles = NOISE_PERIOD_TABLE[data and 0x0F].toRational()
      }

      3 -> synth.length = extractLength(regs)
    }
  }

  // See http://wiki.nesdev.com/w/index.php/APU_DMC
  private fun SynthContext<DmcSynth>.updateDmc(idx: Int, data: Data) {
    regs[idx] = data
    when (idx) {
      0 -> {
        // TODO - IRQ enabled
        synth.loop = data.isBitSet(6)
        synth.periodCycles = DMC_RATE_TABLE[data and 0x0F].toRational()
      }
      1 -> synth.level = data and 0x7F
      2 -> synth.address = 0xC000 + (data * 64)
      3 -> synth.length = (data * 16) + 1
    }
  }

  private fun extractTimer(regs: List<Data>) = ((regs[3] and 0x07) shl 8) or regs[2]
  private fun extractLength(regs: List<Data>) = LENGTH_TABLE[(regs[3] and 0xF8) shr 3]

  fun generate() {
    for (i in buffer.indices step 2) {
      val ticks = sequencer.take()

      // See http://wiki.nesdev.com/w/index.php/APU_Mixer
      // TODO - non-linear mixing (used by SMB to set triangle/noise level)
      val mixed = 0 +
        pulse1.take(ticks) +
        pulse2.take(ticks) +
        triangle.take(ticks) +
        noise.take(ticks) +
        dmc.take(ticks)

      // TODO - validate this filter
      val filtered = alpha * mixed + (1 - alpha) * state
      state = filtered
      val rounded = (filtered * 100).toInt()

      // This seems to be the opposite of little-endian
      buffer[i] = (rounded shr 8).toByte()
      buffer[i + 1] = rounded.toByte()
    }
  }

  private fun SynthContext<*>.take(ticks: Sequencer.Ticks) = synth.take(ticks) * (if (enabled) level else 0.0)

  private fun SynthContext<*>.setStatus(enabled: Boolean) {
    if (!enabled) { synth.length = 0 }
    this.enabled = enabled
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

    private val NOISE_PERIOD_TABLE = listOf(
      4, 8, 16, 32, 64, 96, 128, 160, 202, 254, 380, 508, 762, 1016, 2034, 4068
    )
    private val DMC_RATE_TABLE = listOf(
      428, 380, 340, 320, 286, 254, 226, 214, 190, 160, 142, 128, 106,  84,  72,  54
    )
  }
}
