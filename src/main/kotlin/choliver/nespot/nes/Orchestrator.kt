package choliver.nespot.nes

import choliver.nespot.CYCLES_PER_SCANLINE
import choliver.nespot.SAMPLES_PER_SCANLINE
import choliver.nespot.SCANLINES_PER_FRAME
import choliver.nespot.apu.Apu
import choliver.nespot.ppu.Ppu
import choliver.nespot.sixfiveohtwo.Cpu

internal class Orchestrator(
  private val cpu: Cpu,
  private val apu: Apu,
  private val ppu: Ppu
) {
  private var cyclesRemainingInScanline = CYCLES_PER_SCANLINE
  private var samplesRemainingInScanline = SAMPLES_PER_SCANLINE
  private var scanlinesRemainingInFrame = SCANLINES_PER_FRAME
  private var _endOfFrame = false

  val endOfFrame get() = _endOfFrame

  fun step() {
    _endOfFrame = false
    cyclesRemainingInScanline -= cpu.executeStep()
    if (cyclesRemainingInScanline <= 0) {
      finishScanline()
    }
  }

  private fun finishScanline() {
    cyclesRemainingInScanline += CYCLES_PER_SCANLINE
    ppu.executeScanline()
    generateSamplesForScanline()
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
    _endOfFrame = true
  }

  private fun generateSamplesForScanline() {
    while (samplesRemainingInScanline > 0) {
      apu.generateSample()
      samplesRemainingInScanline -= 1
    }
    samplesRemainingInScanline += SAMPLES_PER_SCANLINE
  }
}
