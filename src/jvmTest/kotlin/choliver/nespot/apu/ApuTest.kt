package choliver.nespot.apu

import choliver.nespot.apu.FrameSequencer.Mode.FIVE_STEP
import choliver.nespot.common.Rational
import choliver.nespot.nes.AudioSink
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.mockito.kotlin.*


class ApuTest {
  private val envelope = mock<EnvelopeActive>()
  private val sweep = mock<SweepActive>()
  private val timer = mock<Timer>()
  private val sq1 = Channel(
    synth = mock<SquareSynth>(),
    timer = timer,
    sweep = sweep,
    envelope = envelope
  )
  private val sq2 = Channel(
    synth = mock<SquareSynth>(),
    timer = timer,
    sweep = sweep,
    envelope = envelope
  )
  private val tri = Channel(
    synth = mock<TriangleSynth>(),
    timer = timer,
    sweep = SweepInactive(),
    envelope = EnvelopeInactive(1)
  )
  private val noi = Channel(
    synth = mock<NoiseSynth>(),
    timer = timer,
    sweep = SweepInactive(),
    envelope = envelope
  )
  private val dmc = Channel(
    synth = mock<DmcSynth>(),
    timer = timer,
    sweep = SweepInactive(),
    envelope = EnvelopeInactive(1)
  )
  private val seq = mock<FrameSequencer>()
  private val apu = Apu(
    memory = mock(),
    channels = Channels(
      seq = seq,
      sq1 = sq1,
      sq2 = sq2,
      tri = tri,
      noi = noi,
      dmc = dmc
    ),
    cpuFreqHz = CYCLES_PER_SAMPLE * SAMPLE_RATE_HZ,
    audioSink = object : AudioSink {
      override val sampleRateHz = SAMPLE_RATE_HZ
    }
  )

  @Nested
  inner class Square {
    @Test
    fun `misc - sq1`() {
      assertMisc(0, sq1)
    }

    @Test
    fun `misc - sq2`() {
      assertMisc(4, sq2)
    }

    @Test
    fun `sweep - sq1`() {
      assertSweep(1)
    }

    @Test
    fun `sweep - sq2`() {
      assertSweep(5)
    }

    @Test
    fun `period - sq1`() {
      assertPeriod(2, 3)
    }

    @Test
    fun `period - sq2`() {
      assertPeriod(6, 7)
    }

    @Test
    fun `length - sq1`() {
      assertLength(3, sq1)
    }

    @Test
    fun `length - sq2`() {
      assertLength(7, sq2)
    }

    private fun assertMisc(reg: Int, ctx: Channel<SquareSynth, SweepActive, EnvelopeActive>) {
      apu.writeReg(reg, 0b11_0_0_0000)
      verify(ctx.synth).dutyCycle = 0b11

      apu.writeReg(reg, 0b00_1_0_0000)
      verify(ctx.synth).haltLength = true
      verify(envelope).loop = true

      apu.writeReg(reg, 0b00_0_1_0000)
      verify(envelope).directMode = true

      apu.writeReg(reg, 0b00_0_0_1011)
      verify(envelope).param = 0b1011
    }

    private fun assertSweep(reg: Int) {
      apu.writeReg(reg, 0b1_000_0_000)
      verify(sweep).enabled = true

      apu.writeReg(reg, 0b0_101_0_000)
      verify(sweep).divider = 0b101

      apu.writeReg(reg, 0b0_000_1_000)
      verify(sweep).negate = true

      apu.writeReg(reg, 0b1_000_0_101)
      verify(sweep).shift = 0b101

      verify(sweep, times(4)).restart()
    }

    private fun assertPeriod(regLo: Int, regHi: Int) {
      apu.writeReg(regLo, 0b11001010)
      apu.writeReg(regHi, 0b00000_101)
      verify(timer).periodCycles = 0b000110010110
      verify(timer).periodCycles = 0b101110010110
    }

    private fun assertLength(reg: Int, ctx: Channel<SquareSynth, SweepActive, EnvelopeActive>) {
      apu.writeReg(reg, 0b10101_000)
      verify(ctx.synth).length = 20 // See the length table
      verify(envelope).restart()
    }
  }

