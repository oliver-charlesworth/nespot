package choliver.nespot.debugger

import choliver.nespot.Address
import choliver.nespot.addr
import choliver.nespot.debugger.CallStackManager.Entry.Frame
import choliver.nespot.debugger.CallStackManager.Entry.UserData
import choliver.nespot.debugger.CallStackManager.FrameType.*
import choliver.nespot.nes.Nes
import choliver.nespot.sixfiveohtwo.Cpu.Companion.VECTOR_IRQ
import choliver.nespot.sixfiveohtwo.Cpu.Companion.VECTOR_NMI
import choliver.nespot.sixfiveohtwo.Cpu.Companion.VECTOR_RESET
import choliver.nespot.sixfiveohtwo.model.Opcode.*
import choliver.nespot.sixfiveohtwo.model.Opcode.JSR
import java.util.*

class CallStackManager(
  private val nes: Nes.Inspection
) {
  enum class FrameType {
    JSR,
    JSR_PARTIAL,
    RESET,
    NMI,
    IRQ
  }

  data class FrameInfo(
    val type: FrameType,
    val start: Address,
    val current: Address
  )

  sealed class Entry {
    object UserData : Entry()
    data class Frame(val idx: Int) : Entry() // Used to ensure uniqueness (required to use as map keys)
  }

  private val map = mutableMapOf<Entry, FrameInfo>()
  private val stack = Stack<Entry>()
  private var nextFrameIdx = 0
  private var valid = false // Becomes valid once S initialised

  fun preInstruction() {
    val decoded = nes.decodeAt(nes.state.PC)

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

      JSR -> pushFrame(FrameType.JSR, decoded.addr)

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

  fun preReset() {
    nextFrameIdx = 0
    valid = false
    map.clear()
    stack.clear()
    pushFrame(RESET, peekVector(VECTOR_RESET))
  }

  fun preNmi() {
    pushFrame(NMI, peekVector(VECTOR_NMI))  // TODO - check for stack overflow
  }

  fun preIrq() {
    pushFrame(IRQ, peekVector(VECTOR_IRQ)) // TODO - check for stack overflow
  }

  fun postInstruction() {
    val latest = map.entries.last()
    latest.setValue(latest.value.copy(current = nes.state.PC))
  }

  private fun pushFrame(type: FrameType, start: Address, current: Address = start) {
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
  )

  val depth get() = map.size

  val frames get() = map.values.reversed()
}
