package choliver.nespot.apu

import choliver.nespot.Data
import choliver.nespot.Memory
import choliver.nespot.apu.Sequencer.Mode.FIVE_STEP
import choliver.nespot.apu.Sequencer.Mode.FOUR_STEP
import choliver.nespot.isBitSet

// TODO - interrupts
// TODO - read status register
class Apu(
  private val buffer: ByteArray,
  memory: Memory
) {
  private val sequencer = Sequencer()
  private val pulse1 = SynthContext(PulseSynth(), 0.00752)
  private val pulse2 = SynthContext(PulseSynth(), 0.00752)
  private val triangle = SynthContext(TriangleSynth(), 0.00851).apply { fixEnvelope(1) }
  private val noise = SynthContext(NoiseSynth(), 0.00494)
  private val dmc = SynthContext(DmcSynth(memory = memory), 0.00335).apply { fixEnvelope(1) }
  private val mixer = Mixer(
    sequencer,
    listOf(pulse1, pulse2, triangle, noise, dmc)
  )

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
    fun extractPeriodCycles() = (extractTimer() + 1) * 2 // APU clock rather than CPU clock

    regs[idx] = data
    when (idx) {
      0 -> {
        synth.dutyCycle = (data and 0xC0) shr 6
        updateEnvelope()
      }

      1 -> {
        sweep.enabled = data.isBitSet(7)
        sweep.divider = (data and 0x70) shr 4
        sweep.negate = data.isBitSet(3)
        sweep.shift = data and 0x07
        sweep.reset()
      }

      2 -> timer.periodCycles = extractPeriodCycles().toRational()

      3 -> {
        timer.periodCycles = extractPeriodCycles().toRational()
        synth.length = extractLength()
        envelope.reset()
      }
    }
  }

  // See http://wiki.nesdev.com/w/index.php/APU_Triangle
  private fun SynthContext<TriangleSynth>.updateTriangle(idx: Int, data: Data) {
    fun extractPeriodCycles() = (extractTimer() + 1).toRational()

    regs[idx] = data
    when (idx) {
      0 -> {
        // TODO - control flag
        synth.linear = data and 0x7F
      }

      2 -> timer.periodCycles = extractPeriodCycles()

      3 -> {
        timer.periodCycles = extractPeriodCycles()
        synth.length = extractLength()
      }
    }
  }

  // See https://wiki.nesdev.com/w/index.php/APU_Noise
  private fun SynthContext<NoiseSynth>.updateNoise(idx: Int, data: Data) {
    regs[idx] = data
    when (idx) {
      0 -> updateEnvelope()

      2 -> {
        synth.mode = (data and 0x80) shr 7
        timer.periodCycles = NOISE_PERIOD_TABLE[data and 0x0F].toRational()
      }

      3 -> {
        synth.length = extractLength()
        envelope.reset()
      }
    }
  }

  // See http://wiki.nesdev.com/w/index.php/APU_DMC
  private fun SynthContext<DmcSynth>.updateDmc(idx: Int, data: Data) {
    regs[idx] = data
    when (idx) {
      0 -> {
        // TODO - IRQ enabled
        synth.loop = data.isBitSet(6)
        timer.periodCycles = DMC_RATE_TABLE[data and 0x0F].toRational()
      }
      1 -> synth.level = data and 0x7F
      2 -> synth.address = 0xC000 + (data * 64)
      3 -> synth.length = (data * 16) + 1
    }
  }

  private fun SynthContext<*>.updateEnvelope() {
    envelope.loop = regs[0].isBitSet(5)
    envelope.directMode = regs[0].isBitSet(4)
    envelope.param = regs[0] and 0x0F
  }

  private fun SynthContext<*>.extractTimer() = ((regs[3] and 0x07) shl 8) or regs[2]
  private fun SynthContext<*>.extractLength() = LENGTH_TABLE[(regs[3] and 0xF8) shr 3]

  fun generate() {
    for (i in buffer.indices step 2) {
      val rounded = mixer.take()

      // This seems to be the opposite of little-endian
      buffer[i] = (rounded shr 8).toByte()
      buffer[i + 1] = rounded.toByte()
    }
  }

  private fun SynthContext<*>.setStatus(enabled: Boolean) {
    if (!enabled) { synth.length = 0 }
    this.enabled = enabled
  }

  private fun SynthContext<*>.fixEnvelope(level: Int) {
    envelope.directMode = true
    envelope.param = level
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