  @Nested
  inner class Triangle {
    @Test
    fun misc() {
      apu.writeReg(8, 0b1_0000000)
      verify(tri.synth).haltLength = true
      verify(tri.synth).preventReloadClear = true

      apu.writeReg(8, 0b0_1010101)
      verify(tri.synth).linLength = 0b1010101
    }

    @Test
    fun period() {
      apu.writeReg(10, 0b11001010)
      apu.writeReg(11, 0b00000_101)
      verify(tri.timer).periodCycles = 0b000011001011
      verify(tri.timer).periodCycles = 0b010111001011
    }

    @Test
    fun length() {
      apu.writeReg(11, 0b10101_000)
      verify(tri.synth).length = 20 // See the length table
    }
  }

  @Nested
  inner class Noise {
    @Test
    fun misc() {
      apu.writeReg(12, 0b00_1_0_0000)
      verify(noi.synth).haltLength = true
      verify(noi.envelope).loop = true

      apu.writeReg(12, 0b00_0_1_0000)
      verify(noi.envelope).directMode = true

      apu.writeReg(12, 0b00_0_0_1011)
      verify(noi.envelope).param = 0b1011

      apu.writeReg(14, 0b1_000_0000)
      verify(noi.synth).mode = 1
    }

    @Test
    fun period() {
      apu.writeReg(14, 0b0_000_1001)
      verify(noi.timer).periodCycles = 254 // See lookup table
    }

    @Test
    fun length() {
      apu.writeReg(15, 0b10101_000)
      verify(noi.synth).length = 20 // See the length table
      verify(noi.envelope).restart()
    }
  }

  @Nested
  inner class Dmc {
    @Test
    fun misc() {
      apu.writeReg(16, 0b0_1_00_0000)
      verify(dmc.synth).loop = true
    }

    @Test
    fun period() {
      apu.writeReg(16, 0b0_0_00_1001)
      verify(dmc.timer).periodCycles = 160 // See lookup table
    }

    @Test
    fun `irq enable`() {
      apu.writeReg(16, 0b1_0_00_0000)
      verify(dmc.synth).irqEnabled = true
    }

    @Test
    fun level() {
      apu.writeReg(17, 0b0_1010101)
      verify(dmc.synth).level = 0b1010101
    }

    @Test
    fun address() {
      apu.writeReg(18, 0xCA)
      verify(dmc.synth).address = 0xF280
    }

    @Test
    fun length() {
      apu.writeReg(19, 0xCA)
      verify(dmc.synth).length = 0xCA1
    }
  }

  @Nested
  inner class Status {
    @Test
    fun `enable normal channels`() {
      apu.writeReg(21, 0x1F)
      listOf(sq1, sq2, tri, noi, dmc).forEach {
        verify(it.synth).enabled = true
      }
    }

    @Test
    fun `disable normal channels`() {
      apu.writeReg(21, 0x00)
      listOf(sq1, sq2, tri, noi, dmc).forEach {
        verify(it.synth).enabled = false
      }
    }

    @Test
    fun read() {
      whenever(sq1.synth.outputRemaining) doReturn true
      whenever(sq2.synth.outputRemaining) doReturn false
      whenever(tri.synth.outputRemaining) doReturn false
      whenever(noi.synth.outputRemaining) doReturn true
      whenever(dmc.synth.outputRemaining) doReturn true

      assertEquals(0b000_11001, apu.readStatus())
    }
  }

  @Test
  fun `set sequencer mode`() {
    apu.writeReg(23, 0b1_0000000)
    verify(seq).mode = FIVE_STEP
  }

  @Test
  fun `exposes DMC IRQ status`() {
    whenever(dmc.synth.irq) doReturn true
    assertTrue(apu.irq)

    whenever(dmc.synth.irq) doReturn false
    assertFalse(apu.irq)
  }

  // TODO - generate samples

  companion object {
    private const val SAMPLE_RATE_HZ = 1000
    private val CYCLES_PER_SAMPLE = Rational.of(100)
  }
}
