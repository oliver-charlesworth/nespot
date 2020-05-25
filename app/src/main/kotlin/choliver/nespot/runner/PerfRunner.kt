package choliver.nespot.runner

import choliver.nespot.cartridge.Rom
import choliver.nespot.cpu.Cpu.NextStep.RESET
import choliver.nespot.nes.Joypads
import choliver.nespot.nes.Nes
import choliver.nespot.ppu.VideoSink
import kotlin.math.roundToInt
import kotlin.system.measureTimeMillis


class PerfRunner(
  rom: Rom,
  private val numFrames: Int
) {
  private val joypads = Joypads()
  private val nes = Nes(
    sampleRateHz = 44100,
    rom = rom,
    joypads = joypads,
    videoSink = object : VideoSink {
      override fun set(idx: Int, color: Int) {}
      override fun commit() {}
    }
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
