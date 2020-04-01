package choliver.sixfiveohtwo

class Cpu(
  memory: Memory
) {
  private val alu = Alu()
  private val addrCalc = AddressCalculator(memory)

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
      // TODO
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
    Reg.P -> state.P.toUInt8()
    Reg.N -> 0.toUInt8()
    Reg.Z -> TODO()
  }

  private fun selectAluSrc(src: AluSrc, reg: UInt8, mem: UInt16): UInt8 = when (src) {
    AluSrc.REG -> reg
    AluSrc.MEM -> mem.toUInt8()
    AluSrc.NON -> 0.toUInt8()
    AluSrc.ZZZ -> TODO()
  }

  private fun selectOut(out: OutSrc, reg: UInt8, mem: UInt16, alu: Alu.Output) = when (out) {
    OutSrc.MEM -> mem.toUInt8()
    OutSrc.REG -> reg
    OutSrc.ALU -> alu.z
    OutSrc.NON -> 0.toUInt8()
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
      Stack.PUSH -> (this - 1u).toUInt8()
      Stack.POP_ -> (this + 1u).toUInt8()
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
