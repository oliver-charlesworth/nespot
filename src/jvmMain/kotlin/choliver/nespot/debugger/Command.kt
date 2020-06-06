package choliver.nespot.debugger

import choliver.nespot.common.Address
import choliver.nespot.cpu.Opcode
import choliver.nespot.nes.Joypads

sealed class Command {
  object Script : Command()
  object Nop : Command()
  object RunMacro : Command()
  data class Repeat(val times: Int, val cmd: Command) : Command()
  data class SetMacro(val cmd: Command) : Command()

  sealed class Execute : Command() {
    data class Step(val num: Int) : Execute()
    data class Next(val num: Int) : Execute()
    data class Until(val pc: Address) : Execute()
    data class UntilOffset(val offset: Int) : Execute()
    data class UntilOpcode(val op: Opcode) : Execute()
    object UntilNmi : Execute()
    object UntilIrq : Execute()
    object Continue : Execute()
    object Finish : Execute()
  }

  sealed class CreatePoint : Command() {
    sealed class Break : CreatePoint() {
      data class AtOffset(val offset: Int) : Break()
      data class At(val pc: Address) : Break()
    }
    data class Watch(val addr: Address) : CreatePoint()
  }

  sealed class DeletePoint : Command() {
    object All : DeletePoint()
    data class ByNum(val num: Int) : DeletePoint()
  }

  data class CreateDisplay(val addr: Address) : Command()

  sealed class Info : Command() {
    object Stats : Info()
    object Reg : Info()
    object Break : Info()
    object Watch : Info()
    object Display : Info()
    object Backtrace : Info()
    data class Print(val addr: Address) : Info()
    data class InspectInst(val pc: Address, val num: Int) : Info()
    object CpuRam : Info()
    object PpuRam : Info()
    object PpuState : Info()
  }

  object ToggleVerbosity : Command()

  sealed class Event : Command() {
    object Reset : Event()
    object Nmi : Event()
    object Irq : Event()
  }

  sealed class Button : Command() {
    data class Up(val which: Int, val button: Joypads.Button) : Button()
    data class Down(val which: Int, val button: Joypads.Button) : Button()
  }

  object ShowScreen : Command()

  object Quit : Command()
  data class Error(val msg: String) : Command()
}
