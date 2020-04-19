package choliver.nes.debugger

import choliver.nes.Joypads
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

      "rep", "repeat" -> when (tokens.size) {
        1, 2 -> null
        else -> cmd(::Repeat, tokens[1].toIntOrNull(), parseTokens(tokens.drop(2)))
      }

      "m", "macro" -> when (tokens.size) {
        1 -> RunMacro
        else -> cmd(::SetMacro, parseTokens(tokens.drop(1)))
      }

      "s", "step" -> when (tokens.size) {
        1 -> Step(1)
        2 -> tokens[1].toIntOrNull()?.let(::Step)
        else -> null
      }

      "n", "next" -> when (tokens.size) {
        1 -> Next(1)
        2 -> cmd(::Next, tokens[1].toIntOrNull())
        else -> null
      }

      "u", "until" -> when (tokens.size) {
        2 -> when {
          tokens[1] == "nmi" -> UntilNmi
          tokens[1].startsWith("+") -> cmd(::UntilOffset, tokens[1].removePrefix("+").toIntOrNull())
          tokens[1].startsWith("0x") -> cmd(::Until, tokens[1].toPcOrNull())
          else -> cmd(::UntilOpcode, tokens[1].toEnumOrNull<Opcode>())
        }
        else -> null
      }

      "c", "cont" -> noArgs(Continue)

      "f", "finish" -> noArgs(Finish)

      "b", "break" -> when (tokens.size) {
        1 -> Break.AtOffset(0)
        2 -> when {
          tokens[1].startsWith("+") -> cmd(Break::AtOffset, tokens[1].removePrefix("+").toIntOrNull())
          else -> cmd(Break::At, tokens[1].toPcOrNull())
        }
        else -> null
      }

      "w", "watch" -> when (tokens.size) {
        2 -> cmd(::Watch, tokens[1].toAddressOrNull())
        else -> null
      }

      "display" -> when (tokens.size) {
        2 -> cmd(::CreateDisplay, tokens[1].toAddressOrNull())
        else -> null
      }

      // TODO - clear

      "d", "delete" -> when (tokens.size) {
        1 -> DeletePoint.All
        2 -> cmd(DeletePoint::ByNum, tokens[1].toIntOrNull())
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
        2 -> cmd(Info::Print, tokens[1].toAddressOrNull())
        else -> null
      }

      "xi" -> when (tokens.size) {
        2, 3 -> {
          tokens[1].toPcOrNull()?.let { pc ->
            when (tokens.size) {
              2 -> Info.InspectInst(pc, 1)
              3 -> cmd(Info::InspectInst, pc, tokens[2].toIntOrNull())
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

      "up" -> when (tokens.size) {
        3 -> parseButtonArgs(tokens, Button::Up)
        else -> null
      }

      "down" -> when (tokens.size) {
        3 -> parseButtonArgs(tokens, Button::Down)
        else -> null
      }

      "screen" -> noArgs(ShowScreen)

      "q", "quit" -> noArgs(Quit)

      else -> null
    }
  }

  private fun parseButtonArgs(tokens: List<String>, create: (Int, Joypads.Button) -> Button): Button? = cmd(
    create,
    tokens[1].toIntOrNull()?.let { if (it == 1 || it == 2) it else null },
    tokens[2].toEnumOrNull<Joypads.Button>()
  )

  private inline fun <reified E : Enum<E>> String.toEnumOrNull() = try {
    enumValueOf<E>(this.toUpperCase())
  } catch (_: IllegalArgumentException) {
    null
  }

  private fun String.toPcOrNull() = toAddressOrNull()?.toPC()

  private fun String.toAddressOrNull() = removePrefix("0x")
    .toIntOrNull(16)
    ?.let { if (it in 0x0000..0xFFFF) it else null }

  private fun <C : Command, T0> cmd(create: (T0) -> C, arg0: T0?): C? {
    return if (arg0 != null) create(arg0) else null
  }

  private fun <C : Command, T0, T1> cmd(create: (T0, T1) -> C, arg0: T0?, arg1: T1?): C? {
    return if (arg0 != null && arg1 != null) create(arg0, arg1) else null
  }
}
