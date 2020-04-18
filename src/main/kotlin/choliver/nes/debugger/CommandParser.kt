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
      r.isEmpty() && !ignoreBlanks -> return RunMacro
      r.isEmpty() && ignoreBlanks -> return Nop
      r.startsWith("#") -> return Nop
    }

    val tokens = r.split("\\s+".toRegex())

    return parseTokens(tokens) ?: Error("Can't parse: ${r}")
  }

  private fun parseTokens(tokens: List<String>): Command? {
    fun noArgs(cmd: Command) = if (tokens.size == 1) cmd else null

    return when (tokens[0]) {
      "script" -> noArgs(Script)

      "macro" -> when (tokens.size) {
        1 -> null
        else -> parseTokens(tokens.drop(1))?.let(::SetMacro)
      }

      "s", "step" -> when (tokens.size) {
        1 -> Step(1)
        2 -> tokens[1].toIntOrNull()?.let(::Step)
        else -> null
      }

      "n", "next" -> when (tokens.size) {
        1 -> Next(1)
        2 -> tokens[1].toIntOrNull()?.let(::Next)
        else -> null
      }

      "u", "until" -> when (tokens.size) {
        2 -> when {
          tokens[1].startsWith("+") -> tokens[1].removePrefix("+").toIntOrNull()?.let(::UntilOffset)
          tokens[1].startsWith("0x") -> tokens[1].toPcOrNull()?.let(::Until)
          else -> tokens[1].toOpcodeOrNull()?.let(::UntilOpcode)
        }
        else -> null
      }

      "c", "cont" -> noArgs(Continue)

      "f", "finish" -> noArgs(Finish)

      "b", "break" -> when (tokens.size) {
        1 -> Break.AtOffset(0)
        2 -> when {
          tokens[1].startsWith("+") -> tokens[1].removePrefix("+").toIntOrNull()?.let(Break::AtOffset)
          else -> tokens[1].toPcOrNull()?.let(Break::At)
        }
        else -> null
      }

      "w", "watch" -> when (tokens.size) {
        2 -> tokens[1].toAddressOrNull()?.let(::Watch)
        else -> null
      }

      "display" -> when (tokens.size) {
        2 -> tokens[1].toAddressOrNull()?.let(::CreateDisplay)
        else -> null
      }

      // TODO - clear

      "d", "delete" -> when (tokens.size) {
        1 -> DeletePoint.All
        2 -> tokens[1].toIntOrNull()?.let(DeletePoint::ByNum)
        else -> null
      }

      "i", "info" -> when (tokens.size) {
        1 -> null
        else -> when (tokens[1]) {
          "s", "stats" -> Info.Stats
          "r", "reg" -> Info.Reg
          "b", "break" -> Info.Break
          "w", "watch" -> Info.Watch
          "d", "display" -> Info.Display
          "ram" -> Info.CpuRam
          "vram" -> Info.PpuRam
          else -> null
        }
      }

      "p", "print" -> when (tokens.size) {
        2 -> tokens[1].toAddressOrNull()?.let(Info::Print)
        else -> null
      }

      "xi" -> when (tokens.size) {
        2, 3 -> {
          tokens[1].toPcOrNull()?.let { pc ->
            when (tokens.size) {
              2 -> Info.InspectInst(pc, 1)
              3 -> tokens[2].toIntOrNull()?.let { Info.InspectInst(pc, it) }
              else -> null
            }
          }
        }
        else -> null
      }

      "v", "verbosity" -> noArgs(ToggleVerbosity)

      "bt", "backtrace" -> noArgs(Info.Backtrace)

      "r", "reset" -> noArgs(Event.Reset)
      "nmi" -> noArgs(Event.Nmi)
      "irq" -> noArgs(Event.Irq)

      "screen" -> noArgs(ShowScreen)

      "q", "quit" -> noArgs(Quit)

      else -> null
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
