package choliver.nespot.nes

import choliver.nespot.CYCLES_PER_SCANLINE
import choliver.nespot.SAMPLES_PER_SCANLINE
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
  private var _endOfFrame = false

  val endOfFrame get() = _endOfFrame

  fun runToEndOfFrame() {
    do step() while (!_endOfFrame)
  }

  fun step() {
    _endOfFrame = false
    cyclesRemainingInScanline -= cpu.executeStep()
    if (cyclesRemainingInScanline < 0) {
      finishScanline()
    }
  }

  private fun finishScanline() {
    cyclesRemainingInScanline += CYCLES_PER_SCANLINE
    ppu.executeScanline()
    generateSamplesForScanline()
    if (ppu.scanline == 0) {
      finishFrame()
    }
  }

  private fun finishFrame() {
    if (apu.iSample != 0) {
      throw IllegalStateException("Unexpected APU/PPU rate mismatch")
    }
    _endOfFrame = true
  }

  private fun generateSamplesForScanline() {
    while (samplesRemainingInScanline > 0) {
      apu.next()
      samplesRemainingInScanline -= 1
    }
    samplesRemainingInScanline += SAMPLES_PER_SCANLINE
  }
}
