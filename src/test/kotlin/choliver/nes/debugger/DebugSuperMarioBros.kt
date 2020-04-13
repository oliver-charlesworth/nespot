package choliver.nes.debugger

class DebugSuperMarioBros {
  companion object {
    @JvmStatic
    fun main(args: Array<String>) = Debugger(
      rom = {}.javaClass.getResource("/smb.nes").readBytes(),
      stdin = System.`in`,
      stdout = System.out,
      script = {}.javaClass.getResource("/smb.script").readText()
    ).start()
  }
}
