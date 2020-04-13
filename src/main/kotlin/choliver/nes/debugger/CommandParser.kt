package choliver.nes.debugger

import choliver.nes.debugger.Command.*
import choliver.nes.debugger.Command.CreatePoint.Break
import choliver.nes.debugger.Command.CreatePoint.Watch
import choliver.nes.debugger.Command.Execute.*
import choliver.nes.sixfiveohtwo.model.toPC
import java.io.InputStream

class CommandParser(stdin: InputStream) {
  private val reader = stdin.bufferedReader()

  fun next(): Command {
    return parseCommand(reader.readLine())
  }

  private fun parseCommand(raw: String): Command {
    val r = raw.trim()
    val bits = if (r.isEmpty()) listOf("next") else r.split("\\s+".toRegex())

    val error = Error("Can't parse: ${r}")

    fun noArgs(cmd: Command) = if (bits.size == 1) cmd else error

    fun <T> T?.oneArg(create: (T) -> Command) = if (this != null) create(this) else error

    return when (bits[0]) {
      "s", "step" -> when (bits.size) {
        1 -> Step(1)
        2 -> bits[1].toIntOrNull().oneArg(::Step)
        else -> error
      }

      "n", "next" -> when (bits.size) {
        1 -> Next(1)
        2 -> bits[1].toIntOrNull().oneArg(::Next)
        else -> error
      }

      "c", "cont" -> noArgs(Continue)

      "f", "finish" -> noArgs(Finish)

      "b", "break" -> when (bits.size) {
        1 -> Break.AtOffset(0)
        2 -> when {
          bits[1].startsWith("+") -> bits[1].removePrefix("+").toIntOrNull().oneArg(Break::AtOffset)
          else -> bits[1].toAddressOrNull().oneArg { Break.At(it.toPC()) }
        }
        else -> error
      }

      "w", "watch" -> when (bits.size) {
        2 -> bits[1].toAddressOrNull().oneArg(::Watch)
        else -> error
      }

      // TODO - clear

      "d", "delete" -> when (bits.size) {
        1 -> DeletePoint.All
        2 -> bits[1].toIntOrNull().oneArg(DeletePoint::ByNum)
        else -> error
      }

      "i", "info" -> when (bits.size) {
        1 -> error
        else -> when (bits[1]) {
          "r", "reg" -> Info.Reg
          "b", "break" -> Info.Break
          "w", "watch" -> Info.Watch
          else -> error
        }
      }

      "p", "print" -> when (bits.size) {
        2 -> bits[1].toAddressOrNull().oneArg(Info::Print)
        else -> error
      }

      "v", "verbosity" -> noArgs(ToggleVerbosity)

      "bt", "backtrace" -> noArgs(Info.Backtrace)

      "r", "reset" -> noArgs(Event.Reset)
      "nmi" -> noArgs(Event.Nmi)
      "irq" -> noArgs(Event.Irq)

      "q", "quit" -> noArgs(Quit)

      else -> error
    }
  }

  private fun String.toAddressOrNull() = removePrefix("0x")
    .toIntOrNull(16)
    ?.let { if (it in 0x0000..0xFFFF) it else null }
}
