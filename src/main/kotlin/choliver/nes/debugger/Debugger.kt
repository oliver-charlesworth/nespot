package choliver.nes.debugger

import choliver.nes.Nes
import choliver.nes.debugger.Command.*
import choliver.nes.sixfiveohtwo.model.ProgramCounter
import choliver.nes.sixfiveohtwo.model.toPC
import java.io.InputStream
import java.io.PrintStream

class Debugger(
  rom: ByteArray,
  stdin: InputStream,
  private val stdout: PrintStream
) {
  private val reader = stdin.bufferedReader()
  private val nes = Nes(rom)
  private val breakpoints = mutableListOf<ProgramCounter>()

  fun start() {
    // TODO - handle Ctrl+C ?
    loop@ while (true) {
      stdout.print("[${nes.state.PC}]: ")
      when (val cmd = parseCommand(reader.readLine())) {
        is Step -> repeat(cmd.num) step@{
          nes.step()
          if (nes.state.PC in breakpoints) {
            return@step
          }
        }

        is Continue -> while (true) {
          nes.step()
          if (nes.state.PC in breakpoints) {
            break
          }
        }

        is BreakHere -> breakpoints.add(nes.state.PC)

        is BreakNext -> breakpoints.add(nes.decodeAt(nes.state.PC).nextPc)

        is BreakAt -> breakpoints.add(cmd.addr.toPC())

        is InfoReg -> println(nes.state)

        is InfoBreak -> {
          if (breakpoints.isEmpty()) {
            stdout.println("No breakpoints")
          } else {
            breakpoints.forEach { stdout.println("    ${it} -> ${nes.decodeAt(it).instruction}") }
          }
        }

        is Where -> println(nes.decodeAt(nes.state.PC).instruction)

        is Restart -> nes.reset()

        is Quit -> break@loop

        is Error -> stdout.println(cmd.msg)
      }
    }
  }

  private fun parseCommand(raw: String): Command {
    val r = raw.trim()
    val bits = if (r.isEmpty()) listOf("step") else r.split("\\s+".toRegex())

    val error = Error("Can't parse: ${r}")
    return when (bits[0]) {
      "s", "step" -> when (bits.size) {
        1 -> Step(1)
        2 -> bits[1].toIntOrNull()?.let(::Step) ?: error
        else -> error
      }

      "c", "cont" -> when (bits.size) {
        1 -> Continue
        else -> error
      }

      "b", "break" -> when (bits.size) {
        1 -> BreakHere
        2 -> when {
          bits[1] == "+" -> BreakNext
          bits[1].startsWith("0x") -> bits[1].toAddressOrNull()?.let(::BreakAt) ?: error
          else -> error
        }
        else -> error
      }

      "i", "info" -> when (bits.size) {
        1 -> error
        else -> when (bits[1]) {
          "r", "reg" -> InfoReg
          "b", "break" -> InfoBreak
          else -> error
        }
      }

      "w", "where" -> when (bits.size) {
        1 -> Where
        else -> error
      }

      "r", "restart" -> when (bits.size) {
        1 -> Restart
        else -> error
      }

      "q", "quit" -> when (bits.size) {
        1 -> Quit
        else -> error
      }

      else -> error
    }
  }

  private fun String.toAddressOrNull() = removePrefix("0x")
    .toIntOrNull(16)
    ?.let { if (it in 0x0000..0xFFFF) it else null }

  companion object {
    @JvmStatic
    fun main(args: Array<String>) = Debugger(
      rom = {}.javaClass.getResource("/smb.nes").readBytes(),
      stdin = System.`in`,
      stdout = System.out
    ).start()


  }

}
