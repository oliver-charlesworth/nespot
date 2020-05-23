package choliver.nespot.apu

import choliver.nespot.Data
import choliver.nespot.Memory
import choliver.nespot.Rational
import choliver.nespot.apu.FrameSequencer.Mode.FIVE_STEP
import choliver.nespot.apu.FrameSequencer.Mode.FOUR_STEP
import choliver.nespot.isBitSet

// TODO - frame interrupt
class Apu(
  cpuFreqHz: Rational,
  sampleRateHz: Int,
  bufferLengthMs: Int = BUFFER_LENGTH_MS,
  memory: Memory,
  private val cyclesPerSample: Rational = cpuFreqHz / sampleRateHz,
  private val sequencer: FrameSequencer = FrameSequencer(cyclesPerSample),
  private val channels: Channels = Channels(cyclesPerSample, memory),
  private val onAudioBufferReady: (FloatArray) -> Unit
) {
  private val bufferSize = sampleRateHz * bufferLengthMs / 1000
  private val bufferA = FloatArray(bufferSize)
  private val bufferB = FloatArray(bufferSize)
  private var buffer = bufferA

  private var untilNextSample = cyclesPerSample.a
  private var iSample = 0
  private val mixer = Mixer(sampleRateHz, sequencer, channels)

  val irq get() = channels.dmc.synth.irq

  fun readStatus() = 0 +
    (if (channels.dmc.synth.hasRemainingOutput) 0x10 else 0x00) +
    (if (channels.noi.synth.hasRemainingOutput) 0x08 else 0x00) +
    (if (channels.tri.synth.hasRemainingOutput) 0x04 else 0x00) +
    (if (channels.sq2.synth.hasRemainingOutput) 0x02 else 0x00) +
    (if (channels.sq1.synth.hasRemainingOutput) 0x01 else 0x00)

  fun writeReg(reg: Int, data: Data) {
    when (reg) {
      in REG_SQ1_RANGE -> channels.sq1.updatePulse(reg - REG_SQ1_RANGE.first, data)
      in REG_SQ2_RANGE -> channels.sq2.updatePulse(reg - REG_SQ2_RANGE.first, data)
      in REG_TRI_RANGE -> channels.tri.updateTriangle(reg - REG_TRI_RANGE.first, data)
      in REG_NOI_RANGE -> channels.noi.updateNoise(reg - REG_NOI_RANGE.first, data)
      in REG_DMC_RANGE -> channels.dmc.updateDmc(reg - REG_DMC_RANGE.first, data)

      REG_SND_CHN -> {
        channels.dmc.synth.enabled = data.isBitSet(4)
        channels.noi.synth.enabled = data.isBitSet(3)
        channels.tri.synth.enabled = data.isBitSet(2)
        channels.sq2.synth.enabled = data.isBitSet(1)
        channels.sq1.synth.enabled = data.isBitSet(0)
      }

      REG_FRAME_COUNTER_CTRL -> {
        sequencer.mode = if (data.isBitSet(7)) FIVE_STEP else FOUR_STEP
      }
    }
  }

  // See http://wiki.nesdev.com/w/index.php/APU_Pulse
  private fun SynthContext<SquareSynth>.updatePulse(idx: Int, data: Data) {
    fun extractPeriodCycles() = (extractTimer() + 1) * 2 // APU clock rather than CPU clock

    regs[idx] = data
    when (idx) {
      0 -> {
        synth.dutyCycle = (data and 0xC0) shr 6
        synth.haltLength = data.isBitSet(5)
        updateEnvelope()
      }

      1 -> {
        sweep.enabled = data.isBitSet(7)
        sweep.divider = (data and 0x70) shr 4
        sweep.negate = data.isBitSet(3)
        sweep.shift = data and 0x07
        sweep.restart()
      }

      2 -> timer.periodCycles = extractPeriodCycles()

      3 -> {
        timer.periodCycles = extractPeriodCycles()
        synth.length = extractLength()
        envelope.restart()
      }
    }
  }

  // See http://wiki.nesdev.com/w/index.php/APU_Triangle
  private fun SynthContext<TriangleSynth>.updateTriangle(idx: Int, data: Data) {
    fun extractPeriodCycles() = extractTimer() + 1

    regs[idx] = data
    when (idx) {
      0 -> {
        synth.haltLength = data.isBitSet(7)
        synth.preventReloadClear = data.isBitSet(7)
        synth.linLength = data and 0x7F
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
      0 -> {
        synth.haltLength = data.isBitSet(5)
        updateEnvelope()
      }

      2 -> {
        synth.mode = (data and 0x80) shr 7
        timer.periodCycles = NOISE_PERIOD_TABLE[data and 0x0F]
      }

      3 -> {
        synth.length = extractLength()
        envelope.restart()
      }
    }
  }

  // See http://wiki.nesdev.com/w/index.php/APU_DMC
  private fun SynthContext<DmcSynth>.updateDmc(idx: Int, data: Data) {
    regs[idx] = data
    when (idx) {
      0 -> {
        synth.irqEnabled = data.isBitSet(7)
        synth.loop = data.isBitSet(6)
        timer.periodCycles = DMC_RATE_TABLE[data and 0x0F]
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

  fun advance(numCycles: Int) {
    untilNextSample -= numCycles * cyclesPerSample.b

    while (untilNextSample <= 0) {
      untilNextSample += cyclesPerSample.a
      generateSample()
    }
  }

  private fun generateSample() {
    buffer[iSample] = mixer.take()
    iSample++
    if (iSample == bufferSize) {
      iSample = 0
      onAudioBufferReady(buffer)
      buffer = if (buffer === bufferA) bufferB else bufferA
    }
  }

  companion object {
    private val BUFFER_LENGTH_MS = 10

    // See https://wiki.nesdev.com/w/index.php/2A03
    private val REG_SQ1_RANGE = 0x00..0x03
    private val REG_SQ2_RANGE = 0x04..0x07
    private val REG_TRI_RANGE = 0x08..0x0B
    private val REG_NOI_RANGE = 0x0C..0x0F
    private val REG_DMC_RANGE = 0x10..0x13
    private const val REG_SND_CHN = 0x15
    private const val REG_FRAME_COUNTER_CTRL = 0x17

    private val NOISE_PERIOD_TABLE = listOf(
      4, 8, 16, 32, 64, 96, 128, 160, 202, 254, 380, 508, 762, 1016, 2034, 4068
    )
    private val DMC_RATE_TABLE = listOf(
      428, 380, 340, 320, 286, 254, 226, 214, 190, 160, 142, 128, 106,  84,  72,  54
    )

    private fun SynthContext<*>.extractTimer() = ((regs[3] and 0x07) shl 8) or regs[2]

    private fun SynthContext<*>.extractLength() = LENGTH_TABLE[(regs[3] and 0xF8) shr 3]
  }
}
