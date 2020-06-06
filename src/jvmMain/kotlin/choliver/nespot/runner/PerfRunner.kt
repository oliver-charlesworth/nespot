package choliver.nespot.runner

import choliver.nespot.SCREEN_HEIGHT
import choliver.nespot.SCREEN_WIDTH
import choliver.nespot.cartridge.Rom
import choliver.nespot.nes.Nes
import choliver.nespot.nes.VideoSink
import kotlin.math.roundToInt
import kotlin.system.measureTimeMillis

class PerfRunner(
  rom: Rom,
  private val numFrames: Int
) {
  private val nes = Nes(
    rom = rom,
    videoSink = object : VideoSink {
      var i = 0
      override fun put(color: Int) {
        if (++i == SCREEN_WIDTH * SCREEN_HEIGHT) {
          endOfFrame = true
          i = 0
        }
      }
    }
  )
  private var endOfFrame = false

  fun run() {
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
}
