package choliver.nespot.nes

import choliver.nespot.CYCLES_PER_SAMPLE
import choliver.nespot.CYCLES_PER_SCANLINE
import choliver.nespot.SCANLINES_PER_FRAME
import choliver.nespot.apu.Apu
import choliver.nespot.ppu.Ppu
import choliver.nespot.sixfiveohtwo.Cpu

class Sequencer(
  private val cpu: Cpu,
  private val apu: Apu,
  private val ppu: Ppu
) {
  private var cyclesRemainingInScanline = CYCLES_PER_SCANLINE
  private var scanlinesRemainingInFrame = SCANLINES_PER_FRAME
  private var cyclesTilNextSample = CYCLES_PER_SAMPLE
  private var endOfFrame = false

  fun runToEndOfFrame() {
    do step() while (!endOfFrame)
  }

  fun step() {
    endOfFrame = false
    val cycles = cpu.executeStep()
    cyclesRemainingInScanline -= cycles
    cyclesTilNextSample -= cycles

    if (cyclesTilNextSample <= 0) {
      apu.generateSample()
      cyclesTilNextSample += CYCLES_PER_SAMPLE
    }

    if (cyclesRemainingInScanline <= 0) {
      finishScanline()
    }
  }

  private fun finishScanline() {
    cyclesRemainingInScanline += CYCLES_PER_SCANLINE
    ppu.executeScanline()
    scanlinesRemainingInFrame--
    if (scanlinesRemainingInFrame == 0) {
      finishFrame()
    }
  }

  private fun finishFrame() {
    if (apu.iSample != 0 || ppu.scanline != 0) {
      throw IllegalStateException("Unexpected APU/PPU rate mismatch")
    }
    scanlinesRemainingInFrame = SCANLINES_PER_FRAME
    endOfFrame = true
  }

  inner class Diagnostics internal constructor() {

    // TODO - state
//    var state
//      get() = _state
//      set(value) { _state = value.copy() }
    val endOfFrame get() = this@Sequencer.endOfFrame
    fun step() = this@Sequencer.step()
  }

  val diagnostics = Diagnostics()
}
