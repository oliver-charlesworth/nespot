package choliver.nes.debugger

import choliver.nes.Address
import choliver.nes.Data
import choliver.nes.Nes
import choliver.nes.Nes.Companion.CPU_RAM_SIZE
import choliver.nes.Nes.Companion.PPU_RAM_SIZE
import choliver.nes.debugger.CallStackManager.FrameType.IRQ
import choliver.nes.debugger.CallStackManager.FrameType.NMI
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
  private val stdin: InputStream,
  private val stdout: PrintStream,
  private val script: String = ""
) {
  private data class Stats(
    val numInstructions: Int
  )

  private val screen = Screen()
  private val nes = Nes(rom).instrumentation
  private var points = PointManager()
  private var stack = CallStackManager(nes)
  private var stats = Stats(0)
  private var isVerbose = true

  // Displays
  private var nextDisplayNum = 1
  private val displays = mutableMapOf<Int, Address>()

  fun start() {
    event(Reset) // TODO - this is cheating
    consume(CommandParser(stdin), true)
  }

  private fun consume(parser: CommandParser, enablePrompts: Boolean) {
    // TODO - handle Ctrl+C ?
    while (true) {
      if (enablePrompts) {
        stdout.print("[${nes.state.PC}]: ")
      }

      when (val cmd = parser.next()) {
        is Script -> script()
        is Execute -> execute(cmd)
        is CreatePoint -> createPoint(cmd)
        is DeletePoint -> deletePoint(cmd)
        is CreateDisplay -> displays[nextDisplayNum++] = cmd.addr
        is Info -> info(cmd)
        is ToggleVerbosity -> isVerbose = !isVerbose
        is Event -> event(cmd)
        is Render -> render()
        is Quit -> return
        is Error -> stdout.println(cmd.msg)
      }
    }
  }

  // TODO - this recursion is weird - can we combine this + stdin with flatMap magic?
  private fun script() {
    script.byteInputStream().use { stream ->
      consume(CommandParser(stream, ignoreBlanks = true), enablePrompts = false)
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

    displayDisplays()
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
      is Info.Stats -> displayStats()

      is Info.Reg -> stdout.println(nes.state)

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

      is Info.Display -> if (displays.isEmpty()) {
        stdout.println("No displays")
      } else {
        println("Num  Address")
        displays.forEach { (k, v) -> stdout.println("%-4d %s".format(k, v.format())) }
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

      is Info.Print -> stdout.println(nes.peek(cmd.addr).format8())

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
        stdout.println("Triggered RESET -> ${nes.state.PC}")
      }
      is Nmi -> {
        nes.nmi()
        stack.handleNmi()
        stdout.println("Triggered NMI -> ${nes.state.PC}")
      }
      is Irq -> {
        nes.irq()
        stack.handleIrq()
        stdout.println("Triggered IRQ -> ${nes.state.PC}")
      }
    }
  }

  private fun step(): Boolean {
    maybeTraceInstruction()
    stack.preStep()
    val stores = nes.step()
    stack.postStep()
    maybeTraceStores(stores)
    stats = stats.copy(numInstructions = stats.numInstructions + 1)
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

  private fun displayDisplays() {
    displays.forEach { (k, v) ->
      stdout.println("${k}: ${v.format()} = ${nes.peek(v).format8()}")
    }
  }

  private fun displayStats() {
    stdout.println("Num instructions executed: ${stats.numInstructions}")
  }

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

  private fun render() {
    screen.show()
    nes.renderTo(screen.buffer)
    screen.redraw()
  }

  private fun Address.format() = "0x%04x".format(this)
  private fun Data.format8() = "0x%02x".format(this)
}
