package choliver.nespot.nes

import choliver.nespot.*
import choliver.nespot.apu.Apu
import choliver.nespot.ppu.Ppu
import choliver.nespot.sixfiveohtwo.Cpu

class Sequencer(
  private val cpu: Cpu,
  private val apu: Apu,
  private val ppu: Ppu
) {
  @MutableForPerfReasons
  data class State(
    var cyclesTilNextSampleCounter: Int = CYCLES_PER_SAMPLE.a,
    var cyclesRemainingInScanlineCounter: Int = CYCLES_PER_SCANLINE.a,
    var scanlinesRemainingInFrame: Int = SCANLINES_PER_FRAME
  )

  private var state = State()

  fun step() = with(state) {
    val cycles = cpu.executeStep()
    cyclesRemainingInScanlineCounter -= cycles * CYCLES_PER_SCANLINE.b
    cyclesTilNextSampleCounter -= cycles * CYCLES_PER_SAMPLE.b

    if (cyclesTilNextSampleCounter <= 0) {
      generateSample()
    }

    if (cyclesRemainingInScanlineCounter <= 0) {
      finishScanline()
    }

    if (scanlinesRemainingInFrame == 0) {
      finishFrame()
    }
  }

  private fun generateSample() = with(state) {
    apu.generateSample()
    cyclesTilNextSampleCounter += CYCLES_PER_SAMPLE.a
  }

  private fun finishScanline() = with(state) {
    cyclesRemainingInScanlineCounter += CYCLES_PER_SCANLINE.a
    ppu.executeScanline()
    scanlinesRemainingInFrame--
  }

  private fun finishFrame() = with(state) {
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
