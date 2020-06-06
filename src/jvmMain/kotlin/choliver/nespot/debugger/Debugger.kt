package choliver.nespot.debugger

import choliver.nespot.BASE_VRAM
import choliver.nespot.VRAM_SIZE
import choliver.nespot.cartridge.Rom
import choliver.nespot.common.Address
import choliver.nespot.common.Data
import choliver.nespot.common.format16
import choliver.nespot.common.format8
import choliver.nespot.cpu.Cpu.NextStep
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
import choliver.nespot.nes.Nes
import choliver.nespot.nes.Nes.Companion.CPU_RAM_SIZE
import choliver.nespot.ui.KeyAction
import choliver.nespot.ui.KeyAction.Joypad
import choliver.nespot.ui.Screen
import com.fasterxml.jackson.databind.MapperFeature
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import java.io.File
import java.io.InputStream
import java.io.PrintStream
import java.util.concurrent.LinkedBlockingQueue
import choliver.nespot.ui.Event as RunnerEvent

class Debugger(
  rom: ByteArray,
  private val stdin: InputStream,
  private val stdout: PrintStream,
  private val script: String = ""
) {
  private data class Stats(
    val numInstructions: Int
  )

  private val events = LinkedBlockingQueue<RunnerEvent>()
  private val screen = Screen(onEvent = { events += it })
  private val stores = mutableListOf<Pair<Address, Data>>() // TODO - this is very global

  private val nes = Nes(
    rom = Rom.parse(rom),
    videoSink = screen.sink,
    onStore = { addr, data -> stores += (addr to data) }
  )
  private val diag = nes.diagnostics
  private val points = PointManager()
  private val stack = CallStackManager(diag)
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
        stdout.print("[${diag.cpu.regs.pc.format16()}]: ")
      }

      if (!handleCommand(parser.next())) {
        return
      }
    }
  }

  private fun handleCommand(cmd: Command): Boolean {
    consumeEvents()
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
      is Nop -> Unit
    }
    return true
  }

  private fun consumeEvents() {
    val myEvents = mutableListOf<RunnerEvent>()
    events.drainTo(myEvents)
    myEvents.forEach { e ->
      when (e) {
        is RunnerEvent.KeyDown -> when (val action = KeyAction.fromKeyCode(e.code)) {
          is Joypad -> nes.joypads.down(1, action.button)
        }
        is RunnerEvent.KeyUp -> when (val action = KeyAction.fromKeyCode(e.code)) {
          is Joypad -> nes.joypads.up(1, action.button)
        }
        is RunnerEvent.Close -> Unit
        is RunnerEvent.Error -> Unit
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
    fun until(cond: () -> Boolean) {
      while (!cond()) {
        if (!step()) return
      }
    }

    fun onePlusUntil(cond: () -> Boolean) {
      var tail = false
      until { (tail && cond()).also { tail = true } }
    }

    fun untilPlusOne(cond: () -> Boolean) {
      var complete = false
      until { complete.also { complete = complete || cond() } }
    }

    when (cmd) {
      is Step -> {
        var i = 0
        until { ++i > cmd.num }
      }

      // Perform specified number of instructions, but only within this stack frame
      is Next -> {
        val myDepth = stack.depth
        var i = 0
        until {
          if (stack.depth == myDepth) i++
          (i > cmd.num) || (stack.depth < myDepth)
        }
      }

      // Prevent Until(currentPC) from doing nothing
      is Until -> onePlusUntil { diag.cpu.regs.pc == cmd.pc }

      is UntilOffset -> {
        val target = nextPc(cmd.offset)
        until { diag.cpu.regs.pc == target }
      }

      // Prevent UntilOpcode(currentOpcode) from doing nothing
      is UntilOpcode -> onePlusUntil { instAt(diag.cpu.regs.pc).opcode == cmd.op }

      // One more so that the interrupt actually occurs
      is UntilNmi -> untilPlusOne { diag.cpu.nextStep == NextStep.NMI }

      // One more so that the interrupt actually occurs
      is UntilIrq -> untilPlusOne { diag.cpu.nextStep == NextStep.IRQ }

      is Continue -> until { false }

      is Finish -> {
        val myDepth = stack.depth
        until { stack.depth < myDepth }
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
        stdout.println("Breakpoint #${point.num}: ${point.pc.format16()} -> ${instAt(point.pc)}")
      }
      is Watch -> {
        val point = points.addWatchpoint(cmd.addr)
        stdout.println("Watchpoint #${point.num}: ${point.addr.format16()}")
      }
    }
  }

  private fun deletePoint(cmd: DeletePoint) {
    when (cmd) {
      is ByNum -> {
        when (val removed = points.remove(cmd.num)) {
          is Breakpoint -> stdout.println("Deleted breakpoint #${removed.num}: ${removed.pc.format16()} -> ${instAt(removed.pc)}")
          is Watchpoint -> stdout.println("Deleted watchpoint #${removed.num}: ${removed.addr.format16()}")
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

      is Info.Reg -> stdout.println(diag.cpu.regs)

      is Info.Break -> if (points.breakpoints.isEmpty()) {
        stdout.println("No breakpoints")
      } else {
        println("Num  Address  Instruction")
        points.breakpoints.forEach { (_, v) -> stdout.println("%-4d ${v.pc.format16()}   ${instAt(v.pc)}".format(v.num)) }
      }

      is Info.Watch -> if (points.watchpoints.isEmpty()) {
        stdout.println("No watchpoints")
      } else {
        println("Num  Address")
        points.watchpoints.forEach { (_, v) -> stdout.println("%-4d ${v.addr.format16()}".format(v.num)) }
      }

      is Info.Display -> if (displays.isEmpty()) {
        stdout.println("No displays")
      } else {
        println("Num  Address")
        displays.forEach { (k, v) -> stdout.println("%-4d ${v.format16()}".format(k)) }
      }

      is Info.Backtrace -> {
        stack.frames.forEachIndexed { idx, frame ->
          stdout.println("#%-4d ${frame.current.format16()}  <${frame.start.format16()}>  %-20s%s".format(
            idx,
            instAt(frame.current),
            when (frame.type) {
              NMI, IRQ -> " (${frame.type.name})"
              else -> ""
            }
          ))
        }
      }

      is Info.CpuRam -> displayDump((0 until CPU_RAM_SIZE).map { diag.peek(it) })

      // TODO - should this be before or after nametable mapping?
      is Info.PpuRam -> displayDump((0 until VRAM_SIZE).map { diag.peekV(it + BASE_VRAM) })

      is Info.PpuState -> {
        val mapper = jacksonObjectMapper()
          .enable(SerializationFeature.INDENT_OUTPUT)
          .enable(MapperFeature.SORT_PROPERTIES_ALPHABETICALLY)
        stdout.println(mapper.writeValueAsString(diag.ppu.state))
      }

      is Info.Print -> stdout.println(diag.peek(cmd.addr).format8())

      is Info.InspectInst -> {
        var pc = cmd.pc
        repeat(cmd.num) {
          val decoded = diag.cpu.decodeAt(pc)
          stdout.println("${pc.format16()}: ${decoded.instruction}")
          pc = decoded.nextPc
        }
      }
    }
  }

  private fun event(cmd: Event) {
    diag.cpu.nextStep = when (cmd) {
      is Reset -> NextStep.RESET
      is Nmi -> NextStep.NMI
      is Irq -> NextStep.IRQ
    }
    step()  // Perform one step so the interrupt actually gets handled
  }

  private fun button(cmd: Button) {
    when (cmd) {
      is Button.Up -> nes.joypads.up(cmd.which, cmd.button)
      is Button.Down -> nes.joypads.down(cmd.which, cmd.button)
    }
  }

  private fun step(): Boolean {
    val thisStep = diag.cpu.nextStep // nextStep might be modified

    when (thisStep) {
      NextStep.INSTRUCTION -> {
        maybeTraceInstruction()
        stack.preInstruction()
      }
      NextStep.RESET -> {
        maybeTraceInterrupt("RESET")
        stack.preReset()
      }
      NextStep.NMI -> {
        maybeTraceInterrupt("NMI")
        stack.preNmi()
      }
      NextStep.IRQ -> {
        maybeTraceInterrupt("IRQ")
        stack.preIrq()
      }
    }

    stores.clear()
    nes.step()

    maybeTraceStores()

    if (thisStep == NextStep.INSTRUCTION) {
      stack.postInstruction()
      stats = stats.copy(numInstructions = stats.numInstructions + 1)
    }

    return isWatchpointHit() && isBreakpointHit()
  }

  private fun maybeTraceInstruction() {
    if (isVerbose) {
      stdout.println("${diag.cpu.regs.pc.format16()}: ${instAt(diag.cpu.regs.pc)}")
    }
  }

  private fun maybeTraceInterrupt(name: String) {
    if (isVerbose) {
      stdout.println("${name} triggered")
    }
  }

  private fun maybeTraceStores() {
    if (isVerbose) {
      stores.forEach { (addr, data) ->
        stdout.println("    ${addr.format16()} <- ${data.format8()}")
      }
    }
  }

  private fun isWatchpointHit() =
    when (val wp = stores.map { points.watchpoints[it.first] }.firstOrNull { it != null }) {
      null -> true
      else -> {
        stdout.println("Hit watchpoint #${wp.num}: ${wp.addr.format16()}")
        false
      }
    }

  private fun isBreakpointHit() = when (val bp = points.breakpoints[diag.cpu.regs.pc]) {
    null -> true
    else -> {
      stdout.println("Hit breakpoint #${bp.num}")
      false
    }
  }

  private fun nextPc(offset: Int = 1) =
    (0 until offset).fold(diag.cpu.regs.pc) { pc, _ -> diag.cpu.decodeAt(pc).nextPc }

  private fun instAt(pc: Address) = diag.cpu.decodeAt(pc).instruction

  private fun displayDisplays() {
    displays.forEach { (k, v) ->
      stdout.println("${k}: ${v.format16()} = ${diag.peek(v).format8()}")
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

        stdout.println("${(i * numPerRow).format16()}:  ${hex}  ${chars}")
      }
  }

  private fun showScreen() {
    screen.show()
  }

  companion object {
    @JvmStatic
    fun main(args: Array<String>) = Debugger(
      rom = File(args[0]).readBytes(),
      stdin = System.`in`,
      stdout = System.out
    ).start()
  }
}
