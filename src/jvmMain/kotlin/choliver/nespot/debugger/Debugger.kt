package choliver.nespot.debugger

import choliver.nespot.BASE_VRAM
import choliver.nespot.RAM_SIZE
import choliver.nespot.VRAM_SIZE
import choliver.nespot.cartridge.Rom
import choliver.nespot.common.Address
import choliver.nespot.common.Data
import choliver.nespot.cpu.Cpu.NextStep
import choliver.nespot.debugger.Command.*
import choliver.nespot.debugger.Command.CreatePoint.Break
import choliver.nespot.debugger.Command.CreatePoint.Watch
import choliver.nespot.debugger.Command.DeletePoint.All
import choliver.nespot.debugger.Command.DeletePoint.ByNum
import choliver.nespot.debugger.Command.Event.*
import choliver.nespot.debugger.Command.Execute.*
import choliver.nespot.debugger.PointManager.Point
import choliver.nespot.memory.Memory
import choliver.nespot.nes.Nes
import choliver.nespot.ui.KeyAction
import choliver.nespot.ui.KeyAction.Joypad
import choliver.nespot.ui.Screen
import java.io.File
import java.io.InputStream
import java.io.PrintStream
import java.util.concurrent.LinkedBlockingQueue
import choliver.nespot.ui.Event as RunnerEvent

class Debugger(
  rom: ByteArray,
  private val stdin: InputStream,
  stdout: PrintStream,
  private val script: String = ""
) {
  private val events = LinkedBlockingQueue<RunnerEvent>()
  private val screen = Screen(onEvent = { events += it })
  private val stores = mutableListOf<Pair<Address, Data>>() // TODO - this is very global

  private val nes = Nes(
    rom = Rom.parse(rom),
    videoSink = screen.sink,
    intercept = { memory ->
      object : Memory {
        override fun get(addr: Address) = memory[addr]
        override fun set(addr: Address, data: Data) {
          memory[addr] = data
          stores += (addr to data)
        }
      }
    }
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

  private val printer = Printer(stdout, diag)


  fun start() {
    event(Reset)
    consume(CommandParser(stdin), true)
  }

  private fun consume(parser: CommandParser, enablePrompts: Boolean) {
    while (true) {
      if (enablePrompts) {
        printer.printPrompt()
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
      is Error -> printer.printParseError(cmd.msg)
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
        else -> Unit
      }
    }
  }

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
      is UntilOpcode -> onePlusUntil { diag.currentInstruction().opcode == cmd.op }

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

    printer.printDisplays(displays)
  }

  private fun createPoint(cmd: CreatePoint) {
    when (cmd) {
      is Break -> points.addBreakpoint(when (cmd) {
        is Break.AtOffset -> nextPc(cmd.offset)
        is Break.At -> cmd.pc
      })
      is Watch -> points.addWatchpoint(cmd.addr)
    }.also { printer.printPointCreated(it) }
  }

  private fun deletePoint(cmd: DeletePoint) {
    when (cmd) {
      is ByNum -> {
        val point = points.remove(cmd.num)
        printer.printPointDeleted(point)
      }

      is All -> {
        points.removeAll()
        printer.printPointDeletedAll()
      }
    }
  }

  private fun createDisplay(cmd: CreateDisplay) {
    displays[nextDisplayNum++] = cmd.addr
  }

  private fun info(cmd: Info) {
    with(printer) {
      when (cmd) {
        is Info.Stats -> printStats(stats)
        is Info.Reg -> printRegs()
        is Info.Break -> printBreakpointTable(points)
        is Info.Watch -> printWatchpointTable(points)
        is Info.Display -> printDisplayTable(displays)
        is Info.Backtrace -> printBacktrace(stack)
        is Info.CpuRam -> printDataDump((0 until RAM_SIZE).map { diag.peek(it) })
        is Info.PpuRam -> printDataDump((0 until VRAM_SIZE).map { diag.peekV(it + BASE_VRAM) }) // TODO - should this be before or after nametable mapping?
        is Info.PpuState -> printPpuState()
        is Info.Print -> printAtAddress(cmd.addr)
        is Info.InspectInst -> printInstContext(cmd.pc, cmd.num)
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

    stack.preStep(thisStep)

    if (isVerbose) {
      printer.printStep(thisStep)
    }

    stores.clear()
    nes.step()

    if (isVerbose) {
      printer.printStores(stores)
    }

    if (thisStep == NextStep.INSTRUCTION) {
      stack.postInstruction()
      stats = stats.copy(numInstructions = stats.numInstructions + 1)
    }

    return isWatchpointHit() && isBreakpointHit()
  }

  private fun isWatchpointHit() = anyHit(stores.map { points.watchpoints[it.first] }.firstOrNull { it != null })

  private fun isBreakpointHit() = anyHit(points.breakpoints[diag.cpu.regs.pc])

  private fun anyHit(point: Point?) = when (point) {
    null -> true
    else -> false.also { printer.printPointHit(point) }
  }

  private fun nextPc(offset: Int = 1) =
    (0 until offset).fold(diag.cpu.regs.pc) { pc, _ -> diag.cpu.decodeAt(pc).nextPc }

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
