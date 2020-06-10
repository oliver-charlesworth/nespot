package choliver.nespot.debugger

import choliver.nespot.common.Address
import choliver.nespot.common.Data
import choliver.nespot.common.format16
import choliver.nespot.common.format8
import choliver.nespot.cpu.Cpu.NextStep
import choliver.nespot.cpu.Cpu.NextStep.INSTRUCTION
import choliver.nespot.debugger.CallStackManager.FrameType.IRQ
import choliver.nespot.debugger.CallStackManager.FrameType.NMI
import choliver.nespot.debugger.PointManager.Point
import choliver.nespot.debugger.PointManager.Point.Breakpoint
import choliver.nespot.debugger.PointManager.Point.Watchpoint
import choliver.nespot.nes.Nes
import com.fasterxml.jackson.databind.MapperFeature.SORT_PROPERTIES_ALPHABETICALLY
import com.fasterxml.jackson.databind.SerializationFeature.INDENT_OUTPUT
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import java.io.PrintStream

internal class Printer(
  private val stdout: PrintStream,
  private val diag: Nes.Diagnostics
) {
  fun printParseError(msg: String) {
    stdout.println(msg)
  }

  fun printPrompt() {
    stdout.print("[${diag.cpu.regs.pc.format16()}]: ")
  }

  fun printDataDump(data: List<Data>) {
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

  fun printStats(stats: Stats) {
    stdout.println("Num instructions executed: ${stats.numInstructions}")
  }

  fun printRegs() {
    stdout.println(diag.cpu.regs)
  }

  fun printBreakpointTable(points: PointManager) {
    if (points.breakpoints.isEmpty()) {
      stdout.println("No breakpoints")
    } else {
      stdout.println("Num  Address  Instruction")
      points.breakpoints.forEach { (_, v) -> stdout.println("%-4d ${v.pc.format16()}   ${instAt(v.pc)}".format(v.num)) }
    }
  }

  fun printWatchpointTable(points: PointManager) {
    if (points.watchpoints.isEmpty()) {
      stdout.println("No watchpoints")
    } else {
      stdout.println("Num  Address")
      points.watchpoints.forEach { (_, v) -> stdout.println("%-4d ${v.addr.format16()}".format(v.num)) }
    }
  }

  fun printDisplayTable(displays: Map<Int, Address>) {
    if (displays.isEmpty()) {
      stdout.println("No displays")
    } else {
      stdout.println("Num  Address")
      displays.forEach { (k, v) -> stdout.println("%-4d ${v.format16()}".format(k)) }
    }
  }

  fun printBacktrace(stack: CallStackManager) {
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

  fun printPpuState() {
    val mapper = jacksonObjectMapper()
      .enable(INDENT_OUTPUT)
      .enable(SORT_PROPERTIES_ALPHABETICALLY)
    stdout.println(mapper.writeValueAsString(diag.ppu.state))
  }

  fun printAtAddress(addr: Address) {
    stdout.println(diag.peek(addr).format8())
  }

  fun printInstContext(base: Address, num: Int) {
    var pc = base
    repeat(num) {
      val decoded = diag.cpu.decodeAt(pc)
      stdout.println("${pc.format16()}: ${decoded.instruction}")
      pc = decoded.nextPc
    }
  }

  fun printDisplays(displays: Map<Int, Address>) {
    displays.forEach { (k, v) ->
      stdout.println("${k}: ${v.format16()} = ${diag.peek(v).format8()}")
    }
  }

  fun printStores(stores: List<Pair<Address, Data>>) {
    stores.forEach { (addr, data) ->
      stdout.println("    ${addr.format16()} <- ${data.format8()}")
    }
  }

  fun printStep(step: NextStep) {
    when (step) {
      INSTRUCTION -> stdout.println("${diag.cpu.regs.pc.format16()}: ${instAt(diag.cpu.regs.pc)}")
      else -> stdout.println("${step.name} triggered")
    }
  }

  fun printPointHit(point: Point) {
    stdout.println("Hit ${point.format()}")
  }

  fun printPointCreated(point: Point) {
    stdout.println("Created ${point.format()}")
  }

  fun printPointDeleted(point: Point?) {
    when (point) {
      null -> stdout.println("No such breakpoint or watchpoint")
      else -> stdout.println("Deleted ${point.format()}")
    }
  }

  fun printPointDeletedAll() {
    stdout.println("Deleted all breakpoints & watchpoints")
  }

  private fun Point.format() = when (this) {
    is Breakpoint -> "breakpoint #${num}: ${pc.format16()} -> ${instAt(pc)}"
    is Watchpoint -> "watchpoint #${num}: ${addr.format16()}"
  }

  private fun instAt(pc: Address) = diag.cpu.decodeAt(pc).instruction
}
