package choliver.nes.debugger

import choliver.nes.runner.Runner

class RunSuperMarioBros {
  companion object {
    @JvmStatic
    fun main(args: Array<String>) = Runner(
      rom = {}.javaClass.getResource("/smb.nes").readBytes()
    ).start()
  }
}
