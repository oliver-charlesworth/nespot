package choliver.sixfiveohtwo

import choliver.sixfiveohtwo.AddressMode.Accumulator
import choliver.sixfiveohtwo.AddressMode.Immediate
import choliver.sixfiveohtwo.InstructionDecoder.Decoded
import choliver.sixfiveohtwo.Opcode.*
import choliver.sixfiveohtwo.utils._0
import choliver.sixfiveohtwo.utils._1

class Cpu(
  private val memory: Memory
) {
  private val decoder = InstructionDecoder()
  private val alu = Alu()
  private val addrCalc = AddressCalculator(memory)

  data class StateAnd<T>(
    val state: State,
    val data: T
  )

  // TODO - homogenise State and Memory paradigm
  fun execute(encoding: Array<UInt8>, state: State): State {
    val decoded = decoder.decode(encoding)
    val addr = addrCalc.calculate(decoded.addrMode, state)

    return with (state.advancePC(decoded)) {
      when (decoded.op) {
        ADC -> resolveOperand(decoded, addr)
          .then { alu.adc(a = A, b = it, c = P.C, d = P.D) }
          .updateA { it.q }
          .updateC { it.c }
          .updateV { it.v }

        SBC -> resolveOperand(decoded, addr)
          .then { alu.adc(a = A, b = it.inv(), c = P.C, d = P.D) }
          .updateA { it.q }
          .updateC { it.c }
          .updateV { it.v }

        CMP -> resolveOperand(decoded, addr)
          .then { alu.adc(a = A, b = it.inv(), c = _1, d = _0) }  // Ignores borrow and decimal mode
          .updateZN { it.q }
          .updateC { it.c }
        CPX -> go()
          .then { alu.adc(a = A, b = X.inv(), c = _1, d = _0) }  // Ignores borrow and decimal mode
          .updateZN { it.q }
          .updateC { it.c }
        CPY -> go()
          .then { alu.adc(a = A, b = Y.inv(), c = _1, d = _0) }  // Ignores borrow and decimal mode
          .updateZN { it.q }
          .updateC { it.c }

        DEC -> resolveOperand(decoded, addr)
          .then { (it - 1u).u8() }
          .store(addr) { it }
          .updateZN { it }
        DEX -> go().updateX { (X - 1u).u8() }
        DEY -> go().updateY { (Y - 1u).u8() }

        INC -> resolveOperand(decoded, addr)
          .then { (it + 1u).u8() }
          .store(addr) { it }
          .updateZN { it }
        INX -> go().updateX { (X + 1u).u8() }
        INY -> go().updateY { (Y + 1u).u8() }

        ASL -> resolveOperand(decoded, addr)
          .then { alu.asl(q = it) }
          .updateFromShift(decoded, addr)
        LSR -> resolveOperand(decoded, addr)
          .then { alu.lsr(q = it) }
          .updateFromShift(decoded, addr)
        ROL -> resolveOperand(decoded, addr)
          .then { alu.rol(q = it, c = P.C) }
          .updateFromShift(decoded, addr)
        ROR -> resolveOperand(decoded, addr)
          .then { alu.ror(q = it, c = P.C) }
          .updateFromShift(decoded, addr)

        AND -> resolveOperand(decoded, addr).updateA { A and it }
        ORA -> resolveOperand(decoded, addr).updateA { A or it }
        EOR -> resolveOperand(decoded, addr).updateA { A xor it }

        BIT -> resolveOperand(decoded, addr)
          .updateZN { A and it }
          .updateV { !(it and 0x40u).isZero() }

        LDA -> resolveOperand(decoded, addr).updateA { it }
        LDX -> resolveOperand(decoded, addr).updateX { it }
        LDY -> resolveOperand(decoded, addr).updateY { it }

        STA -> go().store(addr) { A }
        STX -> go().store(addr) { X }
        STY -> go().store(addr) { Y }

        PHP -> go().push { P.u8() }
        PHA -> go().push { A }

        PLP -> pop().updateP { it }
        PLA -> pop().updateA { it }
        
        JMP -> go()
          .updatePCL { addr.lo() }
          .updatePCH { addr.hi() }
        JSR -> this
          .extract { PC - 1u }    // Push *last* byte of instruction
          .push { it.hi() }
          .push { it.lo() }
          .updatePCL { addr.lo() }  // TODO - this is cheating
          .updatePCH { addr.hi() }
        RTS -> this
          .popTo { updatePC(it.u16()) }
          .popTo { updatePC((combine(lo = PC.u8(), hi = it) + 1u).u16()) }
          .go() // TODO - convert
        RTI -> this
          .pop().updateP { it }
          .pop().updatePCL { it }
          .pop().updatePCH { it }
        BRK -> TODO()

        BPL -> go().updatePC { if (!P.N) addr else PC }
        BMI -> go().updatePC { if (P.N) addr else PC }
        BVC -> go().updatePC { if (!P.V) addr else PC }
        BVS -> go().updatePC { if (P.V) addr else PC }
        BCC -> go().updatePC { if (!P.C) addr else PC }
        BCS -> go().updatePC { if (P.C) addr else PC }
        BNE -> go().updatePC { if (!P.Z) addr else PC }
        BEQ -> go().updatePC { if (P.Z) addr else PC }

        TXA -> go().updateA { X }
        TYA -> go().updateA { Y }
        TXS -> go().updateS { X }
        TAY -> go().updateY { A }
        TAX -> go().updateX { A }
        TSX -> go().updateX { S }

        CLC -> go().updateC { _0 }
        CLD -> go().updateD { _0 }
        CLI -> go().updateI { _0 }
        CLV -> go().updateV { _0 }

        SEC -> go().updateC { _1 }
        SED -> go().updateD { _1 }
        SEI -> go().updateI { _1 }

        NOP -> go()
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

  private fun <T> StateAnd<T>.push(f: (T) -> UInt8) = store(stackAddr(state.S), f).updateS { (state.S - 1u).u8() }

  private fun State.popTo(consume: State.(data: UInt8) -> State): State {
    val updated = updateS((S + 1u).u8())
    val data = memory.load(stackAddr(updated.S))
    return updated.consume(data)
  }

  private fun <T> StateAnd<T>.pop() = state.pop()

  private fun State.pop() = extract { S }
    .updateS { (it + 1u).u8() }
    .then { memory.load(stackAddr(S)) }

  private fun State.resolveOperand(decoded: Decoded, addr: UInt16) = and {
    when (decoded.addrMode) {
      is Accumulator -> this.A
      is Immediate -> decoded.addrMode.literal
      else -> memory.load(addr)
    }
  }

  private fun State.go() = and { this }
  private fun <T> StateAnd<T>.store(addr: UInt16, f: (T) -> UInt8) = also { memory.store(addr, f(data)) }

  private fun <T> State.and(f: State.() -> T) = StateAnd(this, f(this))
  private fun <T> State.extract(f: State.() -> T) = StateAnd(this, f())
  private fun <T, R> StateAnd<T>.then(f: State.(T) -> R) = StateAnd(state, f(state, data))
  
  private fun <T> StateAnd<T>.updateA(f: (T) -> UInt8) = copy(state = state.with(A = f(data))).updateZN(f)
  private fun <T> StateAnd<T>.updateX(f: (T) -> UInt8) = copy(state = state.with(X = f(data))).updateZN(f)
  private fun <T> StateAnd<T>.updateY(f: (T) -> UInt8) = copy(state = state.with(Y = f(data))).updateZN(f)
  private fun <T> StateAnd<T>.updateS(f: (T) -> UInt8) = copy(state = state.with(S = f(data)))
  private fun <T> StateAnd<T>.updateP(f: (T) -> UInt8) = copy(state = state.copy(P = f(data).toFlags()))
  private fun <T> StateAnd<T>.updatePC(f: (T) -> UInt16) = copy(state = state.with(PC = f(data)))
  private fun <T> StateAnd<T>.updatePCL(f: (T) -> UInt8) = copy(state = state.with(PC = f(data).u16()))  // TODO - proper
  private fun <T> StateAnd<T>.updatePCH(f: (T) -> UInt8) = copy(state = state.with(PC = combine(lo = state.PC.u8(), hi = f(data))))  // TODO - proper
  private fun <T> StateAnd<T>.updateC(f: (T) -> Boolean) = copy(state = state.with(C = f(data)))
  private fun <T> StateAnd<T>.updateD(f: (T) -> Boolean) = copy(state = state.with(D = f(data)))
  private fun <T> StateAnd<T>.updateI(f: (T) -> Boolean) = copy(state = state.with(I = f(data)))
  private fun <T> StateAnd<T>.updateV(f: (T) -> Boolean) = copy(state = state.with(V = f(data)))
  private fun <T> StateAnd<T>.updateZN(f: (T) -> UInt8) = copy(state = state.updateZN(f(data)))

  private fun State.updateS(s: UInt8) = with(S = s)
  private fun State.updatePC(pc: UInt16) = with(PC = pc)

  private fun State.updateZN(q: UInt8) = with(Z = q.isZero(), N = q.isNegative())

  private fun State.advancePC(decoded: Decoded) = with(PC = (PC + decoded.length).u16())

  private fun stackAddr(S: UInt8) = (0x0100u + S).u16()
}
