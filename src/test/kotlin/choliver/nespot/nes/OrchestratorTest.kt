package choliver.nespot.nes

import choliver.nespot.CYCLES_PER_FRAME
import choliver.nespot.CYCLES_PER_SCANLINE
import choliver.nespot.SAMPLES_PER_SCANLINE
import choliver.nespot.apu.Apu
import choliver.nespot.ppu.Ppu
import choliver.nespot.sixfiveohtwo.Cpu
import com.nhaarman.mockitokotlin2.*
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import kotlin.math.ceil

class OrchestratorTest {
  private val cpu = mock<Cpu>()
  private val apu = mock<Apu>()
  private val ppu = mock<Ppu>()
  private val orchestrator = Orchestrator(cpu, apu, ppu)

  @Test
  fun `executes PPU scanline and generates APU samples exactly at end of scanline`() {
    whenever(cpu.executeStep()) doReturn 4

    // One before end of scanline
    val numSteps = ceil(CYCLES_PER_SCANLINE.toDouble() / 4).toInt() - 1
    repeat(numSteps) { orchestrator.step() }

    verifyZeroInteractions(ppu)
    verifyZeroInteractions(apu)

    // One more step
    orchestrator.step()

    // Now everything happens
    verify(ppu).executeScanline()
    verify(apu, times(ceil(SAMPLES_PER_SCANLINE.toDouble()).toInt())).generateSample()
  }

  @Test
  fun `sets EOF exactly at end of frame`() {
    whenever(cpu.executeStep()) doReturn 1

    // One before end of frame
    val numSteps = ceil(CYCLES_PER_FRAME.toDouble()).toInt() - 1
    repeat(numSteps) { orchestrator.step() }

    assertFalse(orchestrator.endOfFrame)

    // One more step
    orchestrator.step()

    assertTrue(orchestrator.endOfFrame)
  }
}
