package choliver.nes

import choliver.nes.Debugger.Companion.Command.*
import choliver.nes.sixfiveohtwo.model.ProgramCounter

class Debugger {
  companion object {
    private sealed class Command {
      data class Step(val num: Int) : Command()
      object Continue : Command()
      object BreakHere : Command()
      data class BreakAt(val addr: Address) : Command()
      object InfoReg : Command()
      object InfoBreak : Command()
      object Where : Command()
      object Quit : Command()
      data class Error(val msg: String) : Command()
    }

    @JvmStatic
    fun main(args: Array<String>) {
      val nes = Nes({}.javaClass.getResource("/smb.nes").readBytes())
      val breakpoints = mutableListOf<ProgramCounter>()

      // TODO - handle Ctrl+C ?
      loop@ while (true) {
        print("[${nes.state.PC}]: ")
        when (val cmd = parseCommand(readLine()!!)) {
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

          is BreakAt -> TODO()

          is InfoReg -> println(nes.state)

          is InfoBreak -> {
            if (breakpoints.isEmpty()) {
              println("No breakpoints")
            } else {
              breakpoints.forEach { println("    ${it} -> ${nes.decodeAt(it)}") }
            }
          }

          is Where -> println(nes.decodeAt(nes.state.PC))

          is Quit -> break@loop

          is Error -> println(cmd.msg)
        }
      }
    }

    private fun parseCommand(raw: String): Command {
      val r = raw.trim()
      val bits = when {
        r.isEmpty() -> listOf("step")
        else -> r.split(" ")
      }

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
          2 -> TODO()
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

        "q", "quit" -> when (bits.size) {
          1 -> Quit
          else -> error
        }

        else -> error
      }
    }
  }
}
