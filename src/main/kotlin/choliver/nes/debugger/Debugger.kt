package choliver.nes.debugger

import choliver.nes.Address
import choliver.nes.Data
import choliver.nes.Nes
import choliver.nes.Nes.Companion.CPU_RAM_SIZE
import choliver.nes.Nes.Companion.PPU_RAM_SIZE
import choliver.nes.debugger.Command.*
import choliver.nes.debugger.Command.CreatePoint.Break
import choliver.nes.debugger.Command.CreatePoint.Watch
import choliver.nes.debugger.Command.DeletePoint.All
import choliver.nes.debugger.Command.DeletePoint.ByNum
import choliver.nes.debugger.Command.Event.*
import choliver.nes.debugger.Command.Execute.*
import choliver.nes.debugger.Debugger.RoutineType.*
import choliver.nes.debugger.PointManager.Point.Breakpoint
import choliver.nes.debugger.PointManager.Point.Watchpoint
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
  private var points = PointManager()
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
        // Perform specified number of instructions, but only within this stack frame
        val depth = stack.size
        var n = cmd.num
        while ((n > 0) && (stack.size >= depth)) {
          if (!step()) break
          if (stack.size == depth) {
            n--
          }
        }
      }

      is Until -> while (nes.state.PC != cmd.pc) {
        if (!step()) break
      }

      is UntilOffset -> {
        val target = nextPc(cmd.offset)
        while (nes.state.PC != target) {
          if (!step()) break
        }
      }

      is UntilOpcode -> while (instAt(nes.state.PC).opcode != cmd.op) {
        if (!step()) break
      }

      is Continue -> while (true) {
        if (!step()) break
      }

      is Finish -> {
        val depth = stack.size
        while (stack.size >= depth) {
          if (!step()) break
        }
      }
    }
  }

  private fun createPoint(cmd: CreatePoint) {
    when (cmd) {
      is Break -> {
        val point = points.addBreakpoint(when (cmd) {
          is Break.AtOffset -> nextPc(cmd.offset)
          is Break.At -> cmd.pc
        })
        stdout.println("Breakpoint #${point.num}: ${point.pc} -> ${instAt(point.pc)}")
      }
      is Watch -> {
        val point = points.addWatchpoint(cmd.addr)
        stdout.println("Watchpoint #${point.num}: ${point.addr.format()}")
      }
    }
  }

  private fun deletePoint(cmd: DeletePoint) {
    when (cmd) {
      is ByNum -> {
        when (val removed = points.remove(cmd.num)) {
          is Breakpoint -> stdout.println("Deleted breakpoint #${removed.num}: ${removed.pc} -> ${instAt(removed.pc)}")
          is Watchpoint -> stdout.println("Deleted watchpoint #${removed.num}: ${removed.addr.format()}")
          null -> stdout.println("No such breakpoint or watchpoint")
        }
      }

      is All -> {
        points.removeAll()
        stdout.println("Deleted all breakpoints & watchpoints")
      }
    }
  }

  private fun info(cmd: Info) {
    when (cmd) {
      is Info.Reg -> println(nes.state)

      is Info.Break -> if (points.breakpoints.isEmpty()) {
        stdout.println("No breakpoints")
      } else {
        println("Num  Address  Instruction")
        points.breakpoints.forEach { (_, v) -> stdout.println("%-4d %s   %s".format(v.num, v.pc, instAt(v.pc))) }
      }

      is Info.Watch -> if (points.watchpoints.isEmpty()) {
        stdout.println("No watchpoints")
      } else {
        println("Num  Address")
        points.watchpoints.forEach { (_, v) -> stdout.println("%-4d %s".format(v.num, v.addr.format())) }
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

      is Info.CpuRam -> displayDump((0 until CPU_RAM_SIZE).map { nes.peek(it) })

      is Info.PpuRam -> displayDump((0 until PPU_RAM_SIZE).map { nes.peekV(it) })

      is Info.Print -> stdout.println("0x%02x".format(nes.peek(cmd.addr)))

      is Info.InspectInst -> {
        var pc = cmd.pc
        repeat(cmd.num) {
          val decoded = nes.decodeAt(pc)
          stdout.println("${pc}: ${decoded.instruction}")
          pc = decoded.nextPc
        }
      }
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

  private fun isWatchpointHit(stores: List<Pair<Address, Data>>) =
    when (val wp = stores.map { points.watchpoints[it.first] }.firstOrNull { it != null }) {
      null -> true
      else -> {
        stdout.println("Hit watchpoint #${wp.num}: ${wp.addr.format()}")
        false
      }
    }

  private fun isBreakpointHit() = when (val bp = points.breakpoints[nes.state.PC]) {
    null -> true
    else -> {
      stdout.println("Hit breakpoint #${bp.num}")
      false
    }
  }

  private fun nextPc(offset: Int = 1) =
    (0 until offset).fold(nes.state.PC) { pc, _ -> nes.decodeAt(pc).nextPc }

  private fun instAt(pc: ProgramCounter) = nes.decodeAt(pc).instruction

  private fun displayDump(data: List<Data>) {
    val numPerRow = 32
    data.chunked(numPerRow)
      .forEachIndexed { i, row ->
        val tmp = row
          .chunked(2)
          .joinToString(" ") { "%02x%02x".format(it[0], it[1]) }
        stdout.println("${(i * numPerRow).format()}: ${tmp}")
      }
  }

  private fun Address.format() = "0x%04x".format(this)
  private fun Data.format8() = "0x%02x".format(this)
}
