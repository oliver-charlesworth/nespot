package choliver.nes.debugger

import choliver.nes.Address
import choliver.nes.Data
import choliver.nes.Nes
import choliver.nes.Nes.Companion.CPU_RAM_SIZE
import choliver.nes.Nes.Companion.PPU_RAM_SIZE
import choliver.nes.debugger.CallStackManager.FrameType.*
import choliver.nes.debugger.Command.*
import choliver.nes.debugger.Command.CreatePoint.Break
import choliver.nes.debugger.Command.CreatePoint.Watch
import choliver.nes.debugger.Command.DeletePoint.All
import choliver.nes.debugger.Command.DeletePoint.ByNum
import choliver.nes.debugger.Command.Event.*
import choliver.nes.debugger.Command.Execute.*
import choliver.nes.debugger.PointManager.Point.Breakpoint
import choliver.nes.debugger.PointManager.Point.Watchpoint
import choliver.nes.sixfiveohtwo.model.ProgramCounter
import java.io.InputStream
import java.io.PrintStream

class Debugger(
  rom: ByteArray,
  stdin: InputStream,
  private val stdout: PrintStream
) {
  private val parser = CommandParser(stdin)
  private val nes = Nes(rom).instrumentation
  private var points = PointManager()
  private var stack = CallStackManager(nes)
  private var isVerbose = true

  fun start() {
    event(Reset) // TODO - this is cheating

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
        val myDepth = stack.depth
        var n = cmd.num
        while ((n > 0) && (stack.depth >= myDepth)) {
          if (!step()) break
          if (stack.depth == myDepth) {
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
        val myDepth = stack.depth
        while (stack.depth >= myDepth) {
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
        stack.frames.forEachIndexed { idx, frame ->
          stdout.println("#%-4d %s  <%s>  %-20s%s".format(
            idx,
            frame.current,
            frame.start,
            instAt(frame.current),
            when (frame.type) {
              NMI, IRQ -> " (${frame.type.name})"
              else -> ""
            }
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
        nes.reset()
        stack.handleReset()
      }
      is Nmi -> {
        nes.nmi()
        stack.handleNmi()
      }
      is Irq -> {
        nes.irq()
        stack.handleIrq()
      }
    }
  }

  private fun step(): Boolean {
    maybeTraceInstruction()
    stack.handleStep()
    val stores = nes.step()
    maybeTraceStores(stores)
    return isWatchpointHit(stores) && isBreakpointHit()
  }

  private fun maybeTraceInstruction() {
    if (isVerbose) {
      stdout.println("${nes.state.PC}: ${instAt(nes.state.PC)}")  // TODO - de-dupe with InspectInst handler
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
        val hex = row
          .chunked(16)
          .joinToString("  ") { half ->
            half
              .chunked(2)
              .joinToString(" ") { "%02x%02x".format(it[0], it[1]) }
          }

        val chars = row
          .chunked(16)
          .joinToString(" ") { half ->
            half
              .map { if (it in 32..126) it.toChar() else '.' }
              .joinToString("")
          }

        stdout.println("${(i * numPerRow).format()}:  ${hex}  ${chars}")
      }
  }

  private fun Address.format() = "0x%04x".format(this)
  private fun Data.format8() = "0x%02x".format(this)
}
