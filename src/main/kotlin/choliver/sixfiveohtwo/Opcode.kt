package choliver.sixfiveohtwo

enum class Reg {
  A,
  X,
  Y,
  S,
  P,
  N, // None
  Z  // TODO
}

enum class AluSrc {
  REG, // Register file
  MEM, // Memory
  NON, // Don't care
  ZZZ // TODO
}

enum class OutSrc {
  MEM, // Memory
  REG, // Register file
  ALU, // ALU output
  NON, // Don't care
  ZZZ  // TODO
}

enum class Opcode(
  val RegIn: Reg,
  val aluA: AluSrc,
  val aluB: AluSrc,
  val out: OutSrc,
  val regOut: Reg,
  val memOut: Boolean,
  val aluMode: Alu.(Alu.Input) -> Alu.Output
) {
  ADC(Reg.A, AluSrc.REG, AluSrc.MEM, OutSrc.ALU, Reg.A, _0, Alu::adc),
  AND(Reg.A, AluSrc.REG, AluSrc.MEM, OutSrc.ALU, Reg.A, _0, Alu::and),
  ASL(Reg.Z, AluSrc.ZZZ, AluSrc.ZZZ, OutSrc.ZZZ, Reg.Z, _0, Alu::asl),
  BCC(Reg.Z, AluSrc.ZZZ, AluSrc.ZZZ, OutSrc.ZZZ, Reg.Z, _0, Alu::nop),
  BCS(Reg.Z, AluSrc.ZZZ, AluSrc.ZZZ, OutSrc.ZZZ, Reg.Z, _0, Alu::nop),
  BEQ(Reg.Z, AluSrc.ZZZ, AluSrc.ZZZ, OutSrc.ZZZ, Reg.Z, _0, Alu::nop),
  BIT(Reg.Z, AluSrc.ZZZ, AluSrc.ZZZ, OutSrc.ZZZ, Reg.Z, _0, Alu::nop),
  BMI(Reg.Z, AluSrc.ZZZ, AluSrc.ZZZ, OutSrc.ZZZ, Reg.Z, _0, Alu::nop),
  BNE(Reg.Z, AluSrc.ZZZ, AluSrc.ZZZ, OutSrc.ZZZ, Reg.Z, _0, Alu::nop),
  BPL(Reg.Z, AluSrc.ZZZ, AluSrc.ZZZ, OutSrc.ZZZ, Reg.Z, _0, Alu::nop),
  BRK(Reg.Z, AluSrc.ZZZ, AluSrc.ZZZ, OutSrc.ZZZ, Reg.Z, _0, Alu::nop),
  BVC(Reg.Z, AluSrc.ZZZ, AluSrc.ZZZ, OutSrc.ZZZ, Reg.Z, _0, Alu::nop),
  BVS(Reg.Z, AluSrc.ZZZ, AluSrc.ZZZ, OutSrc.ZZZ, Reg.Z, _0, Alu::nop),
  CLC(Reg.Z, AluSrc.ZZZ, AluSrc.ZZZ, OutSrc.ZZZ, Reg.Z, _0, Alu::nop),
  CLD(Reg.Z, AluSrc.ZZZ, AluSrc.ZZZ, OutSrc.ZZZ, Reg.Z, _0, Alu::nop),
  CLI(Reg.Z, AluSrc.ZZZ, AluSrc.ZZZ, OutSrc.ZZZ, Reg.Z, _0, Alu::nop),
  CLV(Reg.Z, AluSrc.ZZZ, AluSrc.ZZZ, OutSrc.ZZZ, Reg.Z, _0, Alu::nop),
  CMP(Reg.A, AluSrc.REG, AluSrc.MEM, OutSrc.NON, Reg.N, _0, Alu::sbc), // TODO - doesn't set V
  CPX(Reg.A, AluSrc.REG, AluSrc.MEM, OutSrc.NON, Reg.N, _0, Alu::sbc), // TODO - doesn't set V
  CPY(Reg.A, AluSrc.REG, AluSrc.MEM, OutSrc.NON, Reg.N, _0, Alu::sbc), // TODO - doesn't set V
  DEC(Reg.N, AluSrc.NON, AluSrc.MEM, OutSrc.ALU, Reg.N, _1, Alu::dec),
  DEX(Reg.X, AluSrc.NON, AluSrc.REG, OutSrc.ALU, Reg.X, _0, Alu::dec),
  DEY(Reg.Y, AluSrc.NON, AluSrc.REG, OutSrc.ALU, Reg.Y, _0, Alu::dec),
  EOR(Reg.A, AluSrc.REG, AluSrc.MEM, OutSrc.ALU, Reg.A, _0, Alu::eor),
  INC(Reg.N, AluSrc.NON, AluSrc.MEM, OutSrc.ALU, Reg.N, _1, Alu::inc),
  INX(Reg.X, AluSrc.NON, AluSrc.REG, OutSrc.ALU, Reg.X, _0, Alu::inc),
  INY(Reg.Y, AluSrc.NON, AluSrc.REG, OutSrc.ALU, Reg.Y, _0, Alu::inc),
  JMP(Reg.Z, AluSrc.ZZZ, AluSrc.ZZZ, OutSrc.ZZZ, Reg.Z, _0, Alu::nop),
  JSR(Reg.Z, AluSrc.ZZZ, AluSrc.ZZZ, OutSrc.ZZZ, Reg.Z, _0, Alu::nop),
  LDA(Reg.N, AluSrc.NON, AluSrc.NON, OutSrc.MEM, Reg.A, _0, Alu::nop),
  LDX(Reg.N, AluSrc.NON, AluSrc.NON, OutSrc.MEM, Reg.X, _0, Alu::nop),
  LDY(Reg.N, AluSrc.NON, AluSrc.NON, OutSrc.MEM, Reg.Y, _0, Alu::nop),
  LSR(Reg.Z, AluSrc.ZZZ, AluSrc.ZZZ, OutSrc.ZZZ, Reg.Z, _0, Alu::lsr),
  NOP(Reg.N, AluSrc.NON, AluSrc.NON, OutSrc.NON, Reg.N, _0, Alu::nop),
  ORA(Reg.A, AluSrc.REG, AluSrc.MEM, OutSrc.ALU, Reg.A, _0, Alu::ora),
  PHA(Reg.Z, AluSrc.ZZZ, AluSrc.ZZZ, OutSrc.ZZZ, Reg.Z, _0, Alu::nop),
  PHP(Reg.Z, AluSrc.ZZZ, AluSrc.ZZZ, OutSrc.ZZZ, Reg.Z, _0, Alu::nop),
  PLA(Reg.Z, AluSrc.ZZZ, AluSrc.ZZZ, OutSrc.ZZZ, Reg.Z, _0, Alu::nop),
  PLP(Reg.Z, AluSrc.ZZZ, AluSrc.ZZZ, OutSrc.ZZZ, Reg.Z, _0, Alu::nop),
  ROL(Reg.Z, AluSrc.ZZZ, AluSrc.ZZZ, OutSrc.ZZZ, Reg.Z, _0, Alu::rol),
  ROR(Reg.Z, AluSrc.ZZZ, AluSrc.ZZZ, OutSrc.ZZZ, Reg.Z, _0, Alu::ror),
  RTI(Reg.Z, AluSrc.ZZZ, AluSrc.ZZZ, OutSrc.ZZZ, Reg.Z, _0, Alu::nop),
  RTS(Reg.Z, AluSrc.ZZZ, AluSrc.ZZZ, OutSrc.ZZZ, Reg.Z, _0, Alu::nop),
  SBC(Reg.A, AluSrc.REG, AluSrc.MEM, OutSrc.ALU, Reg.A, _0, Alu::nop),
  SEC(Reg.Z, AluSrc.ZZZ, AluSrc.ZZZ, OutSrc.ZZZ, Reg.Z, _0, Alu::nop),
  SED(Reg.Z, AluSrc.ZZZ, AluSrc.ZZZ, OutSrc.ZZZ, Reg.Z, _0, Alu::nop),
  SEI(Reg.Z, AluSrc.ZZZ, AluSrc.ZZZ, OutSrc.ZZZ, Reg.Z, _0, Alu::nop),
  STA(Reg.A, AluSrc.NON, AluSrc.NON, OutSrc.REG, Reg.N, _1, Alu::nop),
  STX(Reg.X, AluSrc.NON, AluSrc.NON, OutSrc.REG, Reg.N, _1, Alu::nop),
  STY(Reg.Y, AluSrc.NON, AluSrc.NON, OutSrc.REG, Reg.N, _1, Alu::nop),
  TAX(Reg.A, AluSrc.NON, AluSrc.NON, OutSrc.REG, Reg.X, _0, Alu::nop),
  TAY(Reg.A, AluSrc.NON, AluSrc.NON, OutSrc.REG, Reg.Y, _0, Alu::nop),
  TSX(Reg.S, AluSrc.NON, AluSrc.NON, OutSrc.REG, Reg.X, _0, Alu::nop),
  TXA(Reg.X, AluSrc.NON, AluSrc.NON, OutSrc.REG, Reg.A, _0, Alu::nop),
  TXS(Reg.X, AluSrc.NON, AluSrc.NON, OutSrc.REG, Reg.S, _0, Alu::nop),
  TYA(Reg.Y, AluSrc.NON, AluSrc.NON, OutSrc.REG, Reg.A, _0, Alu::nop)
}

