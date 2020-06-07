package choliver.nespot.apu

import choliver.nespot.apu.FrameSequencer.Mode.FIVE_STEP
import choliver.nespot.apu.FrameSequencer.Mode.FOUR_STEP
import choliver.nespot.common.Data
import choliver.nespot.common.Rational
import choliver.nespot.common.isBitSet
import choliver.nespot.memory.Memory
import choliver.nespot.nes.AudioSink
import kotlin.math.ceil
import kotlin.math.min

// TODO - frame interrupt
class Apu(
  cpuFreqHz: Rational,
  memory: Memory,
  private val audioSink: AudioSink,
  private val cyclesPerSample: Rational = cpuFreqHz / audioSink.sampleRateHz,
  private val channels: Channels = Channels(memory)
) {
  private var untilNextSample = cyclesPerSample.a
  private val mixer = Mixer(audioSink.sampleRateHz, channels)

  val irq get() = channels.dmc.synth.irq

  fun readStatus() = 0 +
    (if (channels.dmc.synth.outputRemaining) 0x10 else 0x00) +
    (if (channels.noi.synth.outputRemaining) 0x08 else 0x00) +
    (if (channels.tri.synth.outputRemaining) 0x04 else 0x00) +
    (if (channels.sq2.synth.outputRemaining) 0x02 else 0x00) +
    (if (channels.sq1.synth.outputRemaining) 0x01 else 0x00)

  fun writeReg(reg: Int, data: Data) {
    when (reg) {
      in REG_SQ1_RANGE -> channels.sq1.updateSquare(reg - REG_SQ1_RANGE.first, data)
      in REG_SQ2_RANGE -> channels.sq2.updateSquare(reg - REG_SQ2_RANGE.first, data)
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
        channels.seq.mode = if (data.isBitSet(7)) FIVE_STEP else FOUR_STEP
      }
    }
  }

  // See http://wiki.nesdev.com/w/index.php/APU_Pulse
  private fun SquareChannel.updateSquare(idx: Int, data: Data) {
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
  private fun TriangleChannel.updateTriangle(idx: Int, data: Data) {
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
  private fun NoiseChannel.updateNoise(idx: Int, data: Data) {
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
  private fun DmcChannel.updateDmc(idx: Int, data: Data) {
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

  private fun Channel<*, *, EnvelopeActive>.updateEnvelope() {
    envelope.loop = regs[0].isBitSet(5)
    envelope.directMode = regs[0].isBitSet(4)
    envelope.param = regs[0] and 0x0F
  }

  fun advance(numCycles: Int) {
    var n = numCycles
    while (n * cyclesPerSample.b >= untilNextSample) {
      val numChunk = ceil(untilNextSample.toDouble() / cyclesPerSample.b).toInt()
      channels.advance(numChunk)
      untilNextSample -= numChunk * cyclesPerSample.b
      untilNextSample += cyclesPerSample.a
      audioSink.put(mixer.sample())
      n -= numChunk
    }
    channels.advance(n)
    untilNextSample -= n * cyclesPerSample.b
  }

  companion object {
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

    // See http://wiki.nesdev.com/w/index.php/APU_Length_Counter
    private val LENGTH_TABLE = listOf(
      10, 254, 20, 2, 40, 4, 80, 6, 160, 8, 60, 10, 14, 12, 26, 14,
      12, 16, 24, 18, 48, 20, 96, 22, 192, 24, 72, 26, 16, 28, 32, 30
    )

    private fun Channel<*, *, *>.extractTimer() = ((regs[3] and 0x07) shl 8) or regs[2]

    private fun Channel<*, *, *>.extractLength() = LENGTH_TABLE[(regs[3] and 0xF8) shr 3]
  }
}
