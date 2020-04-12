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
  private var nextBreakpointNum = 1
  private val breakpoints = mutableMapOf<ProgramCounter, Int>()

  fun start() {
    // TODO - handle Ctrl+C ?
    loop@ while (true) {
      stdout.print("[${nes.state.PC}]: ")

      when (val cmd = parseCommand(reader.readLine())) {
        is Step -> repeat(cmd.num) step@{ if (!step()) return@step }

        is Continue -> while (true) { if (!step()) break }

        is CreateBreakpoint -> {
          val target = when (cmd) {
            is CreateBreakpoint.Here -> nes.state.PC
            is CreateBreakpoint.Next -> nes.decodeAt(nes.state.PC).nextPc
            is CreateBreakpoint.At -> cmd.addr.toPC()
          }
          // TODO - detect dupes
          stdout.println("Breakpoint #${nextBreakpointNum} at ${target}: ${instAt(target)}")
          breakpoints[target] = nextBreakpointNum++
        }

        is DeleteBreakpoint -> {
          val entry = breakpoints.entries.firstOrNull { it.value == cmd.num }
          if (entry != null) {
            stdout.println("Deleted breakpoint #${entry.value} at ${entry.key}: ${instAt(entry.key)}")
            breakpoints.remove(entry.key)
          } else {
            stdout.println("No such breakpoint")
          }
        }

        is InfoReg -> println(nes.state)

        is InfoBreak -> {
          if (breakpoints.isEmpty()) {
            stdout.println("No breakpoints")
          } else {
            println("Num  Address  Instruction")
            breakpoints.forEach { stdout.println("%-4d %s   %s".format(it.value, it.key, instAt(it.key))) }
          }
        }

        is Where -> println(instAt(nes.state.PC))

        is Restart -> nes.reset()

        is Quit -> break@loop

        is Error -> stdout.println(cmd.msg)
      }
    }
  }

  private fun instAt(pc: ProgramCounter) = nes.decodeAt(pc).instruction

  private fun step(): Boolean {
    nes.step()
    val bp = breakpoints[nes.state.PC]
    return if (bp != null) {
      stdout.println("Hit breakpoint #${bp}")
      false
    } else {
      true
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
        1 -> CreateBreakpoint.Here
        2 -> when {
          bits[1] == "+" -> CreateBreakpoint.Next
          bits[1].startsWith("0x") -> bits[1].toAddressOrNull()?.let { CreateBreakpoint.At(it) } ?: error
          else -> error
        }
        else -> error
      }

      "d", "delete" -> when (bits.size) {
        2 -> bits[1].toIntOrNull()?.let(::DeleteBreakpoint) ?: error
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
}
