package choliver.nes.debugger

import choliver.nes.Address
import choliver.nes.Data
import choliver.nes.Nes
import choliver.nes.debugger.Command.*
import choliver.nes.debugger.Command.CreatePoint.Break
import choliver.nes.debugger.Command.CreatePoint.Watch
import choliver.nes.debugger.Command.DeletePoint.All
import choliver.nes.debugger.Command.DeletePoint.ByNum
import choliver.nes.debugger.Command.Execute.*
import choliver.nes.debugger.Debugger.Point.Breakpoint
import choliver.nes.debugger.Debugger.Point.Watchpoint
import choliver.nes.sixfiveohtwo.model.Opcode.JSR
import choliver.nes.sixfiveohtwo.model.Opcode.RTS
import choliver.nes.sixfiveohtwo.model.ProgramCounter
import java.io.InputStream
import java.io.PrintStream
import java.util.*

class Debugger(
  rom: ByteArray,
  stdin: InputStream,
  private val stdout: PrintStream
) {
  private sealed class Point(open val num: Int) {
    data class Breakpoint(override val num: Int, val pc: ProgramCounter) : Point(num)
    data class Watchpoint(override val num: Int, val addr: Address) : Point(num)
  }

  private val parser = CommandParser(stdin)
  private val nes = Nes(rom).instrumentation

  private var nextPointNum = 1
  private val points = mutableMapOf<Int, Point>()
  private val breakpoints = mutableMapOf<ProgramCounter, Breakpoint>()
  private val watchpoints = mutableMapOf<Address, Watchpoint>()

  private val stack = Stack<ProgramCounter>()
  private var isVerbose = true

  fun start() {
    // TODO - handle Ctrl+C ?
    loop@ while (true) {
      stdout.print("[${nes.state.PC}]: ")

      when (val cmd = parser.next()) {
        is Execute -> execute(cmd)
        is CreatePoint -> createPoint(cmd)
        is DeletePoint -> deletePoint(cmd)
        is Info -> info(cmd)
        is ToggleVerbosity -> isVerbose = !isVerbose
        is Restart -> nes.reset()
        is Quit -> break@loop
        is Error -> stdout.println(cmd.msg)
      }
    }
  }

  private fun execute(cmd: Execute) {
    when (cmd) {
      is Step -> for (i in 0 until cmd.num) {
        if (!step()) break
      }

      is Continue -> while (true) {
        if (!step()) break
      }

      is Finish -> {
        val target = nes.decodeAt(stack.peek()).nextPc
        while (nes.state.PC != target) {
          if (!step()) break
        }
      }
    }
  }

  private fun createPoint(cmd: CreatePoint) {
    when (cmd) {
      is Break -> {
        val point = Breakpoint(nextPointNum++, when (cmd) {
          is Break.Here -> nes.state.PC
          is Break.AtNext -> nes.decodeAt(nes.state.PC).nextPc
          is Break.At -> cmd.pc
        })
        points[point.num] = point
        breakpoints[point.pc] = point
        stdout.println("Breakpoint #${point.num}: ${point.pc} -> ${instAt(point.pc)}")
      }
      is Watch -> {
        val point = Watchpoint(nextPointNum++, cmd.addr)
        points[point.num] = point
        watchpoints[point.addr] = point
        stdout.println("Watchpoint #${point.num}: ${point.addr.format()}")
      }
    }
  }

  private fun deletePoint(cmd: DeletePoint) {
    when (cmd) {
      is ByNum -> {
        when (val point = points.remove(cmd.num)) {
          is Breakpoint -> {
            breakpoints.remove(point.pc)
            stdout.println("Deleted breakpoint #${point.num}: ${point.pc} -> ${instAt(point.pc)}")
          }
          is Watchpoint -> {
            watchpoints.remove(point.addr)
            stdout.println("Deleted watchpoint #${point.num}: ${point.addr.format()}")
          }
          null -> stdout.println("No such breakpoint or watchpoint")
        }
      }

      is All -> {
        points.clear()
        breakpoints.clear()
        watchpoints.clear()
        stdout.println("Deleted all breakpoints & watchpoints")
      }
    }
  }

  private fun info(cmd: Info) {
    when (cmd) {
      is Info.Reg -> println(nes.state)

      is Info.Break -> if (breakpoints.isEmpty()) {
        stdout.println("No breakpoints")
      } else {
        println("Num  Address  Instruction")
        breakpoints.forEach { (_, v) -> stdout.println("%-4d %s   %s".format(v.num, v.pc, instAt(v.pc))) }
      }

      is Info.Watch -> if (watchpoints.isEmpty()) {
        stdout.println("No watchpoints")
      } else {
        println("Num  Address")
        watchpoints.forEach { (_, v) -> stdout.println("%-4d %s".format(v.num, v.addr.format())) }
      }

      is Info.Backtrace -> {
        fun print(num: Int, pc: ProgramCounter) = stdout.println("#%-4d %s: %s".format(num, pc, instAt(pc)))
        print(0, nes.state.PC)
        stack.reversed().forEachIndexed { idx, pc -> print(idx + 1, pc) }
      }

      is Info.Print -> stdout.println("0x%02x".format(nes.peek(cmd.addr)))
    }
  }

  private fun step(): Boolean {
    updateStack()
    maybeTraceInstruction()
    val stores = nes.step()
    maybeTraceStores(stores)
    return isWatchpointHit(stores) && isBreakpointHit()
  }

  private fun updateStack() {
    // TODO - handle interrupts
    when (instAt(nes.state.PC).opcode) {
      JSR -> stack.push(nes.state.PC)
      RTS -> try {
        stack.pop()
      } catch (_: EmptyStackException) {
        stdout.println("Tried to RTS on empty stack")
      }
      else -> {}
    }
  }

  private fun maybeTraceInstruction() {
    if (isVerbose) {
      stdout.println("${nes.state.PC}: ${instAt(nes.state.PC)}")
    }
  }

  private fun maybeTraceStores(stores: List<Pair<Address, Data>>) {
    if (isVerbose) {
      stores.forEach { (addr, data) ->
        stdout.println("    ${addr.format()} <- ${data.format8()}")
      }
    }
  }

  private fun isWatchpointHit(stores: List<Pair<Address, Data>>): Boolean {
    val wp = stores.map { watchpoints[it.first] }.firstOrNull { it != null }
    return if (wp != null) {
      stdout.println("Hit watchpoint #${wp.num}: ${wp.addr.format()}")
      false
    } else {
      true
    }
  }

  private fun isBreakpointHit(): Boolean {
    val bp = breakpoints[nes.state.PC]
    return if (bp != null) {
      stdout.println("Hit breakpoint #${bp.num}")
      false
    } else {
      true
    }
  }

  private fun instAt(pc: ProgramCounter) = nes.decodeAt(pc).instruction

  private fun Address.format() = "0x%04x".format(this)
  private fun Data.format8() = "0x%02x".format(this)
}
