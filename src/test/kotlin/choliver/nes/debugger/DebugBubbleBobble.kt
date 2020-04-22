package choliver.nes.debugger

import java.io.File

class DebugBubbleBobble {
  companion object {
    @JvmStatic
    fun main(args: Array<String>) = Debugger(
      rom = File("roms/bb.nes").readBytes(),
      stdin = System.`in`,
      stdout = System.out
    ).start()
  }
}
