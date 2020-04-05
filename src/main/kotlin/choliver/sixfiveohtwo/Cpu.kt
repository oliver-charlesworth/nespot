package choliver.sixfiveohtwo

import choliver.sixfiveohtwo.AddressMode.*
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

  // TODO - homogenise State and Memory paradigm
  fun execute(encoding: Array<UInt8>, state: State): State {
    val decoded = decode(encoding)

    val addr = addrCalc.calculate(decoded.addrMode, state)

    val operand = resolveOperand(decoded, state, addr)

    val alu = alu.execute(decoded.yeah.op.aluMode, Alu.Input(
      a = state.A,
      b = operand,
      c = state.P.C,
      d = state.P.D
    ))

    when (decoded.yeah.memSrc) {
      MemSrc.A -> memory.store(addr, state.A)
      MemSrc.X -> memory.store(addr, state.X)
      MemSrc.Y -> memory.store(addr, state.Y)
      MemSrc.S -> memory.store(addr, state.S)
      MemSrc.P -> memory.store(addr, state.P.u8())
      MemSrc.R -> TODO()
      MemSrc.N -> {} // Do nothing
      MemSrc.Z -> TODO()
    }

    return state
      .withNewPC(decoded, addr)
      .withNewP(decoded.yeah.op.flag, alu.q, alu) // TODO - simplify args
      .withNewS(decoded.yeah.op)
      .withNewReg(decoded.yeah.regSink, alu.q)
  }

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

  private fun State.withNewPC(decoded: Decoded, addr: UInt16) = when (decoded.yeah.pcSrc) {
    PcSrc.A -> copy(PC = addr)
    PcSrc.N -> copy(PC = (PC + decoded.pcInc).u16())
    PcSrc.Z -> TODO()
  }

  private fun State.withNewP(flag: Flag, out: UInt8, alu: Alu.Output) = copy(P = with(P) {
    val c = alu.c
    val z = out.isZero()
    val v = alu.v
    val n = out.isNegative()

    when (flag) {
      Flag.NON_ -> this
      Flag._Z_N -> copy(Z = z, N = n)
      Flag.CZ_N -> copy(C = c, Z = z, N = n)
      Flag._ZVN -> copy(Z = z, V = v, N = n)
      Flag.CZVN -> copy(C = c, Z = z, V = v, N = n)
      Flag.C0__ -> copy(C = _0)
      Flag.C1__ -> copy(C = _1)
      Flag.I0__ -> copy(I = _0)
      Flag.I1__ -> copy(I = _1)
      Flag.D0__ -> copy(D = _0)
      Flag.D1__ -> copy(D = _1)
      Flag.V0__ -> copy(V = _0)
    }
  })

  // TODO - make this switch on a control line, not opcode
  private fun State.withNewS(op: Opcode) = when (op) {
    Opcode.PLA, Opcode.PLP -> copy(S = (S + 1u).u8())
    Opcode.PHA, Opcode.PHP -> copy(S = (S - 1u).u8())
    else -> this
  }

  private fun State.withNewReg(reg: Reg, out: UInt8) = when (reg) {
    Reg.A -> copy(A = out)
    Reg.X -> copy(X = out)
    Reg.Y -> copy(Y = out)
    Reg.S -> copy(S = out)
    Reg.P -> copy(P = out.toFlags())
    Reg.N -> this
    Reg.Z -> TODO()
  }
}
