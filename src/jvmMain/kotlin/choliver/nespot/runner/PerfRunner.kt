package choliver.nespot.runner

import choliver.nespot.cartridge.Rom
import choliver.nespot.cpu.Cpu.NextStep.RESET
import choliver.nespot.nes.Nes
import kotlin.math.roundToInt
import kotlin.system.measureTimeMillis


class PerfRunner(
  rom: Rom,
  private val numFrames: Int
) {
  private val nes = Nes(rom = rom)
  private var endOfFrame = false

  fun run() {
    reset()

    val runtimeMs = measureTimeMillis {
      repeat(numFrames) {
        while (!endOfFrame) {
          nes.step()
        }
        endOfFrame = false
      }
    }
    println("Ran ${numFrames} frames in ${runtimeMs} ms (${(numFrames * 1000.0 / runtimeMs).roundToInt()} fps)")
  }

  private fun reset() {
    nes.diagnostics.cpu.nextStep = RESET
  }
}