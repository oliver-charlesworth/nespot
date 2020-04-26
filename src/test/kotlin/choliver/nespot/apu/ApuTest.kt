package choliver.nespot.apu

import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.times
import com.nhaarman.mockitokotlin2.verify
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

class ApuTest {
  private val sq1 = SynthContext(synth = mock<SquareSynth>(), envelope = mock(), sweep = mock(), timer = mock())
  private val sq2 = SynthContext(synth = mock<SquareSynth>(), envelope = mock(), sweep = mock(), timer = mock())
  private val tri = SynthContext(synth = mock<TriangleSynth>(), timer = mock())
  private val noi = SynthContext(synth = mock<NoiseSynth>(), envelope = mock(), timer = mock())
  private val dmc = SynthContext(synth = mock<DmcSynth>(), timer = mock())
  private val apu = Apu(
    buffer = ByteArray(0),
    memory = mock(),
    channels = Channels(
      sq1 = sq1,
      sq2 = sq2,
      tri = tri,
      noi = noi,
      dmc = dmc
    )
  )

  @Nested
  inner class Square {
    @Test
    fun misc() {
      assertMisc(0, sq1)
      assertMisc(4, sq2)
    }

    @Test
    fun sweep() {
      assertSweep(1, sq1)
      assertSweep(5, sq2)
    }

    @Test
    fun period() {
      assertPeriod(2, 3, sq1)
      assertPeriod(6, 7, sq2)
    }

    @Test
    fun length() {
      assertLength(3, sq1)
      assertLength(7, sq2)
    }

    private fun assertMisc(reg: Int, ctx: SynthContext<SquareSynth>) {
      apu.writeReg(reg, 0b11_0_0_0000)
      verify(ctx.synth).dutyCycle = 0b11

      apu.writeReg(reg, 0b00_1_0_0000)
      verify(ctx.synth).haltLength = true
      verify(ctx.envelope).loop = true

      apu.writeReg(reg, 0b00_0_1_0000)
      verify(ctx.envelope).directMode = true

      apu.writeReg(reg, 0b00_0_0_1011)
      verify(ctx.envelope).param = 0b1011
    }

    private fun assertSweep(reg: Int, ctx: SynthContext<SquareSynth>) {
      apu.writeReg(reg, 0b1_000_0_000)
      verify(ctx.sweep).enabled = true

      apu.writeReg(reg, 0b0_101_0_000)
      verify(ctx.sweep).divider = 0b101

      apu.writeReg(reg, 0b0_000_1_000)
      verify(ctx.sweep).negate = true

      apu.writeReg(reg, 0b1_000_0_101)
      verify(ctx.sweep).shift = 0b101

      verify(ctx.sweep, times(4)).reset()
    }

    private fun assertPeriod(regLo: Int, regHi: Int, ctx: SynthContext<*>) {
      apu.writeReg(regLo, 0b11001010)
      apu.writeReg(regHi, 0b00000_101)
      verify(ctx.timer).periodCycles = 0b000110010110.toRational()
      verify(ctx.timer).periodCycles = 0b101110010110.toRational()
    }

    private fun assertLength(reg: Int, ctx: SynthContext<*>) {
      apu.writeReg(reg, 0b10101_000)
      verify(ctx.synth).length = 20 // See the length table
      verify(ctx.envelope).reset()
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
      verify(tri.synth).linear = 0b1010101
    }

    @Test
    fun period() {
      apu.writeReg(10, 0b11001010)
      apu.writeReg(11, 0b00000_101)
      verify(tri.timer).periodCycles = 0b000011001011.toRational()
      verify(tri.timer).periodCycles = 0b010111001011.toRational()
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
      verify(noi.timer).periodCycles = 254.toRational() // See lookup table
    }

    @Test
    fun length() {
      apu.writeReg(15, 0b10101_000)
      verify(noi.synth).length = 20 // See the length table
      verify(noi.envelope).reset()
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
      verify(dmc.timer).periodCycles = 160.toRational() // See lookup table
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
}
