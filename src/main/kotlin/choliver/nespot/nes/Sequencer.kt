package choliver.nespot.nes

import choliver.nespot.*
import choliver.nespot.apu.Apu
import choliver.nespot.ppu.Ppu
import choliver.nespot.sixfiveohtwo.Cpu

class Sequencer(
  private val cpu: Cpu,
  private val apu: Apu,
  private val ppu: Ppu,
  private val onAudioBufferReady: () -> Unit
) {
  @MutableForPerfReasons
  data class State(
    var cyclesTilNextSample: Rational = CYCLES_PER_SAMPLE,
    var cyclesRemainingInScanline: Rational = CYCLES_PER_SCANLINE,
    var scanlinesRemainingInFrame: Int = SCANLINES_PER_FRAME
  )

  private var state = State()

  fun runToEndOfFrame() {
    @Suppress("ControlFlowWithEmptyBody")
    while (!step()) {}
  }

  fun step() = with(state) {
    val cycles = cpu.executeStep()
    cyclesRemainingInScanline -= cycles
    cyclesTilNextSample -= cycles

    if (cyclesTilNextSample <= 0) {
      generateSample()
    }

    if (cyclesRemainingInScanline <= 0) {
      finishScanline()
    }

    if (scanlinesRemainingInFrame == 0) {
      finishFrame()
      true
    } else {
      false
    }
  }

  private fun generateSample() = with(state) {
    apu.generateSample()
    cyclesTilNextSample += CYCLES_PER_SAMPLE
  }

  private fun finishScanline() = with(state) {
    cyclesRemainingInScanline += CYCLES_PER_SCANLINE
    ppu.executeScanline()
    scanlinesRemainingInFrame--
  }

  private fun finishFrame() = with(state) {
    onAudioBufferReady()  // TODO - this should be driven by APU
    if (apu.iSample != 0 || ppu.scanline != 0) {
      throw IllegalStateException("Unexpected APU/PPU rate mismatch")
    }
    scanlinesRemainingInFrame = SCANLINES_PER_FRAME
  }

  inner class Diagnostics internal constructor() {
    var state
      get() = this@Sequencer.state
      set(value) { this@Sequencer.state = value.copy() }

    fun step() = this@Sequencer.step()
  }

  val diagnostics = Diagnostics()
}
