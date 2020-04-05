package choliver.sixfiveohtwo

import choliver.sixfiveohtwo.AddressMode.*
import choliver.sixfiveohtwo.Opcode.*
import choliver.sixfiveohtwo.utils._0
import choliver.sixfiveohtwo.utils._1

class Cpu(
  private val memory: Memory
) {
  private val alu = Alu()
  private val addrCalc = AddressCalculator(memory)

  private data class Decoded(
    val yeah: Yeah,
    val addrMode: AddressMode,
    val pcInc: UInt8
  )

  data class StateAnd<T>(
    val state: State,
    val data: T
  )

  private fun <T> State.and(block: State.() -> T) = StateAnd(this, block(this))
  private fun <T, R> StateAnd<T>.gimp(block: State.(T) -> R) = StateAnd(state, block(state, data))
  private fun <T> StateAnd<T>.then(block: State.(T) -> State) = block(state, data)

  // TODO - homogenise State and Memory paradigm
  fun execute(encoding: Array<UInt8>, state: State): State {
    val decoded = decode(encoding)

    val addr = addrCalc.calculate(decoded.addrMode, state)

    val updated: State = with (state) {
      when (decoded.yeah.op) {
        ADC -> resolveOperand(decoded, addr)
          .gimp { alu.adc(a = A, b = it, c = P.C, d = P.D) }
          .then { updateA(it.q).updateC(it.c).updateV(it.v) }

        SBC -> resolveOperand(decoded, addr)
          .gimp { alu.sbc(a = A, b = it, c = P.C, d = P.D) }
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

        JMP -> updatePC((addr - decoded.pcInc).u16())
        JSR -> this
          .push((PC + 2u).hi())   // Push *last* byte of instruction
          .push((PC + 2u).lo())
          .updatePC((addr - decoded.pcInc).u16())
        RTS -> this
          .pop()
          .then { updatePC(it.u16()) }
          .pop()
          .then { updatePC(combine(lo = PC.u8(), hi = it)) }

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

  private fun State.advancePC(decoded: Decoded) = with(PC = (PC + decoded.pcInc).u16())

  private fun stackAddr(S: UInt8) = (0x0100u + S).u16()

  private fun resolveOperand(decoded: Decoded, state: State, addr: UInt16): UInt8 = when (decoded.addrMode) {
    is Accumulator -> state.A
    is Immediate -> decoded.addrMode.literal
    is Implied -> selectInputReg(decoded.yeah.regSrc, state)
    is Relative -> 0.u8()  // Don't care
    is Absolute,
    is ZeroPage,
    is Indirect,
    is AbsoluteIndexed,
    is ZeroPageIndexed,
    is IndexedIndirect,
    is IndirectIndexed
    -> memory.load(addr)
    is Stack -> memory.load((addr + 1u).u16())  // TODO - this is cheating, and doesn't wrap correctly
  }

  private fun decode(encoding: Array<UInt8>): Decoded {
    var pcInc = 1

    // TODO - error handling
    val yeah = ENCODINGS[encoding[0]]!!

    fun operand8(): UInt8 {
      pcInc = 2
      return encoding[1]
    }
    fun operand16(): UInt16 {
      pcInc = 3
      return combine(encoding[1], encoding[2])
    }

    val mode = when (yeah.addrMode) {
      AddrMode.ACCUMULATOR -> Accumulator
      AddrMode.IMMEDIATE -> Immediate(operand8())
      AddrMode.IMPLIED -> Implied
      AddrMode.STACK -> Stack
      AddrMode.INDIRECT -> Indirect(operand16())
      AddrMode.ABSOLUTE -> Absolute(operand16())
      AddrMode.ABSOLUTE_X -> AbsoluteIndexed(operand16(), IndexSource.X)
      AddrMode.ABSOLUTE_Y -> AbsoluteIndexed(operand16(), IndexSource.Y)
      AddrMode.ZERO_PAGE -> ZeroPage(operand8())
      AddrMode.ZERO_PAGE_X -> ZeroPageIndexed(operand8(), IndexSource.X)
      AddrMode.ZERO_PAGE_Y -> ZeroPageIndexed(operand8(), IndexSource.Y)
      AddrMode.INDEXED_INDIRECT -> IndexedIndirect(operand8())
      AddrMode.INDIRECT_INDEXED -> IndirectIndexed(operand8())
    }

    return Decoded(
      yeah,
      mode,
      pcInc.u8()
    )
  }

  private fun selectInputReg(reg: Reg, state: State) = when (reg) {
    Reg.A -> state.A
    Reg.X -> state.X
    Reg.Y -> state.Y
    Reg.S -> state.S
    Reg.P -> state.P.u8()
    Reg.N -> 0.u8()
    Reg.Z -> TODO()
  }
}
