package choliver.nespot.sixfiveohtwo

import choliver.nespot.*
import choliver.nespot.sixfiveohtwo.model.Opcode
import choliver.nespot.sixfiveohtwo.model.Opcode.*
import choliver.nespot.sixfiveohtwo.model.Operand
import choliver.nespot.sixfiveohtwo.model.Operand.*
import choliver.nespot.sixfiveohtwo.model.State
import choliver.nespot.sixfiveohtwo.model.toFlags
import choliver.nespot.sixfiveohtwo.utils._0
import choliver.nespot.sixfiveohtwo.utils._1


class Cpu(
  private val memory: Memory,
  private val pollReset: () -> Boolean,
  private val pollIrq: () -> Boolean,
  private val pollNmi: () -> Boolean,
  initialState: State = State()
) {
  // This flattened mutable operating state means this is strictly non-reentrant
  private var operand: Operand = Implied
  private var addr: Address = 0x0000
  private var _state = initialState
  val state get() = _state

  private val decoder = InstructionDecoder(memory)

  fun executeStep() = when {
    pollReset() -> vector(VECTOR_RESET, updateStack = false, disableIrq = true)
    pollNmi() -> vector(VECTOR_NMI, updateStack = true, disableIrq = false)
    pollIrq() -> if (_state.P.I) executeInstruction() else vector(VECTOR_IRQ, updateStack = true, disableIrq = false)
    else -> executeInstruction()
  }

  private fun vector(addr: Address, updateStack: Boolean, disableIrq: Boolean): Int {
    interrupt(addr, updateStack = updateStack, setBreakFlag = false)
    state.P.I = disableIrq || state.P.I
    return NUM_INTERRUPT_CYCLES
  }

  private fun executeInstruction(): Int {
    val decoded = decodeAt(_state.PC)
    _state.PC = decoded.nextPc
    operand = decoded.instruction.operand
    addr = decoded.addr
    execute(decoded.instruction.opcode)
    return decoded.numCycles
  }

  fun decodeAt(pc: Address) = decoder.decode(pc = pc, x = _state.X, y = _state.Y)

  private fun execute(op: Opcode) {
    _state.apply {
      when (op) {
        ADC -> add(resolve())
        SBC -> add(resolve() xor 0xFF)

        CMP -> compare(A, resolve())
        CPX -> compare(X, resolve())
        CPY -> compare(Y, resolve())

        DEC -> storeResult(resolve() - 1)
        DEX -> {
          X = (X - 1).data()
          updateZN(X)
        }
        DEY -> {
          Y = (Y - 1).data()
          updateZN(Y)
        }

        INC -> storeResult(resolve() + 1)
        INX -> {
          X = (X + 1).data()
          updateZN(X)
        }
        INY -> {
          Y = (Y + 1).data()
          updateZN(Y)
        }

        ASL -> {
          val data = resolve()
          storeResult(data shl 1)
          P.C = data.isBitSet(7)
        }
        LSR -> {
          val data = resolve()
          storeResult(data shr 1)
          P.C = data.isBitSet(0)
        }
        ROL -> {
          val data = resolve()
          storeResult((data shl 1) or (if (P.C) 1 else 0))
          P.C = data.isBitSet(7)
        }
        ROR -> {
          val data = resolve()
          storeResult((data shr 1) or (if (P.C) 0x80 else 0))
          P.C = data.isBitSet(0)
        }

        AND -> {
          A = A and resolve()
          updateZN(A)
        }
        ORA -> {
          A = A or resolve()
          updateZN(A)
        }
        EOR -> {
          A = A xor resolve()
          updateZN(A)
        }

        BIT -> {
          val data = resolve()
          val tmp = A and data
          P.Z = tmp.isZero()
          P.N = data.isNeg()
          P.V = data.isBitSet(6)
        }

        LDA -> {
          A = resolve()
          updateZN(A)
        }
        LDX -> {
          X = resolve()
          updateZN(X)
        }
        LDY -> {
          Y = resolve()
          updateZN(Y)
        }

        STA -> memory.store(addr, A)
        STX -> memory.store(addr, X)
        STY -> memory.store(addr, Y)

        PHP -> push(P.data() or 0x10)  // Most online references state that PHP also sets B on stack
        PHA -> push(A)
        PLP -> P = pop().toFlags()
        PLA -> {
          A = pop()
          updateZN(A)
        }

        JMP -> PC = addr

        JSR -> {
          // One before next instruction (note we already advanced PC)
          push((PC - 1).hi())
          push((PC - 1).lo())
          PC = addr
        }

        RTS -> PC = (addr(lo = pop(), hi = pop()) + 1).addr()

        RTI -> {
          P = pop().toFlags()
          PC = addr(lo = pop(), hi = pop())
        }

        BRK -> interrupt(VECTOR_IRQ, updateStack = true, setBreakFlag = true)

        BPL -> branch(!P.N)
        BMI -> branch(P.N)
        BVC -> branch(!P.V)
        BVS -> branch(P.V)
        BCC -> branch(!P.C)
        BCS -> branch(P.C)
        BNE -> branch(!P.Z)
        BEQ -> branch(P.Z)

        TXA -> {
          A = X
          updateZN(A)
        }
        TYA -> {
          A = Y
          updateZN(A)
        }
        TXS -> S = X
        TAY -> {
          Y = A
          updateZN(Y)
        }
        TAX -> {
          X = A
          updateZN(X)
        }
        TSX -> {
          X = S
          updateZN(X)
        }

        CLC -> P.C = _0
        CLD -> P.D = _0
        CLI -> P.I = _0
        CLV -> P.V = _0
        SEC -> P.C = _1
        SED -> P.D = _1
        SEI -> P.I = _1

        NOP -> { }
      }
    }
  }

  private fun branch(cond: Boolean) {
    state.PC = if (cond) addr else state.PC
  }

  private fun push(data: Data) {
    memory.store(state.S or 0x100, data)
    state.S = (state.S - 1).data()
  }

  private fun pop(): Data {
    state.S = (state.S + 1).data()
    return memory.load(state.S or 0x100)
  }

  private fun add(rhs: Data) = _state.apply {
    val c = P.C
    val raw = A + rhs + (if (c) 1 else 0)
    val result = raw.data()
    val sameOperandSigns = (A.isNeg() == rhs.isNeg())
    val differentResultSign = (A.isNeg() != result.isNeg())

    A = result
    P.C = raw.isBitSet(8)
    P.V = sameOperandSigns && differentResultSign
    updateZN(result)
  }

  private fun compare(lhs: Data, rhs: Data) = _state.apply {
    val raw = (lhs + (rhs xor 0xFF) + 1)
    val result = raw.data()

    P.C = raw.isBitSet(8)
    updateZN(result)
  }

  private fun interrupt(vector: Address, updateStack: Boolean, setBreakFlag: Boolean) = _state.apply {
    if (updateStack) {
      push(PC.hi())
      push(PC.lo())
      push( P.data() or (if (setBreakFlag) 0x10 else 0x00))
    }

    PC = addr(
      lo = memory.load(vector),
      hi = memory.load(vector + 1)
    )
  }

  private fun resolve() = when (operand) {
    is Accumulator -> _state.A
    is Immediate -> (operand as Immediate).literal
    else -> memory.load(addr)
  }

  private fun storeResult(data: Data) {
    val d = data.data()
    if (operand is Accumulator) {
      _state.A = d
    } else {
      memory.store(addr, d)
    }
    updateZN(d)
  }

  private fun updateZN(data: Data) {
    state.P.Z = data.isZero()
    state.P.N = data.isNeg()
  }

  companion object {
    const val VECTOR_NMI: Address = 0xFFFA
    const val VECTOR_RESET: Address = 0xFFFC
    const val VECTOR_IRQ: Address = 0xFFFE

    const val NUM_INTERRUPT_CYCLES = 7
  }
}
