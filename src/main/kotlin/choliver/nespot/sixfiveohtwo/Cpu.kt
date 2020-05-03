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
  private val pollReset: () -> Boolean,   // Level-triggered
  private val pollIrq: () -> Boolean,     // Level-triggered
  private val pollNmi: () -> Boolean      // Edge-triggered
) {
  // Instance-level variables to keep the code clean
  private var extraCycles: Int = 0
  private var operand: Operand = Implied
  private var addr: Address = 0x0000
  private var state = State()
  private var prevNmi = _0

  private val decoder = InstructionDecoder(memory)

  // The order here represents interrupt priority
  fun executeStep(): Int {
    val reset = pollReset()
    val nmi = pollNmi()
    val irq = pollIrq()
    val ret = when {
      reset -> vector(VECTOR_RESET, updateStack = false, disableIrq = true)
      nmi && !prevNmi -> vector(VECTOR_NMI, updateStack = true, disableIrq = false)
      irq && !state.p.i -> vector(VECTOR_IRQ, updateStack = true, disableIrq = true)
      else -> executeInstruction()
    }
    prevNmi = nmi
    return ret
  }

  private fun vector(addr: Address, updateStack: Boolean, disableIrq: Boolean): Int {
    interrupt(addr, updateStack = updateStack, setBreakFlag = false)
    state.p.i = state.p.i || disableIrq
    return NUM_INTERRUPT_CYCLES
  }

  private fun executeInstruction(): Int {
    val decoded = decodeAt(state.pc)
    state.pc = decoded.nextPc
    operand = decoded.instruction.operand
    addr = decoded.addr
    extraCycles = 0
    execute(decoded.instruction.opcode)
    return decoded.numCycles + extraCycles
  }

  private fun decodeAt(pc: Address) = decoder.decode(pc = pc, x = state.x, y = state.y)

  private fun execute(op: Opcode) {
    state.apply {
      when (op) {
        ADC -> add(resolve())
        SBC -> add(resolve() xor 0xFF)

        CMP -> compare(a, resolve())
        CPX -> compare(x, resolve())
        CPY -> compare(y, resolve())

        DEC -> storeResult(resolve() - 1)
        DEX -> {
          x = (x - 1).data()
          updateZN(x)
        }
        DEY -> {
          y = (y - 1).data()
          updateZN(y)
        }

        INC -> storeResult(resolve() + 1)
        INX -> {
          x = (x + 1).data()
          updateZN(x)
        }
        INY -> {
          y = (y + 1).data()
          updateZN(y)
        }

        ASL -> {
          val data = resolve()
          storeResult(data shl 1)
          p.c = data.isBitSet(7)
        }
        LSR -> {
          val data = resolve()
          storeResult(data shr 1)
          p.c = data.isBitSet(0)
        }
        ROL -> {
          val data = resolve()
          storeResult((data shl 1) or (if (p.c) 1 else 0))
          p.c = data.isBitSet(7)
        }
        ROR -> {
          val data = resolve()
          storeResult((data shr 1) or (if (p.c) 0x80 else 0))
          p.c = data.isBitSet(0)
        }

        AND -> {
          a = a and resolve()
          updateZN(a)
        }
        ORA -> {
          a = a or resolve()
          updateZN(a)
        }
        EOR -> {
          a = a xor resolve()
          updateZN(a)
        }

        BIT -> {
          val data = resolve()
          p.z = (a and data).isZero()
          p.n = data.isNeg()
          p.v = data.isBitSet(6)
        }

        LDA -> {
          a = resolve()
          updateZN(a)
        }
        LDX -> {
          x = resolve()
          updateZN(x)
        }
        LDY -> {
          y = resolve()
          updateZN(y)
        }

        STA -> memory[addr] = a
        STX -> memory[addr] = x
        STY -> memory[addr] = y

        PHP -> push(p.data() or 0x10)  // Most online references state that PHP also sets B on stack
        PHA -> push(a)
        PLP -> p = pop().toFlags()
        PLA -> {
          a = pop()
          updateZN(a)
        }

        JMP -> pc = addr

        JSR -> {
          // One before next instruction (note we already advanced PC)
          push((pc - 1).hi())
          push((pc - 1).lo())
          pc = addr
        }

        RTS -> pc = (addr(lo = pop(), hi = pop()) + 1).addr()

        RTI -> {
          p = pop().toFlags()
          pc = addr(lo = pop(), hi = pop())
        }

        BRK -> interrupt(VECTOR_IRQ, updateStack = true, setBreakFlag = true)

        BPL -> branch(!p.n)
        BMI -> branch(p.n)
        BVC -> branch(!p.v)
        BVS -> branch(p.v)
        BCC -> branch(!p.c)
        BCS -> branch(p.c)
        BNE -> branch(!p.z)
        BEQ -> branch(p.z)

        TXA -> {
          a = x
          updateZN(a)
        }
        TYA -> {
          a = y
          updateZN(a)
        }
        TXS -> s = x
        TAY -> {
          y = a
          updateZN(y)
        }
        TAX -> {
          x = a
          updateZN(x)
        }
        TSX -> {
          x = s
          updateZN(x)
        }

        CLC -> p.c = _0
        CLD -> p.d = _0
        CLI -> p.i = _0
        CLV -> p.v = _0
        SEC -> p.c = _1
        SED -> p.d = _1
        SEI -> p.i = _1

        NOP -> { }
      }
    }
  }

  private fun branch(cond: Boolean) {
    if (cond) {
      extraCycles++
      if (((state.pc xor addr) and 0xFF00) != 0) {
        extraCycles++   // Page change
      }
      state.pc = addr
    }
  }

  private fun push(data: Data) {
    memory[state.s or 0x100] = data
    state.s = (state.s - 1).data()
  }

  private fun pop(): Data {
    state.s = (state.s + 1).data()
    return memory[state.s or 0x100]
  }

  private fun add(rhs: Data) = state.apply {
    val c = p.c
    val raw = a + rhs + (if (c) 1 else 0)
    val result = raw.data()
    val sameOperandSigns = (a.isNeg() == rhs.isNeg())
    val differentResultSign = (a.isNeg() != result.isNeg())

    a = result
    p.c = raw.isBitSet(8)
    p.v = sameOperandSigns && differentResultSign
    updateZN(result)
  }

  private fun compare(lhs: Data, rhs: Data) = state.apply {
    val raw = (lhs + (rhs xor 0xFF) + 1)
    val result = raw.data()

    p.c = raw.isBitSet(8)
    updateZN(result)
  }

  private fun interrupt(vector: Address, updateStack: Boolean, setBreakFlag: Boolean) = state.apply {
    if (updateStack) {
      push(pc.hi())
      push(pc.lo())
      push(p.data() or (if (setBreakFlag) 0x10 else 0x00))
    }

    pc = addr(
      lo = memory[vector],
      hi = memory[vector + 1]
    )
  }

  private fun resolve() = when (operand) {
    is Accumulator -> state.a
    is Immediate -> (operand as Immediate).literal
    else -> memory[addr]
  }

  private fun storeResult(data: Data) {
    val d = data.data()
    if (operand is Accumulator) {
      state.a = d
    } else {
      memory[addr] = d
    }
    updateZN(d)
  }

  private fun updateZN(data: Data) {
    state.p.z = data.isZero()
    state.p.n = data.isNeg()
  }

  inner class Diagnostics internal constructor() {
    var state
      get() = this@Cpu.state
      set(value) { this@Cpu.state = value.copy() }
    fun decodeAt(pc: Address) = this@Cpu.decodeAt(pc)
  }

  val diagnostics = Diagnostics()

  companion object {
    const val VECTOR_NMI: Address = 0xFFFA
    const val VECTOR_RESET: Address = 0xFFFC
    const val VECTOR_IRQ: Address = 0xFFFE

    const val NUM_INTERRUPT_CYCLES = 7
  }
}
