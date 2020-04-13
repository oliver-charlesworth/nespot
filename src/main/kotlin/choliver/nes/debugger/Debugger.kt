package choliver.nes.debugger

import choliver.nes.Address
import choliver.nes.Data
import choliver.nes.Nes
import choliver.nes.debugger.Command.*
import choliver.nes.debugger.Command.CreatePoint.Break
import choliver.nes.debugger.Command.CreatePoint.Watch
import choliver.nes.debugger.Command.DeletePoint.All
import choliver.nes.debugger.Command.DeletePoint.ByNum
import choliver.nes.debugger.Command.Event.*
import choliver.nes.debugger.Command.Execute.*
import choliver.nes.debugger.Debugger.Point.Breakpoint
import choliver.nes.debugger.Debugger.Point.Watchpoint
import choliver.nes.debugger.Debugger.RoutineType.*
import choliver.nes.sixfiveohtwo.model.Opcode.*
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

  private enum class RoutineType {
    JUMP,
    NMI,
    IRQ
  }

  private data class StackEntry(
    val next: ProgramCounter,
    val type: RoutineType
  )

  private val parser = CommandParser(stdin)
  private val nes = Nes(rom).instrumentation

  private var nextPointNum = 1
  private val points = mutableMapOf<Int, Point>()
  private val breakpoints = mutableMapOf<ProgramCounter, Breakpoint>()
  private val watchpoints = mutableMapOf<Address, Watchpoint>()

  private val stack = Stack<StackEntry>()
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
        is Event -> event(cmd)
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

      is Next -> {
        val target = nextPc(cmd.num)
        while (nes.state.PC != target) {
          if (!step()) break
        }
      }

      is Continue -> while (true) {
        if (!step()) break
      }

      is Finish -> {
        val target = stack.peek().next
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
          is Break.AtOffset -> nextPc(cmd.offset)
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
        stdout.println("#%-4d %s: %s".format(0, nes.state.PC, instAt(nes.state.PC)))
        stack.reversed().forEachIndexed { idx, entry ->
          stdout.println("#%-4d %s: %-20s%s".format(
            idx + 1,
            entry.next,
            instAt(entry.next),
            if (entry.type != JUMP) "(${entry.type.name})" else ""
          ))
        }
      }

      is Info.Print -> stdout.println("0x%02x".format(nes.peek(cmd.addr)))
    }
  }

  private fun event(cmd: Event) {
    when (cmd) {
      is Reset -> {
        stack.clear()
        nes.reset()
      }
      is Nmi -> {
        stack.push(StackEntry(nes.state.PC, NMI))
        nes.nmi()
      }
      is Irq -> {
        stack.push(StackEntry(nes.state.PC, IRQ))
        nes.irq()
      }
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
      JSR -> stack.push(StackEntry(nextPc(), JUMP))
      RTS, RTI -> try {
        stack.pop() // TODO - validate type
      } catch (_: EmptyStackException) {
        stdout.println("Tried to return with empty stack")
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

  private fun nextPc(count: Int = 1) =
    (0 until count).fold(nes.state.PC) { pc, _ -> nes.decodeAt(pc).nextPc }

  private fun instAt(pc: ProgramCounter) = nes.decodeAt(pc).instruction

  private fun Address.format() = "0x%04x".format(this)
  private fun Data.format8() = "0x%02x".format(this)
}
