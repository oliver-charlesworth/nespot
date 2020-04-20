package choliver.nes.debugger

class DebugDonkeyKong {
  companion object {
    @JvmStatic
    fun main(args: Array<String>) = Debugger(
      rom = {}.javaClass.getResource("").readBytes(),
      stdin = System.`in`,
      stdout = System.out
    ).start()
  }
}
