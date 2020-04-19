package choliver.nes.debugger

import choliver.nes.runner.Runner

class RunDonkeyKong {
  companion object {
    @JvmStatic
    fun main(args: Array<String>) = Runner(
      rom = {}.javaClass.getResource("/dk.nes").readBytes()
    ).start()
  }
}
