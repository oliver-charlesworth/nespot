package choliver.nespot.cpu

import choliver.nespot.common.*
import choliver.nespot.cpu.AddressMode.ACCUMULATOR
import choliver.nespot.cpu.AddressMode.IMMEDIATE
import choliver.nespot.cpu.Cpu.NextStep.*
import choliver.nespot.cpu.InstructionDecoder.Decoded
import choliver.nespot.cpu.Opcode.*
import choliver.nespot.memory.Memory

class Cpu(
  private val memory: Memory,
  private val pollInterrupts: () -> Int
) {
  private var regs = Regs()
  private var prevNmi: Boolean = false
  private var interrupts = 0
  private var interruptsNext = 0
  private var extraCycles: Int = 0
  private val decoded = Decoded()
  private val decoder = InstructionDecoder(memory)

  fun executeStep(): Int {
    val next = nextStepType()
    prevNmi = (interrupts and INTERRUPT_NMI != 0)
    diagnostics.nextStepOverride = null   // Clear the override
    return when (next) {
      RESET -> vector(VECTOR_RESET, updateStack = false, disableIrq = true)
      NMI -> vector(VECTOR_NMI, updateStack = true, disableIrq = false)
      IRQ -> vector(VECTOR_IRQ, updateStack = true, disableIrq = true)
      INSTRUCTION -> executeInstruction()
    }
  }

  // Note the priority order
  private fun nextStepType(): NextStep {
    updateInterrupts()
    return diagnostics.nextStepOverride ?: when {
      (interrupts and INTERRUPT_RESET != 0) -> RESET
      (interrupts and INTERRUPT_NMI != 0) && !prevNmi -> NMI
      (interrupts and INTERRUPT_IRQ != 0) && !regs.p.i -> IRQ
      else -> INSTRUCTION
    }
  }

  // In reality, interrupts are polled part way through an instruction; we emulate this by delaying them by a *whole*
  // instruction.  This is required for New Zealand Story, because it does this:
  //
  // waitForNmi:
  //   bit $2002        # Checks the VBL flag!
  //   bpl waitForNmi
  //
  // nmi:
  //   lda $2002        # Clears the VBL flag!
  private fun updateInterrupts() {
    interrupts = interruptsNext
    interruptsNext = pollInterrupts()
  }

  private fun vector(addr: Address, updateStack: Boolean, disableIrq: Boolean): Int {
    regs.interrupt(addr, updateStack = updateStack, setBreakFlag = false)
    regs.p.i = regs.p.i || disableIrq
    return NUM_INTERRUPT_CYCLES
  }

  private fun executeInstruction(): Int {
    decoder.decode(decoded, pc = regs.pc, x = regs.x, y = regs.y)
    regs.pc = decoded.nextPc
    extraCycles = 0
    regs.execute()
    return decoded.numCycles + extraCycles
  }

  private fun Regs.execute() {
    when (decoded.opcode) {
      ADC -> add(resolve())
      SBC -> add(resolve() xor 0xFF)

      CMP -> compare(a, resolve())
      CPX -> compare(x, resolve())
      CPY -> compare(y, resolve())

      DEC -> {
        val data = resolve()
        storeResult(data, data - 1)
      }
      DEX -> {
        x = (x - 1).data()
        updateZN(x)
      }
      DEY -> {
        y = (y - 1).data()
        updateZN(y)
      }

      INC -> {
        val data = resolve()
        storeResult(data, data + 1)
      }
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
        storeResult(data, data shl 1)
        p.c = data.isBitSet(7)
      }
      LSR -> {
        val data = resolve()
        storeResult(data, data shr 1)
        p.c = data.isBitSet(0)
      }
      ROL -> {
        val data = resolve()
        storeResult(data, (data shl 1) or (if (p.c) 1 else 0))
        p.c = data.isBitSet(7)
      }
      ROR -> {
        val data = resolve()
        storeResult(data, (data shr 1) or (if (p.c) 0x80 else 0))
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

      STA -> memory[decoded.addr] = a
      STX -> memory[decoded.addr] = x
      STY -> memory[decoded.addr] = y

      PHP -> push(p.data() or 0x10)  // Most online references state that PHP also sets B on stack
      PHA -> push(a)
      PLP -> p = pop().toFlags()
      PLA -> {
        a = pop()
        updateZN(a)
      }

      JMP -> pc = decoded.addr

      JSR -> {
        // One before next instruction (note we already advanced PC)
        push((pc - 1).hi())
        push((pc - 1).lo())
        pc = decoded.addr
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

  private fun Regs.branch(cond: Boolean) {
    if (cond) {
      extraCycles++
      if (!samePage(pc, decoded.addr)) {
        extraCycles++
      }
      pc = decoded.addr
    }
  }

  private fun Regs.push(data: Data) {
    memory[s or 0x100] = data
    s = (s - 1).data()
  }

  private fun Regs.pop(): Data {
    s = (s + 1).data()
    return memory[s or 0x100]
  }

  private fun Regs.add(rhs: Data) {
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

  private fun Regs.compare(lhs: Data, rhs: Data) {
    val raw = (lhs + (rhs xor 0xFF) + 1)
    val result = raw.data()

    p.c = raw.isBitSet(8)
    updateZN(result)
  }

  private fun Regs.interrupt(vector: Address, updateStack: Boolean, setBreakFlag: Boolean) {
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

  private fun resolve() = when (decoded.addressMode) {
    ACCUMULATOR -> regs.a
    IMMEDIATE -> decoded.addr // The immediate masquerades as the address
    else -> memory[decoded.addr]
  }

  private fun Regs.storeResult(original: Data, data: Data) {
    val d = data.data()
    if (decoded.addressMode == ACCUMULATOR) {
      a = d
    } else {
      // Read-modify-write ops do dummy store of original value - must model correctly for MMC1
      memory[decoded.addr] = original
      memory[decoded.addr] = d
    }
    updateZN(d)
  }

  private fun Regs.updateZN(data: Data) {
    p.z = data.isZero()
    p.n = data.isNeg()
  }

  enum class NextStep {
    RESET,
    NMI,
    IRQ,
    INSTRUCTION
  }

  inner class Diagnostics internal constructor() {
    internal var nextStepOverride: NextStep? = RESET    // Reset on startup

    var regs
      get() = this@Cpu.regs
      set(value) { this@Cpu.regs = value.copy() }
    var nextStep
      get() = nextStepType()
      set(value) { nextStepOverride = value }
    fun decodeAt(pc: Address) = Decoded().apply { decoder.decode(this, pc, 0, 0) }
  }

  val diagnostics = Diagnostics()

  companion object {
    const val INTERRUPT_RESET = 0x01
    const val INTERRUPT_NMI = 0x02
    const val INTERRUPT_IRQ = 0x04

    const val VECTOR_NMI: Address = 0xFFFA
    const val VECTOR_RESET: Address = 0xFFFC
    const val VECTOR_IRQ: Address = 0xFFFE

    const val NUM_INTERRUPT_CYCLES = 7
  }
}
