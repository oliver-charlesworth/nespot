package choliver.nespot.nes

import choliver.nespot.CYCLES_PER_FRAME
import choliver.nespot.CYCLES_PER_SAMPLE
import choliver.nespot.CYCLES_PER_SCANLINE
import choliver.nespot.Rational
import choliver.nespot.apu.Apu
import choliver.nespot.ppu.Ppu
import choliver.nespot.sixfiveohtwo.Cpu
import com.nhaarman.mockitokotlin2.*
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import kotlin.math.ceil

class SequencerTest {
  private val cpu = mock<Cpu>()
  private val apu = mock<Apu>()
  private val ppu = mock<Ppu>()
  private val onAudioBufferReady = mock<() -> Unit>()
  private val onVideoBufferReady = mock<() -> Unit>()
  private val sequencer = Sequencer(
    cpu = cpu,
    apu = apu,
    ppu = ppu,
    onAudioBufferReady = onAudioBufferReady,
    onVideoBufferReady = onVideoBufferReady
  )

  @Test
  fun `executes APU sample generation continuously`() {
    whenever(cpu.executeStep()) doReturnConsecutively listOf(
      CYCLES_PER_SAMPLE.roundUp() - 1,
      1
    )

    sequencer.step()

    verifyZeroInteractions(apu)   // Not quite enough

    sequencer.step()              // One more cycle

    verify(apu).generateSample()  // Oh yes
  }

  @Test
  fun `executes PPU scanline exactly at end of scanline`() {
    whenever(cpu.executeStep()) doReturn 4

    repeat((CYCLES_PER_SCANLINE / 4).roundUp() - 1) { sequencer.step() } // One before end of scanline

    verifyZeroInteractions(ppu)     // Not quite enough

    sequencer.step()                // One more step

    verify(ppu).executeScanline()   // Oh yes
  }

  @Test
  fun `sets EOF and invokes callbacks exactly at end of frame`() {
    whenever(cpu.executeStep()) doReturn 1

    repeat(CYCLES_PER_FRAME.roundUp() - 2) { sequencer.step() }  // Two before end of frame

    assertFalse(sequencer.step())  // Not quite enough
    verifyZeroInteractions(onAudioBufferReady)
    verifyZeroInteractions(onVideoBufferReady)

    assertTrue(sequencer.step())   // Oh yes
    verify(onAudioBufferReady)()
    verify(onVideoBufferReady)()
  }

  private fun Rational.roundUp() = ceil(toDouble()).toInt()
}
