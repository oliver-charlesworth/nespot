package choliver.nes.debugger

import choliver.nes.Address

sealed class Command {
  data class Step(val num: Int) : Command()
  object Continue : Command()
  object BreakHere : Command()
  object BreakNext : Command()
  data class BreakAt(val addr: Address) : Command()
  object InfoReg : Command()
  object InfoBreak : Command()
  object Where : Command()
  object Restart : Command()
  object Quit : Command()
  data class Error(val msg: String) : Command()
}
