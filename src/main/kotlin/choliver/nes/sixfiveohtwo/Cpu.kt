package choliver.nes.sixfiveohtwo

import choliver.nes.*
import choliver.nes.sixfiveohtwo.model.Opcode
import choliver.nes.sixfiveohtwo.model.Opcode.*
import choliver.nes.sixfiveohtwo.model.Operand
import choliver.nes.sixfiveohtwo.model.Operand.*
import choliver.nes.sixfiveohtwo.model.State
import choliver.nes.sixfiveohtwo.model.toFlags
import choliver.nes.sixfiveohtwo.utils._0
import choliver.nes.sixfiveohtwo.utils._1


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

  fun runSteps(num: Int): Int {
    var n = 0
    repeat(num) {
      n += handleInterruptOrStep()
    }
    return n
  }

  private fun handleInterruptOrStep() = when {
    pollReset() -> vector(VECTOR_RESET, updateStack = false, disableIrq = true)
    pollNmi() -> vector(VECTOR_NMI, updateStack = true, disableIrq = false)
    pollIrq() -> if (_state.P.I) step() else vector(VECTOR_IRQ, updateStack = true, disableIrq = false)
    else -> step()
  }

  private fun vector(addr: Address, updateStack: Boolean, disableIrq: Boolean): Int {
    interrupt(addr, updateStack = updateStack, setBreakFlag = false)
    state.P.I = disableIrq || state.P.I
    return NUM_INTERRUPT_CYCLES
  }

  private fun step(): Int {
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

        DEC -> storeResult(resolve() - 1) // TODO - where should masking occur?
        DEX -> {
          X = (X - 1).data()
          P.Z = X.isZero()
          P.N = X.isNeg()
        }
        DEY -> {
          Y = (Y - 1).data()
          P.Z = Y.isZero()
          P.N = Y.isNeg()
        }

        INC -> storeResult(resolve() + 1) // TODO - where should masking occur?
        INX -> {
          X = (X + 1).data()
          P.Z = X.isZero()
          P.N = X.isNeg()
        }
        INY -> {
          Y = (Y + 1).data()
          P.Z = Y.isZero()
          P.N = Y.isNeg()
        }

        ASL -> {
          val data = resolve()
          storeResult(data shl 1) // TODO - where does masking occur?
          P.C = data.isBitSet(7)
        }
        LSR -> {
          val data = resolve()
          storeResult(data shr 1)
          P.C = data.isBitSet(0)
        }
        ROL -> {
          val data = resolve()
          storeResult((data shl 1) or (if (P.C) 1 else 0)) // TODO - where does masking occur?
          P.C = data.isBitSet(7)
        }
        ROR -> {
          val data = resolve()
          storeResult((data shr 1) or (if (P.C) 0x80 else 0))
          P.C = data.isBitSet(0)
        }

        AND -> {
          A = A and resolve()
          P.Z = A.isZero()
          P.N = A.isNeg()
        }
        ORA -> {
          A = A or resolve()
          P.Z = A.isZero()
          P.N = A.isNeg()
        }
        EOR -> {
          A = A xor resolve()
          P.Z = A.isZero()
          P.N = A.isNeg()
        }

        BIT -> {
          val data = resolve()
          val tmp = A and data
          P.Z = tmp.isZero()
          P.N = tmp.isNeg()
          P.V = data.isBitSet(6)
        }

        LDA -> {
          A = resolve()
          P.Z = A.isZero()
          P.N = A.isNeg()
        }
        LDX -> {
          X = resolve()
          P.Z = X.isZero()
          P.N = X.isNeg()
        }
        LDY -> {
          Y = resolve()
          P.Z = Y.isZero()
          P.N = Y.isNeg()
        }

        STA -> memory.store(addr, A)
        STX -> memory.store(addr, X)
        STY -> memory.store(addr, Y)

        PHP -> {
          memory.store(stackAddr(S), P.data() or 0x10)  // Most online references state that PHP also sets B on stack
          S = (S - 1).data()
        }
        PHA -> {
          memory.store(stackAddr(S), A)
          S = (S - 1).data()
        }
        PLP -> {
          P = memory.load(stackAddr(S + 1)).toFlags()
          S = (S + 1).data()
        }
        PLA -> {
          A = memory.load(stackAddr(S + 1))
          S = (S + 1).data()
          P.Z = A.isZero()
          P.N = A.isNeg()
        }

        JMP -> PC = addr

        JSR -> {
          // One before next instruction (note we already advanced PC)
          memory.store(stackAddr(S), (PC - 1).hi())
          memory.store(stackAddr(S - 1), (PC - 1).lo())
          S = (S - 2).data()
          PC = addr
        }

        RTS -> {
          PC = (
            addr(
              lo = memory.load(stackAddr(S + 1)),
              hi = memory.load(stackAddr(S + 2))) + 1
            ).addr()
          S = (S + 2).data()
        }

        RTI -> {
          P = memory.load(stackAddr(S + 1)).toFlags()
          PC = addr(
            lo = memory.load(stackAddr(S + 2)),
            hi = memory.load(stackAddr(S + 3))
          )
          S = (S + 3).data()
        }

        BRK -> interrupt(VECTOR_IRQ, updateStack = true, setBreakFlag = true)

        BPL -> PC = if (!P.N) addr else PC
        BMI -> PC = if (P.N) addr else PC
        BVC -> PC = if (!P.V) addr else PC
        BVS -> PC = if (P.V) addr else PC
        BCC -> PC = if (!P.C) addr else PC
        BCS -> PC = if (P.C) addr else PC
        BNE -> PC = if (!P.Z) addr else PC
        BEQ -> PC = if (P.Z) addr else PC

        TXA -> {
          A = X
          P.Z = A.isZero()
          P.N = A.isNeg()
        }
        TYA -> {
          A = Y
          P.Z = A.isZero()
          P.N = A.isNeg()
        }
        TXS -> S = X
        TAY -> {
          Y = A
          P.Z = Y.isZero()
          P.N = Y.isNeg()
        }
        TAX -> {
          X = A
          P.Z = X.isZero()
          P.N = X.isNeg()
        }
        TSX -> {
          X = S
          P.Z = X.isZero()
          P.N = X.isNeg()
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

  private fun add(rhs: Data) = _state.apply {
    val c = P.C
    val raw = A + rhs + (if (c) 1 else 0)
    val result = raw.data()
    val sameOperandSigns = (A.isNeg() == rhs.isNeg())
    val differentResultSign = (A.isNeg() != result.isNeg())

    A = result
    P.C = raw.isBitSet(8)
    P.V = sameOperandSigns && differentResultSign
    P.Z = result.isZero()
    P.N = result.isNeg()
  }

  private fun compare(lhs: Data, rhs: Data) = _state.apply {
    val raw = (lhs + (rhs xor 0xFF) + 1)
    val result = raw.data()

    P.C = raw.isBitSet(8)
    P.Z = result.isZero()
    P.N = result.isNeg()
  }

  private fun interrupt(vector: Address, updateStack: Boolean, setBreakFlag: Boolean) = _state.apply {
    if (updateStack) {
      memory.store(stackAddr(S), PC.hi())
      memory.store(stackAddr(S - 1), PC.lo())
      memory.store(stackAddr(S - 2), P.data() or (if (setBreakFlag) 0x10 else 0x00))
      S = (S - 3).data()
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
    val data = data.data()
    _state.apply {
      if (operand is Accumulator) {
        A = data
      } else {
        memory.store(addr, data)
      }
      P.Z = data.isZero()
      P.N = data.isNeg()
    }
  }

  private fun stackAddr(S: Data): Address = (0x0100 + (S and 0xFF))

  companion object {
    const val VECTOR_NMI: Address = 0xFFFA
    const val VECTOR_RESET: Address = 0xFFFC
    const val VECTOR_IRQ: Address = 0xFFFE

    const val NUM_INTERRUPT_CYCLES = 7
  }
}
