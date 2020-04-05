package choliver.sixfiveohtwo

import choliver.sixfiveohtwo.AddressMode.Accumulator
import choliver.sixfiveohtwo.AddressMode.Immediate
import choliver.sixfiveohtwo.InstructionDecoder.Decoded
import choliver.sixfiveohtwo.Opcode.*
import choliver.sixfiveohtwo.utils._0
import choliver.sixfiveohtwo.utils._1

typealias F<T, R> = State.(T) -> R

class Cpu(
  private val memory: Memory
) {
  private val decoder = InstructionDecoder()
  private val alu = Alu()
  private val addrCalc = AddressCalculator(memory)

  // TODO - homogenise State and Memory paradigm
  fun execute(encoding: Array<UInt8>, state: State): State {
    val decoded = decoder.decode(encoding)
    val context = Ctx(
      state,
      decoded,
      addrCalc.calculate(decoded.addrMode, state),
      null
    )

    return with (context.advancePC()) {
      when (decoded.op) {
        ADC -> operand()
          .calc { alu.adc(a = A, b = it, c = P.C, d = P.D) }
          .updateA { it.q }
          .updateC { it.c }
          .updateV { it.v }

        SBC -> operand()
          .calc { alu.adc(a = A, b = it.inv(), c = P.C, d = P.D) }
          .updateA { it.q }
          .updateC { it.c }
          .updateV { it.v }

        CMP -> operand()
          .calc { alu.adc(a = A, b = it.inv(), c = _1, d = _0) }  // Ignores borrow and decimal mode
          .updateZN { it.q }
          .updateC { it.c }

        CPX -> this
          .calc { alu.adc(a = A, b = X.inv(), c = _1, d = _0) }  // Ignores borrow and decimal mode
          .updateZN { it.q }
          .updateC { it.c }

        CPY -> this
          .calc { alu.adc(a = A, b = Y.inv(), c = _1, d = _0) }  // Ignores borrow and decimal mode
          .updateZN { it.q }
          .updateC { it.c }

        DEC -> operand()
          .calc { (it - 1u).u8() }
          .store { it }
          .updateZN { it }
        DEX -> updateX { (X - 1u).u8() }
        DEY -> updateY { (Y - 1u).u8() }

        INC -> operand()
          .calc { (it + 1u).u8() }
          .store { it }
          .updateZN { it }
        INX -> updateX { (X + 1u).u8() }
        INY -> updateY { (Y + 1u).u8() }

        ASL -> operand()
          .calc { alu.asl(q = it) }
          .updateFromShift()

        LSR -> operand()
          .calc { alu.lsr(q = it) }
          .updateFromShift()

        ROL -> operand()
          .calc { alu.rol(q = it, c = P.C) }
          .updateFromShift()

        ROR -> operand()
          .calc { alu.ror(q = it, c = P.C) }
          .updateFromShift()

        AND -> operand().updateA { A and it }
        ORA -> operand().updateA { A or it }
        EOR -> operand().updateA { A xor it }

        BIT -> operand()
          .updateZN { A and it }
          .updateV { !(it and 0x40u).isZero() }

        LDA -> operand().updateA { it }
        LDX -> operand().updateX { it }
        LDY -> operand().updateY { it }

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
          .push { (PC - 1u).hi() }
          .push { (PC - 1u).lo() }
          .updatePCL { addr.lo() }  // TODO - this is cheating
          .updatePCH { addr.hi() }

        RTS -> this
          .pop().updatePCL { (it + 1u).u8() } // TODO - what about carry?
          .pop().updatePCH { it }

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
    }.state
  }

  // TODO - this is pretty gross
  private fun Ctx<Alu.Output>.updateFromShift() =
    if (decoded.addrMode is Accumulator) {
      updateA { it.q }.updateC { it.c }
    } else {
      store { it.q }.updateZN { it.q }.updateC { it.c }
    }

  private fun <T> Ctx<T>.operand() = calc {
    when (decoded.addrMode) {
      is Accumulator -> A
      is Immediate -> decoded.addrMode.literal
      else -> memory.load(addr)
    }
  }

  private fun <T> Ctx<T>.branch(f: F<T, Boolean>) = updatePC { if (f(state, data)) addr else PC }

  private fun <T> Ctx<T>.push(f: F<T, UInt8>) = store(stackAddr(state.S), f).updateS { (S - 1u).u8() }

  private fun <T> Ctx<T>.pop() = updateS { (S + 1u).u8() }.calc { memory.load(stackAddr(S)) }

  private fun <T> Ctx<T>.store(f: F<T, UInt8>) = store(addr, f)
  private fun <T> Ctx<T>.store(addr: UInt16, f: F<T, UInt8>) = also { memory.store(addr, f(state, data)) }

  private fun <T, R> Ctx<T>.calc(f: F<T, R>) = Ctx(state, decoded, addr, f(state, data))

  private fun <T> Ctx<T>.advancePC() = updatePC { (PC + decoded.length).u16() }

  private fun <T> Ctx<T>.updateA(f: F<T, UInt8>) = update(f) { with(A = it, Z = it.isZero(), N = it.isNegative()) }
  private fun <T> Ctx<T>.updateX(f: F<T, UInt8>) = update(f) { with(X = it, Z = it.isZero(), N = it.isNegative()) }
  private fun <T> Ctx<T>.updateY(f: F<T, UInt8>) = update(f) { with(Y = it, Z = it.isZero(), N = it.isNegative()) }
  private fun <T> Ctx<T>.updateS(f: F<T, UInt8>) = update(f) { with(S = it) }
  private fun <T> Ctx<T>.updateP(f: F<T, UInt8>) = update(f) { copy(P = it.toFlags()) }
  private fun <T> Ctx<T>.updatePC(f: F<T, UInt16>) = update(f) { with(PC = it) }
  private fun <T> Ctx<T>.updatePCL(f: F<T, UInt8>) = update(f) { with(PC = it.u16()) }  // TODO - proper
  private fun <T> Ctx<T>.updatePCH(f: F<T, UInt8>) = update(f) { with(PC = combine(lo = PC.u8(), hi = it)) }  // TODO - proper
  private fun <T> Ctx<T>.updateC(f: F<T, Boolean>) = update(f) { with(C = it) }
  private fun <T> Ctx<T>.updateD(f: F<T, Boolean>) = update(f) { with(D = it) }
  private fun <T> Ctx<T>.updateI(f: F<T, Boolean>) = update(f) { with(I = it) }
  private fun <T> Ctx<T>.updateV(f: F<T, Boolean>) = update(f) { with(V = it) }
  private fun <T> Ctx<T>.updateZN(f: F<T, UInt8>) = update(f) { with(Z = it.isZero(), N = it.isNegative()) }

  private fun <T, R> Ctx<T>.update(f: F<T, R>, g: State.(R) -> State) = copy(state = g(state, f(state, data)))

  private fun stackAddr(S: UInt8) = (0x0100u + S).u16()

  private data class Ctx<T>(
    val state: State,
    val decoded: Decoded,
    val addr: UInt16,
    val data: T
  )
}
