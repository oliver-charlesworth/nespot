package choliver.sixfiveohtwo

import choliver.sixfiveohtwo.model.*
import choliver.sixfiveohtwo.model.Operand.Accumulator
import choliver.sixfiveohtwo.model.Operand.Immediate
import choliver.sixfiveohtwo.model.Opcode.*
import choliver.sixfiveohtwo.utils._0
import choliver.sixfiveohtwo.utils._1

typealias F<T, R> = State.(T) -> R

class Cpu(
  private val memory: Memory
) {
  // Should sequence this via a state machine triggered on reset
  private var _state: State = State(
    PC = ProgramCounter(
      L = memory.load(VECTOR_RESET),
      H = memory.load((VECTOR_RESET + 1u).u16())
    ),
    P = Flags(I = _1)
  )
  val state get() = _state

  private val decoder = InstructionDecoder()
  private val alu = Alu()
  private val addrCalc = AddressCalculator(memory)

  fun next() {
    val decoded = decoder.decode(memory, _state.PC)
    val context = Ctx(
      _state.with(PC = decoded.pc),
      decoded.op,
      decoded.operand,
      addrCalc.calculate(decoded.operand, state),
      null
    )
    _state = context.execute().state
  }

  private fun <T> Ctx<T>.execute() = when (op) {
    ADC -> resolve().add { it }
    SBC -> resolve().add { it.inv() }

    CMP -> resolve().compare { it }
    CPX -> compare { X }
    CPY -> compare { Y }

    DEC -> resolve().storeResult { (it - 1u).u8() }
    DEX -> updateX { (X - 1u).u8() }
    DEY -> updateY { (Y - 1u).u8() }

    INC -> resolve().storeResult { (it + 1u).u8() }
    INX -> updateX { (X + 1u).u8() }
    INY -> updateY { (Y + 1u).u8() }

    ASL -> resolve().shift { alu.asl(q = it) }
    LSR -> resolve().shift { alu.lsr(q = it) }
    ROL -> resolve().shift { alu.rol(q = it, c = P.C) }
    ROR -> resolve().shift { alu.ror(q = it, c = P.C) }

    AND -> resolve().updateA { A and it }
    ORA -> resolve().updateA { A or it }
    EOR -> resolve().updateA { A xor it }

    BIT -> resolve()
      .updateZN { A and it }
      .updateV { !(it and 0x40u).isZero() }

    LDA -> resolve().updateA { it }
    LDX -> resolve().updateX { it }
    LDY -> resolve().updateY { it }

    STA -> store { A }
    STX -> store { X }
    STY -> store { Y }

    PHP -> push { P.u8() }
    PHA -> push { A }
    PLP -> pop().updateP { it }
    PLA -> pop().updateA { it }

    JMP -> this
      .updatePCL { addr.lo() }
      .updatePCH { addr.hi() }

    JSR -> this
      .push { (PC - 1u).H }   // PC already pointing at next instruction
      .push { (PC - 1u).L }
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

    BRK -> TODO()

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

  private fun <T> Ctx<T>.resolve() = calc {
    when (operand) {
      is Accumulator -> A
      is Immediate -> operand.literal
      else -> memory.load(addr)
    }
  }

  private fun Ctx<UInt8>.add(f: F<UInt8, UInt8>) = this
    .calc { alu.adc(a = A, b = f(it), c = P.C, d = P.D) }
    .updateA { it.q }
    .updateC { it.c }
    .updateV { it.v }

  private fun <T> Ctx<T>.compare(f: F<T, UInt8>) = this
    .calc { alu.adc(a = A, b = f(it).inv(), c = _1, d = _0) }  // Ignores borrow and decimal mode
    .updateZN { it.q }
    .updateC { it.c }

  private fun Ctx<UInt8>.shift(f: F<UInt8, Alu.Output>) = this
    .calc(f)
    .storeResult { it.q }
    .updateC { it.c }

  private fun <T> Ctx<T>.branch(f: F<T, Boolean>) = updatePC { if (f(state, data)) addr.toPC() else PC }

  private fun <T> Ctx<T>.push(f: F<T, UInt8>) = store(stackAddr(state.S), f).updateS { (S - 1u).u8() }

  private fun <T> Ctx<T>.pop() = updateS { (S + 1u).u8() }.calc { memory.load(stackAddr(S)) }

  private fun <T> Ctx<T>.storeResult(f: F<T, UInt8>): Ctx<T> {
    val data = f(state, data)
    return if (operand is Accumulator) {
      updateA { data }
    } else {
      store { data }.updateZN { data }
    }
  }

  private fun <T> Ctx<T>.store(f: F<T, UInt8>) = store(addr, f)
  private fun <T> Ctx<T>.store(addr: UInt16, f: F<T, UInt8>) = also { memory.store(addr, f(state, data)) }

  private fun <T, R> Ctx<T>.calc(f: F<T, R>) = Ctx(state, op, operand, addr, f(state, data))

  private fun <T> Ctx<T>.updateA(f: F<T, UInt8>) = update(f) { with(A = it, Z = it.isZero(), N = it.isNegative()) }
  private fun <T> Ctx<T>.updateX(f: F<T, UInt8>) = update(f) { with(X = it, Z = it.isZero(), N = it.isNegative()) }
  private fun <T> Ctx<T>.updateY(f: F<T, UInt8>) = update(f) { with(Y = it, Z = it.isZero(), N = it.isNegative()) }
  private fun <T> Ctx<T>.updateS(f: F<T, UInt8>) = update(f) { with(S = it) }
  private fun <T> Ctx<T>.updateP(f: F<T, UInt8>) = update(f) { copy(P = it.toFlags()) }
  private fun <T> Ctx<T>.updatePC(f: F<T, ProgramCounter>) = update(f) { with(PC = it) }
  private fun <T> Ctx<T>.updatePCL(f: F<T, UInt8>) = update(f) { with(PC = PC.copy(L = it)) }
  private fun <T> Ctx<T>.updatePCH(f: F<T, UInt8>) = update(f) { with(PC = PC.copy(H = it)) }
  private fun <T> Ctx<T>.updateC(f: F<T, Boolean>) = update(f) { with(C = it) }
  private fun <T> Ctx<T>.updateD(f: F<T, Boolean>) = update(f) { with(D = it) }
  private fun <T> Ctx<T>.updateI(f: F<T, Boolean>) = update(f) { with(I = it) }
  private fun <T> Ctx<T>.updateV(f: F<T, Boolean>) = update(f) { with(V = it) }
  private fun <T> Ctx<T>.updateZN(f: F<T, UInt8>) = update(f) { with(Z = it.isZero(), N = it.isNegative()) }

  private fun <T, R> Ctx<T>.update(f: F<T, R>, g: State.(R) -> State) = copy(state = g(state, f(state, data)))

  private fun stackAddr(S: UInt8) = (0x0100u + S).u16()

  private data class Ctx<T>(
    val state: State,
    val op: Opcode,
    val operand: Operand,
    val addr: UInt16,
    val data: T
  )

  companion object {
    val VECTOR_NMI = 0xFFFA.u16()
    val VECTOR_RESET = 0xFFFC.u16()
    val VECTOR_IRQ = 0xFFFE.u16()
  }
}
