package choliver.nespot.nes

import choliver.nespot.CYCLES_PER_SCANLINE
import choliver.nespot.Rational
import choliver.nespot.apu.Apu
import choliver.nespot.ppu.Ppu
import choliver.nespot.sixfiveohtwo.Cpu
import com.nhaarman.mockitokotlin2.*
import org.junit.jupiter.api.Test
import kotlin.math.ceil


class SequencerTest {
  private val cpu = mock<Cpu>()
  private val apu = mock<Apu>()
  private val ppu = mock<Ppu>()
  private val sequencer = Sequencer(cpu, apu, ppu)

  @Test
  fun `executes APU sample generation continuously`() {
    whenever(cpu.executeStep()) doReturnConsecutively listOf(5, 2, 3, 4)

    repeat(4) { sequencer.step() }

    inOrder(apu) {
      verify(apu).advance(5)
      verify(apu).advance(2)
      verify(apu).advance(3)
      verify(apu).advance(4)
    }
  }

  @Test
  fun `executes PPU scanline exactly at end of scanline`() {
    whenever(cpu.executeStep()) doReturn 4

    repeat((CYCLES_PER_SCANLINE / 4).roundUp() - 1) { sequencer.step() } // One before end of scanline

    verifyZeroInteractions(ppu)     // Not quite enough

    sequencer.step()                // One more step

    verify(ppu).executeScanline()   // Oh yes
  }

  private fun Rational.roundUp() = ceil(toDouble()).toInt()
}
