package choliver.nes.debugger

class DebugSuperMarioBros {
  companion object {
    @JvmStatic
    fun main(args: Array<String>) = Debugger(
      rom = {}.javaClass.getResource("").readBytes(),
      stdin = System.`in`,
      stdout = System.out,
      script = {}.javaClass.getResource("m u nmi\nv\nscreen\n\n# Get to complete title screen\nrep 30 m\n\nq\n\n# Start !\ndown 1 start\nrep 20 m\nup 1 start\n\n# Wait until we get to regular gamefield\nrep 150 m\n\ndown 1 right\nrep 25 m\ndown 1 a\nrep 25 m\nup 1 a\nup 1 right\ndown 1 left\nrep 50 m\nup 1 left\n\n\n").readText()
    ).start()
  }
}
