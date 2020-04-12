package choliver.nes.debugger

import choliver.nes.Address
import choliver.nes.sixfiveohtwo.model.ProgramCounter

sealed class Command {
  sealed class Execute : Command() {
    data class Step(val num: Int) : Execute()
    object Continue : Execute()
    object Finish : Execute()
  }

  sealed class CreatePoint : Command() {
    sealed class Break : CreatePoint() {
      object Here : Break()
      object AtNext : Break()
      data class At(val pc: ProgramCounter) : Break()
    }
    data class Watch(val addr: Address) : CreatePoint()
  }

  sealed class DeletePoint : Command() {
    object All : DeletePoint()
    data class ByNum(val num: Int) : DeletePoint()
  }

  sealed class Info : Command() {
    object Reg : Info()
    object Break : Info()
    object Watch : Info()
    object Backtrace : Info()
    data class Print(val addr: Address) : Info()
  }

  object ToggleVerbosity : Command()
  object Restart : Command()
  object Quit : Command()
  data class Error(val msg: String) : Command()
}
