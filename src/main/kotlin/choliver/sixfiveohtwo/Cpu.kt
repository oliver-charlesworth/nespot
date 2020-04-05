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

  data class StateAnd<T>(
    val state: State,
    val decoded: Decoded,
    val addr: UInt16,
    val data: T
  )

  // TODO - homogenise State and Memory paradigm
  fun execute(encoding: Array<UInt8>, state: State): State {
    val decoded = decoder.decode(encoding)
    val addr = addrCalc.calculate(decoded.addrMode, state)

    return with (state.advancePC(decoded).go(decoded, addr)) {
      when (decoded.op) {
        ADC -> resolveOperand()
          .then { alu.adc(a = A, b = it, c = P.C, d = P.D) }
          .updateA { it.q }
          .updateC { it.c }
          .updateV { it.v }

        SBC -> resolveOperand()
          .then { alu.adc(a = A, b = it.inv(), c = P.C, d = P.D) }
          .updateA { it.q }
          .updateC { it.c }
          .updateV { it.v }

        CMP -> resolveOperand()
          .then { alu.adc(a = A, b = it.inv(), c = _1, d = _0) }  // Ignores borrow and decimal mode
          .updateZN { it.q }
          .updateC { it.c }
        CPX -> this
          .then { alu.adc(a = A, b = X.inv(), c = _1, d = _0) }  // Ignores borrow and decimal mode
          .updateZN { it.q }
          .updateC { it.c }
        CPY -> this
          .then { alu.adc(a = A, b = Y.inv(), c = _1, d = _0) }  // Ignores borrow and decimal mode
          .updateZN { it.q }
          .updateC { it.c }

        DEC -> resolveOperand()
          .then { (it - 1u).u8() }
          .store(addr) { it }
          .updateZN { it }
        DEX -> updateX { (X - 1u).u8() }
        DEY -> updateY { (Y - 1u).u8() }

        INC -> resolveOperand()
          .then { (it + 1u).u8() }
          .store(addr) { it }
          .updateZN { it }
        INX -> updateX { (X + 1u).u8() }
        INY -> updateY { (Y + 1u).u8() }

        ASL -> resolveOperand()
          .then { alu.asl(q = it) }
          .updateFromShift(decoded, addr)
        LSR -> resolveOperand()
          .then { alu.lsr(q = it) }
          .updateFromShift(decoded, addr)
        ROL -> resolveOperand()
          .then { alu.rol(q = it, c = P.C) }
          .updateFromShift(decoded, addr)
        ROR -> resolveOperand()
          .then { alu.ror(q = it, c = P.C) }
          .updateFromShift(decoded, addr)

        AND -> resolveOperand().updateA { A and it }
        ORA -> resolveOperand().updateA { A or it }
        EOR -> resolveOperand().updateA { A xor it }

        BIT -> resolveOperand()
          .updateZN { A and it }
          .updateV { !(it and 0x40u).isZero() }

        LDA -> resolveOperand().updateA { it }
        LDX -> resolveOperand().updateX { it }
        LDY -> resolveOperand().updateY { it }

        STA -> store(addr) { A }
        STX -> store(addr) { X }
        STY -> store(addr) { Y }

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
        RTS -> state
          .popTo { updatePC(it.u16()) }
          .popTo { updatePC((combine(lo = PC.u8(), hi = it) + 1u).u16()) }
          .go(decoded, addr) // TODO - convert
        RTI -> this
          .pop().updateP { it }
          .pop().updatePCL { it }
          .pop().updatePCH { it }
        BRK -> TODO()

        BPL -> updatePC { if (!P.N) addr else PC }
        BMI -> updatePC { if (P.N) addr else PC }
        BVC -> updatePC { if (!P.V) addr else PC }
        BVS -> updatePC { if (P.V) addr else PC }
        BCC -> updatePC { if (!P.C) addr else PC }
        BCS -> updatePC { if (P.C) addr else PC }
        BNE -> updatePC { if (!P.Z) addr else PC }
        BEQ -> updatePC { if (P.Z) addr else PC }

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
  private fun StateAnd<Alu.Output>.updateFromShift(decoded: Decoded, addr: UInt16) =
    if (decoded.addrMode is Accumulator) {
      updateA { it.q }.updateC { it.c }
    } else {
      store(addr) { it.q }.updateZN { it.q }.updateC { it.c }
    }

  private fun <T> StateAnd<T>.push(f: F<T, UInt8>) = store(stackAddr(state.S), f).updateS { (state.S - 1u).u8() }

  private fun State.popTo(consume: State.(data: UInt8) -> State): State {
    val updated = updateS((S + 1u).u8())
    val data = memory.load(stackAddr(updated.S))
    return updated.consume(data)
  }

  private fun <T> StateAnd<T>.pop() = updateS { (S + 1u).u8() }
    .then { memory.load(stackAddr(S)) }

  private fun <T> StateAnd<T>.resolveOperand() = then {
    when (decoded.addrMode) {
      is Accumulator -> A
      is Immediate -> decoded.addrMode.literal
      else -> memory.load(addr)
    }
  }

  private fun State.go(decoded: Decoded, addr: UInt16) = StateAnd(this, decoded, addr, null)
  private fun <T> StateAnd<T>.store(addr: UInt16, f: F<T, UInt8>) = also { memory.store(addr, f(state, data)) }

  private fun <T, R> StateAnd<T>.then(f: F<T, R>) = StateAnd(state, decoded, addr, f(state, data))
  
  private fun <T> StateAnd<T>.updateA(f: F<T, UInt8>) = with(f(state, data)) { copy(state = state.with(A = this, Z = isZero(), N = isNegative())) }
  private fun <T> StateAnd<T>.updateX(f: F<T, UInt8>) = with(f(state, data)) { copy(state = state.with(X = this, Z = isZero(), N = isNegative())) }
  private fun <T> StateAnd<T>.updateY(f: F<T, UInt8>) = with(f(state, data)) { copy(state = state.with(Y = this, Z = isZero(), N = isNegative())) }
  private fun <T> StateAnd<T>.updateS(f: F<T, UInt8>) = copy(state = state.with(S = f(state, data)))
  private fun <T> StateAnd<T>.updateP(f: F<T, UInt8>) = copy(state = state.copy(P = f(state, data).toFlags()))
  private fun <T> StateAnd<T>.updatePC(f: F<T, UInt16>) = copy(state = state.with(PC = f(state, data)))
  private fun <T> StateAnd<T>.updatePCL(f: F<T, UInt8>) = copy(state = state.with(PC = f(state, data).u16()))  // TODO - proper
  private fun <T> StateAnd<T>.updatePCH(f: F<T, UInt8>) = copy(state = state.with(PC = combine(lo = state.PC.u8(), hi = f(state, data))))  // TODO - proper
  private fun <T> StateAnd<T>.updateC(f: F<T, Boolean>) = copy(state = state.with(C = f(state, data)))
  private fun <T> StateAnd<T>.updateD(f: F<T, Boolean>) = copy(state = state.with(D = f(state, data)))
  private fun <T> StateAnd<T>.updateI(f: F<T, Boolean>) = copy(state = state.with(I = f(state, data)))
  private fun <T> StateAnd<T>.updateV(f: F<T, Boolean>) = copy(state = state.with(V = f(state, data)))
  private fun <T> StateAnd<T>.updateZN(f: F<T, UInt8>) = with(f(state, data)) { copy(state = state.with(Z = isZero(), N = isNegative())) }

  private fun State.updateS(s: UInt8) = with(S = s)
  private fun State.updatePC(pc: UInt16) = with(PC = pc)

  private fun State.advancePC(decoded: Decoded) = with(PC = (PC + decoded.length).u16())

  private fun stackAddr(S: UInt8) = (0x0100u + S).u16()
}
