package choliver.sixfiveohtwo

import choliver.sixfiveohtwo.AddressMode.*
import choliver.sixfiveohtwo.InstructionDecoder.Decoded
import choliver.sixfiveohtwo.Opcode.*
import choliver.sixfiveohtwo.utils._0
import choliver.sixfiveohtwo.utils._1
import java.lang.RuntimeException

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

  private fun <T> State.and(block: State.() -> T) = StateAnd(this, block(this))
  private fun <T, R> StateAnd<T>.gimp(block: State.(T) -> R) = StateAnd(state, block(state, data))
  private fun <T> StateAnd<T>.then(block: State.(T) -> State) = block(state, data)

  // TODO - homogenise State and Memory paradigm
  fun execute(encoding: Array<UInt8>, state: State): State {
    val decoded = decoder.decode(encoding)

    val addr = addrCalc.calculate(decoded.addrMode, state)
    val addrO = (addr - decoded.length).u16()  // TODO - handle this more nicely

    val updated: State = with (state) {
      when (decoded.op) {
        ADC -> resolveOperand(decoded, addr)
          .gimp { alu.adc(a = A, b = it, c = P.C, d = P.D) }
          .then { updateA(it.q).updateC(it.c).updateV(it.v) }

        SBC -> resolveOperand(decoded, addr)
          .gimp { alu.adc(a = A, b = it.inv(), c = P.C, d = P.D) }
          .then { updateA(it.q).updateC(it.c).updateV(it.v) }

        AND -> resolveOperand(decoded, addr)
          .then { updateA(A and it) }

        ORA -> resolveOperand(decoded, addr)
          .then { updateA(A or it) }

        EOR -> resolveOperand(decoded, addr)
          .then { updateA(A xor it) }

        BIT -> resolveOperand(decoded, addr)
          .then { updateZN(A and it).updateV(!(it and 0x40u).isZero()) }

        DEC -> resolveOperand(decoded, addr)
          .then {
            val data = (it - 1u).u8()
            store(addr, data).updateZN(data)
          }
        DEX -> updateX((X - 1u).u8())
        DEY -> updateY((Y - 1u).u8())

        INC -> resolveOperand(decoded, addr)
          .then {
            val data = (it + 1u).u8()
            store(addr, data).updateZN(data)
          }
        INX -> updateX((X + 1u).u8())
        INY -> updateY((Y + 1u).u8())

        ASL -> resolveOperand(decoded, addr)
          .gimp { alu.asl(q = it) }
          .then { updateFromShift(decoded, it, addr) }

        LSR -> resolveOperand(decoded, addr)
          .gimp { alu.lsr(q = it) }
          .then { updateFromShift(decoded, it, addr) }

        ROL -> resolveOperand(decoded, addr)
          .gimp { alu.rol(q = it, c = P.C) }
          .then { updateFromShift(decoded, it, addr) }

        ROR -> resolveOperand(decoded, addr)
          .gimp { alu.ror(q = it, c = P.C) }
          .then { updateFromShift(decoded, it, addr) }

        LDA -> resolveOperand(decoded, addr).then { updateA(it) }
        LDX -> resolveOperand(decoded, addr).then { updateX(it) }
        LDY -> resolveOperand(decoded, addr).then { updateY(it) }

        STA -> store(addr, A)
        STX -> store(addr, X)
        STY -> store(addr, Y)

        PHP -> push(state.P.u8())
        PHA -> push(state.A)

        PLP -> pop().then { updateP(it) }
        PLA -> pop().then { updateA(it) } // Original datasheet claims no flags set, but rest of the world disagrees

        JMP -> updatePC(addrO)
        JSR -> this
          .push((PC + 2u).hi())   // Push *last* byte of instruction
          .push((PC + 2u).lo())
          .updatePC(addrO)
        RTS -> this
          .pop()
          .then { updatePC(it.u16()) }
          .pop()
          .then { updatePC(combine(lo = PC.u8(), hi = it)) }

        BPL -> updatePC(if (!P.N) addrO else PC)
        BMI -> updatePC(if (P.N) addrO else PC)
        BVC -> updatePC(if (!P.V) addrO else PC)
        BVS -> updatePC(if (P.V) addrO else PC)
        BCC -> updatePC(if (!P.C) addrO else PC)
        BCS -> updatePC(if (P.C) addrO else PC)
        BNE -> updatePC(if (!P.Z) addrO else PC)
        BEQ -> updatePC(if (P.Z) addrO else PC)

        TXA -> updateA(X)
        TYA -> updateA(Y)
        TXS -> updateS(X)
        TAY -> updateY(A)
        TAX -> updateX(A)
        TSX -> updateX(S)

        CLC -> updateC(_0)
        CLD -> updateD(_0)
        CLI -> updateI(_0)
        CLV -> updateV(_0)

        SEC -> updateC(_1)
        SED -> updateD(_1)
        SEI -> updateI(_1)

        NOP -> this

        else -> TODO()
      }
    }

    return updated.advancePC(decoded)
  }

  // TODO - this is pretty gross
  private fun State.updateFromShift(decoded: Decoded, it: Alu.Output, addr: UInt16) =
    if (decoded.addrMode is Accumulator) {
      updateA(it.q).updateC(it.c)
    } else {
      store(addr, it.q).updateZN(it.q).updateC(it.c)
    }

  private fun State.push(data: UInt8) = store(stackAddr(S), data)
    .updateS((S - 1u).u8())

  private fun State.pop() = updateS((S + 1u).u8())
    .and { memory.load(stackAddr(S)) }

  private fun State.resolveOperand(decoded: Decoded, addr: UInt16) = and { resolveOperand(decoded, this, addr) }

  private fun State.store(addr: UInt16, data: UInt8): State {
    memory.store(addr, data)
    return this
  }

  private fun State.updateA(a: UInt8) = with(A = a).updateZN(a)
  private fun State.updateX(x: UInt8) = with(X = x).updateZN(x)
  private fun State.updateY(y: UInt8) = with(Y = y).updateZN(y)
  private fun State.updateS(s: UInt8) = with(S = s)
  private fun State.updateP(p: UInt8) = copy(P = p.toFlags())
  private fun State.updatePC(pc: UInt16) = with(PC = pc)
  private fun State.updateC(c: Boolean) = with(C = c)
  private fun State.updateD(d: Boolean) = with(D = d)
  private fun State.updateI(i: Boolean) = with(I = i)
  private fun State.updateV(v: Boolean) = with(V = v)

  private fun State.updateZN(q: UInt8) = with(Z = q.isZero(), N = q.isNegative())

  private fun State.advancePC(decoded: Decoded) = with(PC = (PC + decoded.length).u16())

  private fun stackAddr(S: UInt8) = (0x0100u + S).u16()

  private fun resolveOperand(decoded: Decoded, state: State, addr: UInt16): UInt8 = when (decoded.addrMode) {
    is Accumulator -> state.A
    is Immediate -> decoded.addrMode.literal
    is Absolute,
    is ZeroPage,
    is Indirect,
    is AbsoluteIndexed,
    is ZeroPageIndexed,
    is IndexedIndirect,
    is IndirectIndexed
    -> memory.load(addr)
    else -> throw RuntimeException("Unexpected address mode ${decoded.addrMode}")
  }
}
