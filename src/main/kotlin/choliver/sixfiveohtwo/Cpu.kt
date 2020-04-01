package choliver.sixfiveohtwo

import choliver.sixfiveohtwo.AddressMode.*

class Cpu(
  private val memory: Memory
) {
  private val alu = Alu()
  private val addrCalc = AddressCalculator(memory)

  private data class Decoded(
    val yeah: Yeah,
    val addrMode: AddressMode
  )

  fun execute(encoding: Array<UInt8>, state: State): State {
    val decoded = decode(encoding)

    val addr = addrCalc.calculate(decoded.addrMode, state)

    val out = resolveOperand(decoded.addrMode, state, addr)

    when (decoded.yeah.memSrc) {
      MemSrc.A -> memory.store(addr, state.A)
      MemSrc.X -> memory.store(addr, state.X)
      MemSrc.Y -> memory.store(addr, state.Y)
      MemSrc.S -> memory.store(addr, state.S)
      MemSrc.P -> memory.store(addr, state.P.u8())
      MemSrc.R -> TODO()
      MemSrc.N -> {} // Do nothing
    }

    return state
      .withNewReg(decoded.yeah.regSink, out)
  }

  private fun resolveOperand(mode: AddressMode, state: State, addr: UInt16): UInt8 = when (mode) {
    is Accumulator -> state.A
    is Immediate -> mode.literal
    is Absolute,
    is ZeroPage,
    is Indirect,
    is AbsoluteIndexed,
    is ZeroPageIndexed,
    is IndexedIndirect,
    is IndirectIndexed
    -> memory.load(addr)
    is Implied,
    is Relative
    -> 0.u8()  // Don't care
  }

  private fun decode(encoding: Array<UInt8>): Decoded {
    // TODO - error handling
    val yeah = ENCODINGS[encoding[0]]!!

    fun operand8() = encoding[1]
    fun operand16() = combine(encoding[1], encoding[2])

    val mode = when (yeah.addrMode) {
      AddrMode.IMMEDIATE -> Immediate(operand8())
      AddrMode.IMPLIED -> Implied
      AddrMode.ABSOLUTE -> Absolute(operand16())
      AddrMode.ABSOLUTE_X -> AbsoluteIndexed(operand16(), IndexSource.X)
      AddrMode.ABSOLUTE_Y -> AbsoluteIndexed(operand16(), IndexSource.Y)
      AddrMode.ZERO_PAGE -> ZeroPage(operand8())
      AddrMode.ZERO_PAGE_X -> ZeroPageIndexed(operand8(), IndexSource.X)
      AddrMode.ZERO_PAGE_Y -> ZeroPageIndexed(operand8(), IndexSource.Y)
      AddrMode.INDEXED_INDIRECT -> IndexedIndirect(operand8())
      AddrMode.INDIRECT_INDEXED -> IndirectIndexed(operand8())
    }

    return Decoded(yeah, mode)
  }

  // TODO - homogenise State and Memory paradigm
  fun execute(inst: Instruction, state: State): State {
    val reg = selectInputReg(inst.op.RegIn, state)

    val addr = addrCalc.calculate(inst.addressMode, state)

    val alu = inst.op.aluMode(alu, Alu.Input(
      a = selectAluSrc(inst.op.aluA, reg = reg, mem = addr),
      b = selectAluSrc(inst.op.aluB, reg = reg, mem = addr),
      c = state.P.C,
      d = state.P.D
    ))

    val out = selectOut(inst.op.out, reg = reg, mem = addr, alu = alu)

    if (inst.op.memOut) {
      memory.store(addr, out)
    }

    // TODO - update PC
    // - regular increment
    // - jump
    // - return
    // - branch

    return state
      .withNewP(inst.op.flag, out, alu)
      .withNewS(inst.op.stack)
      .withNewReg(inst.op.regOut, out)
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

  private fun selectAluSrc(src: AluSrc, reg: UInt8, mem: UInt16): UInt8 = when (src) {
    AluSrc.REG -> reg
    AluSrc.MEM -> mem.u8()
    AluSrc.NON -> 0.u8()
    AluSrc.ZZZ -> TODO()
  }

  private fun selectOut(out: OutSrc, reg: UInt8, mem: UInt16, alu: Alu.Output) = when (out) {
    OutSrc.MEM -> mem.u8()
    OutSrc.REG -> reg
    OutSrc.ALU -> alu.z
    OutSrc.NON -> 0.u8()
    OutSrc.ZZZ -> TODO()
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

  private fun State.withNewS(stack: Stack) = copy(S = with(S) {
    when (stack) {
      Stack.PUSH -> (this - 1u).u8()
      Stack.POP_ -> (this + 1u).u8()
      Stack.NONE -> this
    }
  })

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
