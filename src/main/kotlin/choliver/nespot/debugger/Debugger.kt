package choliver.nespot.debugger

import choliver.nespot.Address
import choliver.nespot.Data
import choliver.nespot.Nes
import choliver.nespot.Nes.Companion.CPU_RAM_SIZE
import choliver.nespot.Nes.Companion.PPU_RAM_SIZE
import choliver.nespot.debugger.CallStackManager.FrameType.IRQ
import choliver.nespot.debugger.CallStackManager.FrameType.NMI
import choliver.nespot.debugger.Command.*
import choliver.nespot.debugger.Command.CreatePoint.Break
import choliver.nespot.debugger.Command.CreatePoint.Watch
import choliver.nespot.debugger.Command.DeletePoint.All
import choliver.nespot.debugger.Command.DeletePoint.ByNum
import choliver.nespot.debugger.Command.Event.*
import choliver.nespot.debugger.Command.Execute.*
import choliver.nespot.debugger.PointManager.Point.Breakpoint
import choliver.nespot.debugger.PointManager.Point.Watchpoint
import choliver.nespot.runner.FakeJoypads
import choliver.nespot.runner.Screen
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

  private enum class NextStep {
    INSTRUCTION,
    RESET,
    NMI,
    IRQ
  }

  private var nextStep = NextStep.INSTRUCTION

  private val joypads = FakeJoypads()
  private val screen = Screen(
    onButtonDown = { joypads.down(1, it) },
    onButtonUp = { joypads.up(1, it) }
  )

  private val stores = mutableListOf<Pair<Address, Data>>() // TODO - this is very global

  private val nes = Nes(
    rom,
    screen.buffer,
    joypads,
    onReset = { nextStep = NextStep.RESET },
    onNmi = { nextStep = NextStep.NMI; screen.redraw() },
    onIrq = { nextStep = NextStep.IRQ },
    onStore = { addr, data -> stores += (addr to data) }
  ).inspection
  private val points = PointManager()
  private val stack = CallStackManager(nes)
  private var stats = Stats(0)
  private var isVerbose = true
  private var macro: Command = Next(1)

  // Displays
  private var nextDisplayNum = 1
  private val displays = mutableMapOf<Int, Address>()

  fun start() {
    event(Reset)
    consume(CommandParser(stdin), true)
  }

  private fun consume(parser: CommandParser, enablePrompts: Boolean) {
    while (true) {
      if (enablePrompts) {
        stdout.print("[${nes.state.PC}]: ")
      }

      if (!handleCommand(parser.next())) {
        return
      }
    }
  }

  private fun handleCommand(cmd: Command): Boolean {
    when (cmd) {
      is Script -> script()
      is Repeat -> repeat(cmd.times) { handleCommand(cmd.cmd) }
      is RunMacro -> handleCommand(macro)
      is SetMacro -> macro = cmd.cmd
      is Execute -> execute(cmd)
      is CreatePoint -> createPoint(cmd)
      is DeletePoint -> deletePoint(cmd)
      is CreateDisplay -> createDisplay(cmd)
      is Info -> info(cmd)
      is ToggleVerbosity -> isVerbose = !isVerbose
      is Event -> event(cmd)
      is Button -> button(cmd)
      is ShowScreen -> showScreen()
      is Quit -> return false
      is Error -> stdout.println(cmd.msg)
    }
    return true
  }

  // TODO - this recursion is weird - can we combine this + stdin with flatMap magic?
  private fun script() {
    script.byteInputStream().use { stream ->
      consume(CommandParser(stream, ignoreBlanks = true), enablePrompts = false)
    }
  }

  private fun execute(cmd: Execute) {
    fun until(cond: () -> Boolean) {
      while (cond()) {
        if (!step()) return
      }
    }

    fun oneThenUntil(cond: () -> Boolean) {
      var first = true
      until { (first || cond()).also { first = false } }
    }

    fun untilThenOne(cond: () -> Boolean) {
      var incomplete = true
      until { incomplete.also { incomplete = incomplete && cond() } }
    }

    when (cmd) {
      is Step -> {
        var i = 0
        until { ++i <= cmd.num }
      }

      // Perform specified number of instructions, but only within this stack frame
      is Next -> {
        val myDepth = stack.depth
        var i = 0
        until {
          if (stack.depth == myDepth) i++
          (i <= cmd.num) && (stack.depth >= myDepth)
        }
      }

      // Prevent Until(currentPC) from doing nothing
      is Until -> oneThenUntil { nes.state.PC != cmd.pc }

      is UntilOffset -> {
        val target = nextPc(cmd.offset)
        until { nes.state.PC != target }
      }

      // Prevent UntilOpcode(currentOpcode) from doing nothing
      is UntilOpcode -> oneThenUntil { instAt(nes.state.PC).opcode != cmd.op }

      // One more so that the interrupt actually occurs
      is UntilNmi -> untilThenOne { nextStep != NextStep.NMI }

      is Continue -> until { true }

      is Finish -> {
        val myDepth = stack.depth
        until { stack.depth >= myDepth }
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
        stdout.println("Breakpoint #${point.num}: ${point.pc.format()} -> ${instAt(point.pc)}")
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
          is Breakpoint -> stdout.println("Deleted breakpoint #${removed.num}: ${removed.pc.format()} -> ${instAt(removed.pc)}")
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

  private fun createDisplay(cmd: CreateDisplay) {
    displays[nextDisplayNum++] = cmd.addr
  }

  private fun info(cmd: Info) {
    when (cmd) {
      is Info.Stats -> displayStats()

      is Info.Reg -> stdout.println(nes.state)

      is Info.Break -> if (points.breakpoints.isEmpty()) {
        stdout.println("No breakpoints")
      } else {
        println("Num  Address  Instruction")
        points.breakpoints.forEach { (_, v) -> stdout.println("%-4d %s   %s".format(v.num, v.pc.format(), instAt(v.pc))) }
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
          stdout.println("#%-4d 0x%04x  <0x%04x>  %-20s%s".format(
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

      is Info.PpuRam -> displayDump((0 until PPU_RAM_SIZE).map { nes.peekV(it + 0x2000) })

      is Info.Print -> stdout.println(nes.peek(cmd.addr).format8())

      is Info.InspectInst -> {
        var pc = cmd.pc
        repeat(cmd.num) {
          val decoded = nes.decodeAt(pc)
          stdout.println("0x%04x: ${decoded.instruction}".format(pc))
          pc = decoded.nextPc
        }
      }
    }
  }

  private fun event(cmd: Event) {
    when (cmd) {
      is Reset -> nes.fireReset()
      is Nmi -> nes.fireNmi()
      is Irq -> nes.fireIrq()
    }
    step()  // Perform one step so the interrupt actually gets handled
  }

  private fun button(cmd: Button) {
    when (cmd) {
      is Button.Up -> joypads.up(cmd.which, cmd.button)
      is Button.Down -> joypads.down(cmd.which, cmd.button)
    }
  }

  private fun step(): Boolean {
    val thisStep = nextStep // nextStep might be modified

    when (thisStep) {
      NextStep.INSTRUCTION -> {
        maybeTraceInstruction()
        stack.preInstruction()
      }
      NextStep.RESET -> {
        stdout.println("RESET triggered")
        stack.preReset()
      }
      NextStep.NMI -> {
        stdout.println("NMI triggered")
        stack.preNmi()
      }
      NextStep.IRQ -> {
        stdout.println("IRQ triggered")
        stack.preIrq()
      }
    }

    stores.clear()
    nes.step()

    maybeTraceStores()

    when (thisStep) {
      NextStep.INSTRUCTION -> {
        stack.postInstruction()
        stats = stats.copy(numInstructions = stats.numInstructions + 1)
      }
      else -> nextStep = NextStep.INSTRUCTION
    }

    return isWatchpointHit() && isBreakpointHit()
  }

  private fun maybeTraceInstruction() {
    if (isVerbose) {
      stdout.println("${nes.state.PC}: ${instAt(nes.state.PC)}")  // TODO - de-dupe with InspectInst handler
    }
  }

  private fun maybeTraceStores() {
    if (isVerbose) {
      stores.forEach { (addr, data) ->
        stdout.println("    ${addr.format()} <- ${data.format8()}")
      }
    }
  }

  private fun isWatchpointHit() =
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

  private fun instAt(pc: Address) = nes.decodeAt(pc).instruction

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

  private fun showScreen() {
    screen.show()
  }

  private fun Address.format() = "0x%04x".format(this)
  private fun Data.format8() = "0x%02x".format(this)
}
