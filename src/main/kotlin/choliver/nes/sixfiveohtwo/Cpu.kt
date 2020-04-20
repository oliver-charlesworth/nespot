package choliver.nes.sixfiveohtwo

import choliver.nes.*
import choliver.nes.sixfiveohtwo.model.Opcode
import choliver.nes.sixfiveohtwo.model.Opcode.*
import choliver.nes.sixfiveohtwo.model.Operand
import choliver.nes.sixfiveohtwo.model.Operand.Accumulator
import choliver.nes.sixfiveohtwo.model.Operand.Immediate
import choliver.nes.sixfiveohtwo.model.State
import choliver.nes.sixfiveohtwo.model.toFlags
import choliver.nes.sixfiveohtwo.utils._0
import choliver.nes.sixfiveohtwo.utils._1

typealias F<T, R> = State.(T) -> R

class Cpu(
  private val memory: Memory,
  private val pollReset: () -> Boolean,
  private val pollIrq: () -> Boolean,
  private val pollNmi: () -> Boolean,
  initialState: State = State()
) {
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
    val context = Ctx(
      _state,
      Operand.Implied,
      0,
      null
    )
    _state = context
      .interrupt(addr, updateStack = updateStack, setBreakFlag = false)
      .updateI { disableIrq || P.I }
      .state
    return NUM_INTERRUPT_CYCLES
  }

  private fun step(): Int {
    val decoded = decodeAt(_state.PC)
    _state.PC = decoded.nextPc
    val context = Ctx(
      _state,
      decoded.instruction.operand,
      decoded.addr,
      null
    )
    _state = context.execute(decoded.instruction.opcode).state
    return decoded.numCycles
  }

  fun decodeAt(pc: Address) = decoder.decode(pc = pc, x = _state.X, y = _state.Y)

  private fun <T> Ctx<T>.execute(op: Opcode) = when (op) {
    ADC -> resolve().add { it }
    SBC -> resolve().add { it xor 0xFF }

    CMP -> resolve().compare { A }
    CPX -> resolve().compare { X }
    CPY -> resolve().compare { Y }

    DEC -> resolve().storeResult { it - 1 }
    DEX -> updateX { X - 1 }
    DEY -> updateY { Y - 1 }

    INC -> resolve().storeResult { it + 1 }
    INX -> updateX { X + 1 }
    INY -> updateY { Y + 1 }

    ASL -> resolve()
      .storeResult { (it shl 1).data() }
      .updateC { it.isBitSet(7) }
    LSR -> resolve()
      .storeResult { (it shr 1).data() }
      .updateC { it.isBitSet(0) }
    ROL -> resolve()
      .storeResult { (it shl 1).data() + (if (P.C) 1 else 0) }
      .updateC { it.isBitSet(7) }
    ROR -> resolve()
      .storeResult { (it shr 1).data() + (if (P.C) 0x80 else 0) }
      .updateC { it.isBitSet(0) }

    AND -> resolve().updateA { A and it }
    ORA -> resolve().updateA { A or it }
    EOR -> resolve().updateA { A xor it }

    BIT -> resolve()
      .updateZN { A and it }
      .updateV { it.isBitSet(6) }

    LDA -> resolve().updateA { it }
    LDX -> resolve().updateX { it }
    LDY -> resolve().updateY { it }

    STA -> store { A }
    STX -> store { X }
    STY -> store { Y }

    PHP -> push { P.data() or 0x10 } // Most online references state that PHP also sets B on stack
    PHA -> push { A }
    PLP -> pop().updateP { it }
    PLA -> pop().updateA { it }

    JMP -> this
      .updatePCL { addr.lo() }
      .updatePCH { addr.hi() }

    JSR -> this
      .push { (PC - 1).hi() }   // One before next instruction (note we already advanced PC)
      .push { (PC - 1).lo() }
      .updatePCL { addr.lo() }
      .updatePCH { addr.hi() }

    RTS -> this
      .pop().updatePCL { it }
      .pop().updatePCH { it }
      .updatePC { PC + 1 }

    RTI -> this
      .pop().updateP { it }
      .pop().updatePCL { it }
      .pop().updatePCH { it }

    BRK -> interrupt(VECTOR_IRQ, updateStack = true, setBreakFlag = true)

    BPL -> branch { !P.N }
    BMI -> branch { P.N }
    BVC -> branch { !P.V }
    BVS -> branch { P.V }
    BCC -> branch { !P.C }
    BCS -> branch { P.C }
    BNE -> branch { !P.Z }
    BEQ -> branch { P.Z }

    TXA -> updateA { X }
    TYA -> updateA { Y }
    TXS -> updateS { X }
    TAY -> updateY { A }
    TAX -> updateX { A }
    TSX -> updateX { S }

    CLC -> updateC { _0 }
    CLD -> updateD { _0 }
    CLI -> updateI { _0 }
    CLV -> updateV { _0 }
    SEC -> updateC { _1 }
    SED -> updateD { _1 }
    SEI -> updateI { _1 }

    NOP -> this
  }

  private fun <T> Ctx<T>.resolve() = when (operand) {
    is Accumulator -> calc { A }
    is Immediate -> calc { (operand as Immediate).literal }
    else -> load { addr }
  }

  private fun Ctx<Data>.add(f: F<Data, Data>) = apply {
    val a = state.A
    val b = f(state, data)
    val c = state.P.C
    val raw = a + b + (if (c) 1 else 0)
    val result = raw.data()
    val sameOperandSigns = (a.isNeg() == b.isNeg())
    val differentResultSign = (a.isNeg() != result.isNeg())

    state.A = result
    state.P.C = raw.isBitSet(8)
    state.P.V = sameOperandSigns && differentResultSign
    state.P.Z = result.isZero()
    state.P.N = result.isNeg()
  }

  private fun Ctx<Data>.compare(f: F<Data, Data>) = apply {
    val raw = (f(state, data) + (data xor 0xFF) + 1)
    val result = raw.data()

    state.P.C = raw.isBitSet(8)
    state.P.Z = result.isZero()
    state.P.N = result.isNeg()
  }

  private fun <T> Ctx<T>.branch(f: F<T, Boolean>) = updatePC { if (f(state, data)) addr else PC }

  private fun <T> Ctx<T>.interrupt(vector: Address, updateStack: Boolean, setBreakFlag: Boolean) = this
    .run {
      if (updateStack) {
        this
          .push { PC.hi() }
          .push { PC.lo() }
          .push { P.data() or (if (setBreakFlag) 0x10 else 0x00) }
      } else {
        this
      }
    }
    .load { vector }.updatePCL { it }
    .load { vector + 1 }.updatePCH { it }

  private fun <T> Ctx<T>.push(f: F<T, Data>) = store(stackAddr(state.S), f).updateS { S - 1 }

  private fun <T> Ctx<T>.pop() = updateS { S + 1 }.load { stackAddr(S) }

  private fun <T> Ctx<T>.load(f: F<T, Address>) = calc { memory.load(f(it)) }

  private fun <T> Ctx<T>.storeResult(f: F<T, Data>): Ctx<T> {
    val data = f(state, data)
    return if (operand is Accumulator) {
      updateA { data }
    } else {
      store { data }.updateZN { data }
    }
  }

  private fun <T> Ctx<T>.store(f: F<T, Data>) = store(addr, f)
  private fun <T> Ctx<T>.store(addr: Address, f: F<T, Data>) = also { memory.store(addr, f(state, data).data()) }

  private fun <T, R> Ctx<T>.calc(f: F<T, R>) = Ctx(state, operand, addr, f(state, data))

  private fun <T> Ctx<T>.updateA(f: F<T, Data>) = updateD(f) {
    apply {
      A = it
      P.Z = it.isZero()
      P.N = it.isNeg()
    }
  }
  private fun <T> Ctx<T>.updateX(f: F<T, Data>) = updateD(f) {
    apply {
      X = it
      P.Z = it.isZero()
      P.N = it.isNeg()
    }
  }
  private fun <T> Ctx<T>.updateY(f: F<T, Data>) = updateD(f) {
    apply {
      Y = it
      P.Z = it.isZero()
      P.N = it.isNeg()
    }
  }
  private fun <T> Ctx<T>.updateS(f: F<T, Data>) = updateD(f) {
    apply {
      S = it
    }
  }
  private inline fun <T> Ctx<T>.updateP(f: F<T, Data>) = updateD(f) { apply { P = it.toFlags() } }
  private inline fun <T> Ctx<T>.updatePC(f: F<T, Address>) = update(f) { apply { PC = it } }
  private inline fun <T> Ctx<T>.updatePCL(f: F<T, Data>) = updateD(f) { apply { PC = addr(lo = it, hi = PC.hi()) } }
  private inline fun <T> Ctx<T>.updatePCH(f: F<T, Data>) = updateD(f) { apply { PC = addr(lo = PC.lo(), hi = it) } }
  private inline fun <T> Ctx<T>.updateC(f: F<T, Boolean>) = update(f) { apply { P.C = it } }
  private inline fun <T> Ctx<T>.updateD(f: F<T, Boolean>) = update(f) { apply { P.D = it } }
  private inline fun <T> Ctx<T>.updateI(f: F<T, Boolean>) = update(f) { apply { P.I = it } }
  private inline fun <T> Ctx<T>.updateV(f: F<T, Boolean>) = update(f) { apply { P.V = it } }
  private inline fun <T> Ctx<T>.updateZN(f: F<T, Data>) = updateD(f) { apply { P.Z = it.isZero(); P.N = it.isNeg() } }

  private inline fun <T> Ctx<T>.updateD(f: F<T, Data>, g: State.(Data) -> State) = apply {
    state = g(state, f(state, data).data())
  }
  private inline fun <T, R> Ctx<T>.update(f: F<T, R>, g: State.(R) -> State) = apply {
    state = g(state, f(state, data))
  }

  private fun stackAddr(S: Data): Address = (0x0100 + S)

  @MutableForPerfReasons
  private data class Ctx<T>(
    var state: State,
    var operand: Operand,
    var addr: Address,
    var data: T
  )

  companion object {
    const val VECTOR_NMI: Address = 0xFFFA
    const val VECTOR_RESET: Address = 0xFFFC
    const val VECTOR_IRQ: Address = 0xFFFE

    const val NUM_INTERRUPT_CYCLES = 7
  }
}
