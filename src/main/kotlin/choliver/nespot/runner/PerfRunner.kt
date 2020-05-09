package choliver.nespot.runner

import choliver.nespot.cartridge.Rom
import choliver.nespot.nes.Nes
import choliver.nespot.sixfiveohtwo.Cpu.NextStep.RESET
import kotlin.math.roundToInt
import kotlin.system.measureTimeMillis


class PerfRunner(
  rom: Rom,
  private val numFrames: Int
) {
  private val joypads = FakeJoypads()
  private val nes = Nes(
    rom = rom,
    joypads = joypads,
    onVideoBufferReady = { endOfFrame = true }
  )
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
