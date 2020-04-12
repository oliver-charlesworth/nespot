package choliver.nes.debugger

import choliver.nes.debugger.Command.*
import choliver.nes.sixfiveohtwo.model.toPC
import java.io.InputStream

class CommandParser(stdin: InputStream) {
  private val reader = stdin.bufferedReader()

  fun next(): Command {
    return parseCommand(reader.readLine())
  }

  private fun parseCommand(raw: String): Command {
    val r = raw.trim()
    val bits = if (r.isEmpty()) listOf("step") else r.split("\\s+".toRegex())

    val error = Error("Can't parse: ${r}")

    fun noArgs(cmd: Command) = when (bits.size) {
      1 -> cmd
      else -> error
    }

    return when (bits[0]) {
      "s", "step" -> when (bits.size) {
        1 -> Execute.Step(1)
        2 -> bits[1].toIntOrNull()?.let(Execute::Step) ?: error
        else -> error
      }

      "c", "cont" -> noArgs(Execute.Continue)

      "f", "finish" -> noArgs(Execute.Finish)

      "b", "break" -> when (bits.size) {
        1 -> CreatePoint.Break.Here
        2 -> when {
          bits[1] == "+" -> CreatePoint.Break.AtNext
          else -> bits[1].toAddressOrNull()?.let { CreatePoint.Break.At(it.toPC()) } ?: error
        }
        else -> error
      }

      "w", "watch" -> when (bits.size) {
        2 -> bits[1].toAddressOrNull()?.let(CreatePoint::Watch) ?: error
        else -> error
      }

      // TODO - clear

      "d", "delete" -> when (bits.size) {
        1 -> DeletePoint.All
        2 -> bits[1].toIntOrNull()?.let(DeletePoint::ByNum) ?: error
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
        2 -> bits[1].toAddressOrNull()?.let { Info.Print(it) } ?: error
        else -> error
      }

      "v", "verbosity" -> noArgs(ToggleVerbosity)

      "bt", "backtrace" -> noArgs(Info.Backtrace)

      "r", "restart" -> noArgs(Restart)

      "q", "quit" -> noArgs(Quit)

      else -> error
    }
  }

  private fun String.toAddressOrNull() = removePrefix("0x")
    .toIntOrNull(16)
    ?.let { if (it in 0x0000..0xFFFF) it else null }
}
