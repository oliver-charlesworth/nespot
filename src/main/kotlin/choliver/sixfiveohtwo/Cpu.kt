package choliver.sixfiveohtwo

class Cpu(
  memory: Memory
) {
  private val alu = Alu()
  private val operandCalculator = OperandCalculator(memory)

  // TODO - homogenise State and Memory paradigm
  fun process(inst: Instruction, state: State): State {
    val regIn: UInt8 = when (inst.op.RegIn) {
      Reg.A -> state.A
      Reg.X -> state.X
      Reg.Y -> state.Y
      Reg.S -> state.S
      Reg.P -> state.P.toUInt8()
      Reg.N -> 0.toUInt8()
      Reg.Z -> TODO()
    }

    val memIn: UInt16 = operandCalculator.calculate(inst.addressMode, state)

    fun selectAluSrc(src: AluSrc): UInt8 = when (src) {
      AluSrc.REG -> regIn
      AluSrc.MEM -> memIn.toUInt8()
      AluSrc.NON -> 0.toUInt8()
      AluSrc.ZZZ -> TODO()
    }

    val aluOut = inst.op.aluMode(alu, Alu.Input(
      a = selectAluSrc(inst.op.aluA),
      b = selectAluSrc(inst.op.aluB),
      c = state.P.C,
      d = state.P.D
    ))

    val out: UInt8 = when (inst.op.out) {
      OutSrc.MEM -> memIn.toUInt8()
      OutSrc.REG -> regIn
      OutSrc.ALU -> aluOut.z
      OutSrc.NON -> 0.toUInt8()
      OutSrc.ZZZ -> TODO()
    }

    val stateOut = when (inst.op.regOut) {
      Reg.A -> state.copy(A = aluOut.z)
      Reg.X -> state.copy(X = aluOut.z)
      Reg.Y -> state.copy(Y = aluOut.z)
      Reg.S -> state.copy(S = aluOut.z)
      Reg.P -> TODO()
      Reg.N -> state
      Reg.Z -> TODO()
    }

    if (inst.op.memOut) {
      // TODO
    }

    // TODO - update flags
    // TODO - update PC

    return stateOut
  }
}
