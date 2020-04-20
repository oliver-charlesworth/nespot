package choliver.nes.debugger

import java.io.File

class DebugDonkeyKong {
  companion object {
    @JvmStatic
    fun main(args: Array<String>) = Debugger(
      rom = File("roms/dk.nes").readBytes(),
      stdin = System.`in`,
      stdout = System.out
    ).start()
  }
}
