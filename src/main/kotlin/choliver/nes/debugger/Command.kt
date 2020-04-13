package choliver.nes.debugger

import choliver.nes.Address
import choliver.nes.sixfiveohtwo.model.Opcode
import choliver.nes.sixfiveohtwo.model.ProgramCounter

sealed class Command {
  object Script : Command()
  object Nop : Command()

  sealed class Execute : Command() {
    data class Step(val num: Int) : Execute()
    data class Next(val num: Int) : Execute()
    data class Until(val pc: ProgramCounter) : Execute()
    data class UntilOffset(val offset: Int) : Execute()
    data class UntilOpcode(val op: Opcode) : Execute()
    object Continue : Execute()
    object Finish : Execute()
  }

  sealed class CreatePoint : Command() {
    sealed class Break : CreatePoint() {
      data class AtOffset(val offset: Int) : Break()
      data class At(val pc: ProgramCounter) : Break()
    }
    data class Watch(val addr: Address) : CreatePoint()
  }

  sealed class DeletePoint : Command() {
    object All : DeletePoint()
    data class ByNum(val num: Int) : DeletePoint()
  }

  data class CreateDisplay(val addr: Address) : Command()

  sealed class Info : Command() {
    object Reg : Info()
    object Break : Info()
    object Watch : Info()
    object Display : Info()
    object Backtrace : Info()
    data class Print(val addr: Address) : Info()
    data class InspectInst(val pc: ProgramCounter, val num: Int) : Info()
    object CpuRam : Info()
    object PpuRam : Info()
  }

  object ToggleVerbosity : Command()

  sealed class Event : Command() {
    object Reset : Event()
    object Nmi : Event()
    object Irq : Event()
  }
  object Quit : Command()
  data class Error(val msg: String) : Command()
}
