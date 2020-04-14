package choliver.nes.debugger

import choliver.nes.debugger.Command.*
import choliver.nes.debugger.Command.CreatePoint.Break
import choliver.nes.debugger.Command.CreatePoint.Watch
import choliver.nes.debugger.Command.Execute.*
import choliver.nes.sixfiveohtwo.model.Opcode
import choliver.nes.sixfiveohtwo.model.toPC
import java.io.InputStream

class CommandParser(
  stdin: InputStream,
  private val ignoreBlanks: Boolean = false
) {
  private val reader = stdin.bufferedReader()

  fun next(): Command {
    return parseCommand(reader.readLine())
  }

  private fun parseCommand(raw: String?): Command {
    if (raw == null) {
      return Quit
    }
    val r = raw.trim()
    when {
      r.isEmpty() && !ignoreBlanks -> return Next(1)
      r.isEmpty() && ignoreBlanks -> return Nop
      r.startsWith("#") -> return Nop
    }

    val bits = r.split("\\s+".toRegex())

    val error = Error("Can't parse: ${r}")
    fun noArgs(cmd: Command) = if (bits.size == 1) cmd else error
    fun <T> T?.oneArg(create: (T) -> Command) = if (this != null) create(this) else error

    return when (bits[0]) {
      "script" -> noArgs(Script)

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

      "u", "until" -> when (bits.size) {
        2 -> when {
          bits[1].startsWith("+") -> bits[1].removePrefix("+").toIntOrNull().oneArg(::UntilOffset)
          bits[1].startsWith("0x") -> bits[1].toPcOrNull().oneArg(::Until)
          else -> bits[1].toOpcodeOrNull().oneArg(::UntilOpcode)
        }
        else -> error
      }

      "c", "cont" -> noArgs(Continue)

      "f", "finish" -> noArgs(Finish)

      "b", "break" -> when (bits.size) {
        1 -> Break.AtOffset(0)
        2 -> when {
          bits[1].startsWith("+") -> bits[1].removePrefix("+").toIntOrNull().oneArg(Break::AtOffset)
          else -> bits[1].toPcOrNull().oneArg(Break::At)
        }
        else -> error
      }

      "w", "watch" -> when (bits.size) {
        2 -> bits[1].toAddressOrNull().oneArg(::Watch)
        else -> error
      }

      "display" -> when (bits.size) {
        2 -> bits[1].toAddressOrNull().oneArg(::CreateDisplay)
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
          "d", "display" -> Info.Display
          "ram" -> Info.CpuRam
          "vram" -> Info.PpuRam
          else -> error
        }
      }

      "p", "print" -> when (bits.size) {
        2 -> bits[1].toAddressOrNull().oneArg(Info::Print)
        else -> error
      }

      "xi" -> when (bits.size) {
        2, 3 -> {
          bits[1].toPcOrNull()?.let { pc ->
            when (bits.size) {
              2 -> Info.InspectInst(pc, 1)
              3 -> bits[2].toIntOrNull()?.let { Info.InspectInst(pc, it) }
              else -> error
            }
          } ?: error
        }
        else -> error
      }

      "v", "verbosity" -> noArgs(ToggleVerbosity)

      "bt", "backtrace" -> noArgs(Info.Backtrace)

      "r", "reset" -> noArgs(Event.Reset)
      "nmi" -> noArgs(Event.Nmi)
      "irq" -> noArgs(Event.Irq)

      "render" -> noArgs(Render)

      "q", "quit" -> noArgs(Quit)

      else -> error
    }
  }

  private fun String.toOpcodeOrNull() = try {
    Opcode.valueOf(this.toUpperCase())
  } catch (_: IllegalArgumentException) {
    null
  }

  private fun String.toPcOrNull() = toAddressOrNull()?.toPC()

  private fun String.toAddressOrNull() = removePrefix("0x")
    .toIntOrNull(16)
    ?.let { if (it in 0x0000..0xFFFF) it else null }
}
