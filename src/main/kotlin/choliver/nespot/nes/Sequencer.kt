package choliver.nespot.nes

import choliver.nespot.CYCLES_PER_SCANLINE
import choliver.nespot.MutableForPerfReasons
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
    var untilNextScanline: Int = CYCLES_PER_SCANLINE.a
  )

  private var state = State()

  fun step() = with(state) {
    val cycles = cpu.executeStep()
    untilNextScanline -= cycles * CYCLES_PER_SCANLINE.b

    apu.advance(cycles)

    if (untilNextScanline <= 0) {
      untilNextScanline += CYCLES_PER_SCANLINE.a
      ppu.executeScanline()
    }
  }

  inner class Diagnostics internal constructor() {
    var state
      get() = this@Sequencer.state
      set(value) { this@Sequencer.state = value.copy() }

    fun step() = this@Sequencer.step()
  }

  val diagnostics = Diagnostics()
}
