package choliver.nespot.debugger

import java.io.File

class DebugSuperMarioBros {
  companion object {
    @JvmStatic
    fun main(args: Array<String>) = Debugger(
      rom = File("roms/smb.nes").readBytes(),
      stdin = System.`in`,
      stdout = System.out,
      script = File("scripts/smb.script").readText()
    ).start()
  }
}
