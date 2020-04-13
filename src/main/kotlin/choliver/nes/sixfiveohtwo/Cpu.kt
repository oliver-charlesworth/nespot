package choliver.nes.sixfiveohtwo

import choliver.nes.*
import choliver.nes.sixfiveohtwo.model.*
import choliver.nes.sixfiveohtwo.model.Opcode.*
import choliver.nes.sixfiveohtwo.model.Operand.Accumulator
import choliver.nes.sixfiveohtwo.model.Operand.Immediate
import choliver.nes.sixfiveohtwo.utils._0
import choliver.nes.sixfiveohtwo.utils._1
import mu.KotlinLogging

typealias F<T, R> = State.(T) -> R

class Cpu(
  private val memory: Memory
) {
  private val logger = KotlinLogging.logger {}

  private var _state: State = State()
  val state get() = _state

  private val decoder = InstructionDecoder()
  private val alu = Alu()
  private val addrCalc = AddressCalculator(memory)

  fun reset() = vector(VECTOR_RESET, updateStack = false, disableIrq = true)
  fun irq() = vector(VECTOR_IRQ, updateStack = true, disableIrq = false)  // TODO - only if I = _1
  fun nmi() = vector(VECTOR_NMI, updateStack = true, disableIrq = false)

  private fun vector(addr: Address, updateStack: Boolean, disableIrq: Boolean) {
    val context = Ctx(
      _state,
      Operand.Implied,
      0,
      null
    )
    _state = context
      .interrupt(addr, updateStack = updateStack, setBreakFlag = false)
      .updateI { disableIrq }
      .state

    logger.info("Vectoring to ${_state.PC}")
  }

  fun step() {
    val decoded = decodeAt(_state.PC)
    _state = _state.with(PC = decoded.nextPc)
    val context = Ctx(
      _state,
      decoded.instruction.operand,
      calcAddr(decoded.instruction),
      null
    )
    _state = context.execute(decoded.instruction.opcode).state
  }

  // TODO - combine
  fun decodeAt(pc: ProgramCounter) = decoder.decode(memory, pc)
  fun calcAddr(instruction: Instruction) = addrCalc.calculate(instruction.operand, _state)

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

    ASL -> resolve().shift { alu.asl(q = it) }
    LSR -> resolve().shift { alu.lsr(q = it) }
    ROL -> resolve().shift { alu.rol(q = it, c = P.C) }
    ROR -> resolve().shift { alu.ror(q = it, c = P.C) }

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
      .push { (PC - 1).H }   // One before next instruction (note we already advanced PC)
      .push { (PC - 1).L }
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
    is Immediate -> calc { operand.literal }
    else -> load { addr }
  }

  private fun Ctx<Data>.add(f: F<Data, Data>) = this
    .calc { alu.adc(a = A, b = f(it), c = P.C, d = P.D) }
    .updateA { it.q }
    .updateC { it.c }
    .updateV { it.v }

  private fun Ctx<Data>.compare(f: F<Data, Data>) = this
    .calc { alu.adc(a = f(it), b = data xor 0xFF, c = _1, d = _0) }  // Ignores borrow and decimal mode
    .updateZN { it.q }
    .updateC { it.c }

  private fun Ctx<Data>.shift(f: F<Data, Alu.Output>) = this
    .calc(f)
    .storeResult { it.q }
    .updateC { it.c }

  private fun <T> Ctx<T>.branch(f: F<T, Boolean>) = updatePC { if (f(state, data)) addr.toPC() else PC }

  private fun <T> Ctx<T>.interrupt(vector: Address, updateStack: Boolean, setBreakFlag: Boolean) = this
    .run {
      if (updateStack) {
        this
          .push { PC.H }
          .push { PC.L }
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

  private fun <T> Ctx<T>.updateA(f: F<T, Data>) = updateD(f) { with(A = it, Z = it.isZero(), N = it.isNeg()) }
  private fun <T> Ctx<T>.updateX(f: F<T, Data>) = updateD(f) { with(X = it, Z = it.isZero(), N = it.isNeg()) }
  private fun <T> Ctx<T>.updateY(f: F<T, Data>) = updateD(f) { with(Y = it, Z = it.isZero(), N = it.isNeg()) }
  private fun <T> Ctx<T>.updateS(f: F<T, Data>) = updateD(f) { with(S = it) }
  private fun <T> Ctx<T>.updateP(f: F<T, Data>) = updateD(f) { copy(P = it.toFlags()) }
  private fun <T> Ctx<T>.updatePC(f: F<T, ProgramCounter>) = update(f) { with(PC = it) }
  private fun <T> Ctx<T>.updatePCL(f: F<T, Data>) = updateD(f) { with(PC = PC.copy(L = it)) }
  private fun <T> Ctx<T>.updatePCH(f: F<T, Data>) = updateD(f) { with(PC = PC.copy(H = it)) }
  private fun <T> Ctx<T>.updateC(f: F<T, Boolean>) = update(f) { with(C = it) }
  private fun <T> Ctx<T>.updateD(f: F<T, Boolean>) = update(f) { with(D = it) }
  private fun <T> Ctx<T>.updateI(f: F<T, Boolean>) = update(f) { with(I = it) }
  private fun <T> Ctx<T>.updateV(f: F<T, Boolean>) = update(f) { with(V = it) }
  private fun <T> Ctx<T>.updateZN(f: F<T, Data>) = updateD(f) { with(Z = it.isZero(), N = it.isNeg()) }

  private fun <T> Ctx<T>.updateD(f: F<T, Data>, g: State.(Data) -> State) = copy(state = g(state, f(state, data).data()))
  private fun <T, R> Ctx<T>.update(f: F<T, R>, g: State.(R) -> State) = copy(state = g(state, f(state, data)))

  private fun stackAddr(S: Data): Address = (0x0100 + S)

  private data class Ctx<T>(
    val state: State,
    val operand: Operand,
    val addr: Address,
    val data: T
  )

  companion object {
    const val VECTOR_NMI: Address = 0xFFFA
    const val VECTOR_RESET: Address = 0xFFFC
    const val VECTOR_IRQ: Address = 0xFFFE
  }
}
