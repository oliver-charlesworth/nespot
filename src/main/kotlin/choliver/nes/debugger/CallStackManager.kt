package choliver.nes.debugger

import choliver.nes.Address
import choliver.nes.Nes
import choliver.nes.addr
import choliver.nes.debugger.CallStackManager.Entry.Frame
import choliver.nes.debugger.CallStackManager.Entry.UserData
import choliver.nes.debugger.CallStackManager.FrameType.*
import choliver.nes.sixfiveohtwo.Cpu.Companion.VECTOR_IRQ
import choliver.nes.sixfiveohtwo.Cpu.Companion.VECTOR_NMI
import choliver.nes.sixfiveohtwo.Cpu.Companion.VECTOR_RESET
import choliver.nes.sixfiveohtwo.model.Opcode.*
import choliver.nes.sixfiveohtwo.model.Opcode.JSR
import choliver.nes.sixfiveohtwo.model.ProgramCounter
import choliver.nes.sixfiveohtwo.model.toPC
import java.util.*

class CallStackManager(
  private val nes: Nes.Instrumentation
) {
  private enum class Next {
    INSTRUCTION,
    RESET,
    NMI,
    IRQ
  }

  enum class FrameType {
    JSR,
    JSR_PARTIAL,
    RESET,
    NMI,
    IRQ
  }

  data class FrameInfo(
    val type: FrameType,
    val start: ProgramCounter,
    val current: ProgramCounter
  )

  sealed class Entry {
    object UserData : Entry()
    data class Frame(val idx: Int) : Entry() // Used to ensure uniqueness (required to use as map keys)
  }

  private val map = mutableMapOf<Entry, FrameInfo>()
  private val stack = Stack<Entry>()
  private var nextFrameIdx = 0
  private var valid = false // Becomes valid once S initialised
  private var next = Next.INSTRUCTION

  fun nextIsReset() {
    next = Next.RESET
  }

  fun nextIsNmi() {
    next = Next.NMI
  }

  fun nextIsIrq() {
    next = Next.IRQ
  }

  fun preStep() {
    when (next) {
      Next.INSTRUCTION -> preInstruction()
      Next.RESET -> preReset()
      Next.NMI -> preNmi()
      Next.IRQ -> preIrq()
    }
    next = Next.INSTRUCTION
  }

  private fun preInstruction() {
    val decoded = nes.decodeAt(nes.state.PC)
    val addr = nes.calcAddr(decoded.instruction)

    when (decoded.instruction.opcode) {
      TXS -> if (valid) {
        unsupported("overwriting stack pointer")
      } else {
        valid = true
      }

      // TODO
      //      STA, STX, STY -> if (addr in 0x0100..0x01FF) {
      //        unsupported("manual manipulation of stack content")
      //      }

      BRK -> unsupported("BRK")

      // TODO - check for stack overflow
      PHA, PHP -> stack.push(UserData)

      PLA, PLP -> popAndHandle(
        onUserData = {},  // Vanilla
        onFrame = {
          when (it.type) {
            FrameType.JSR -> pushFrame(JSR_PARTIAL, it.start)
            JSR_PARTIAL -> {} // Manually removed a stack frame
            NMI -> unsupported("manual unwinding of NMI frame")
            IRQ -> unsupported("manual unwinding of IRQ frame")
            RESET -> unsupported("stack underflow")
          }
        }
      )

      JSR -> pushFrame(FrameType.JSR, addr.toPC())

      RTS -> popAndHandle(
        onUserData = { unsupported("RTS for manually constructed frame") },
        onFrame = {
          when (it.type) {
            FrameType.JSR -> {} // Vanilla
            JSR_PARTIAL -> unsupported("RTS for partially unwound JSR frame")
            NMI -> unsupported("RTS for NMI frame")
            IRQ -> unsupported("RTS for IRQ frame")
            RESET -> unsupported("stack underflow")
          }
        }
      )

      RTI -> popAndHandle(
        onUserData = { unsupported("RTI for manually constructed frame") },
        onFrame = {
          when (it.type) {
            FrameType.JSR -> unsupported("RTI for JSR frame")
            JSR_PARTIAL -> unsupported("RTI for partially unwound JSR frame")
            NMI, IRQ -> {} // Vanilla
            RESET -> unsupported("stack underflow")
          }
        }
      )

      else -> {}
    }
  }

  private fun preReset() {
    nextFrameIdx = 0
    valid = false
    map.clear()
    stack.clear()
    pushFrame(RESET, peekVector(VECTOR_RESET))
  }

  private fun preNmi() {
    pushFrame(NMI, peekVector(VECTOR_NMI))  // TODO - check for stack overflow
  }

  private fun preIrq() {
    pushFrame(IRQ, peekVector(VECTOR_IRQ)) // TODO - check for stack overflow
  }

  fun postStep() {
    val latest = map.entries.last()
    latest.setValue(latest.value.copy(current = nes.state.PC))
  }

  private fun pushFrame(type: FrameType, start: ProgramCounter, current: ProgramCounter = start) {
    val frame = Frame(nextFrameIdx++)
    stack.push(frame)
    map[frame] = FrameInfo(type = type, start = start, current = current)
  }

  private fun popAndHandle(onUserData: () -> Unit, onFrame: (FrameInfo) -> Unit) {
    when (val removed = stack.pop()) {
      is UserData -> onUserData()
      is Frame -> onFrame(map.remove(removed)!!)
    }
  }

  private fun unsupported(msg: String): Nothing = throw NotImplementedError("Debugger doesn't support ${msg}")

  private fun peekVector(addr: Address) = addr(
    lo = nes.peek(addr), hi = nes.peek((addr + 1).addr())
  ).toPC()

  val depth get() = map.size

  val frames get() = map.values.reversed()
}
