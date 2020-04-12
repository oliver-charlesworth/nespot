package choliver.nes.debugger

import choliver.nes.Address

sealed class Command {
  data class Step(val num: Int) : Command()
  object Continue : Command()
  sealed class CreateBreakpoint : Command() {
    object Here : CreateBreakpoint()
    object Next : CreateBreakpoint()
    data class At(val addr: Address) : Command()
  }
  data class DeleteBreakpoint(val num: Int) : Command()
  object InfoReg : Command()
  object InfoBreak : Command()
  object Where : Command()
  object Restart : Command()
  object Quit : Command()
  data class Error(val msg: String) : Command()
}
