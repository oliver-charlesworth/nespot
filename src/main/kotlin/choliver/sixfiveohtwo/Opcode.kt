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

enum class Flag {
  NON_,
  _Z_N,
  CZ_N,
  _ZVN,
  CZVN,
  C0__,
  C1__,
  I0__,
  I1__,
  D0__,
  D1__,
  V0__
}

enum class Opcode(
  val RegIn: Reg,
  val aluA: AluSrc,
  val aluB: AluSrc,
  val out: OutSrc,
  val regOut: Reg,
  val memOut: Boolean,
  val flag: Flag,
  val aluMode: Alu.(Alu.Input) -> Alu.Output
) {
  ADC(Reg.A, AluSrc.REG, AluSrc.MEM, OutSrc.ALU, Reg.A, _0, Flag.CZVN, Alu::adc),
  AND(Reg.A, AluSrc.REG, AluSrc.MEM, OutSrc.ALU, Reg.A, _0, Flag._Z_N, Alu::and),
  ASL(Reg.Z, AluSrc.ZZZ, AluSrc.ZZZ, OutSrc.ZZZ, Reg.Z, _0, Flag.CZ_N, Alu::asl),
  BCC(Reg.Z, AluSrc.ZZZ, AluSrc.ZZZ, OutSrc.ZZZ, Reg.Z, _0, Flag.NON_, Alu::nop),
  BCS(Reg.Z, AluSrc.ZZZ, AluSrc.ZZZ, OutSrc.ZZZ, Reg.Z, _0, Flag.NON_, Alu::nop),
  BEQ(Reg.Z, AluSrc.ZZZ, AluSrc.ZZZ, OutSrc.ZZZ, Reg.Z, _0, Flag.NON_, Alu::nop),
  BIT(Reg.Z, AluSrc.ZZZ, AluSrc.ZZZ, OutSrc.ZZZ, Reg.Z, _0, Flag._ZVN, Alu::nop),
  BMI(Reg.Z, AluSrc.ZZZ, AluSrc.ZZZ, OutSrc.ZZZ, Reg.Z, _0, Flag.NON_, Alu::nop),
  BNE(Reg.Z, AluSrc.ZZZ, AluSrc.ZZZ, OutSrc.ZZZ, Reg.Z, _0, Flag.NON_, Alu::nop),
  BPL(Reg.Z, AluSrc.ZZZ, AluSrc.ZZZ, OutSrc.ZZZ, Reg.Z, _0, Flag.NON_, Alu::nop),
  BRK(Reg.Z, AluSrc.ZZZ, AluSrc.ZZZ, OutSrc.ZZZ, Reg.Z, _0, Flag.NON_, Alu::nop),
  BVC(Reg.Z, AluSrc.ZZZ, AluSrc.ZZZ, OutSrc.ZZZ, Reg.Z, _0, Flag.NON_, Alu::nop),
  BVS(Reg.Z, AluSrc.ZZZ, AluSrc.ZZZ, OutSrc.ZZZ, Reg.Z, _0, Flag.NON_, Alu::nop),
  CLC(Reg.Z, AluSrc.ZZZ, AluSrc.ZZZ, OutSrc.ZZZ, Reg.Z, _0, Flag.C0__, Alu::nop),
  CLD(Reg.Z, AluSrc.ZZZ, AluSrc.ZZZ, OutSrc.ZZZ, Reg.Z, _0, Flag.D0__, Alu::nop),
  CLI(Reg.Z, AluSrc.ZZZ, AluSrc.ZZZ, OutSrc.ZZZ, Reg.Z, _0, Flag.I0__, Alu::nop),
  CLV(Reg.Z, AluSrc.ZZZ, AluSrc.ZZZ, OutSrc.ZZZ, Reg.Z, _0, Flag.V0__, Alu::nop),
  CMP(Reg.A, AluSrc.REG, AluSrc.MEM, OutSrc.NON, Reg.N, _0, Flag.CZ_N, Alu::sbc),
  CPX(Reg.A, AluSrc.REG, AluSrc.MEM, OutSrc.NON, Reg.N, _0, Flag.CZ_N, Alu::sbc),
  CPY(Reg.A, AluSrc.REG, AluSrc.MEM, OutSrc.NON, Reg.N, _0, Flag.CZ_N, Alu::sbc),
  DEC(Reg.N, AluSrc.NON, AluSrc.MEM, OutSrc.ALU, Reg.N, _1, Flag._Z_N, Alu::dec),
  DEX(Reg.X, AluSrc.NON, AluSrc.REG, OutSrc.ALU, Reg.X, _0, Flag._Z_N, Alu::dec),
  DEY(Reg.Y, AluSrc.NON, AluSrc.REG, OutSrc.ALU, Reg.Y, _0, Flag._Z_N, Alu::dec),
  EOR(Reg.A, AluSrc.REG, AluSrc.MEM, OutSrc.ALU, Reg.A, _0, Flag._Z_N, Alu::eor),
  INC(Reg.N, AluSrc.NON, AluSrc.MEM, OutSrc.ALU, Reg.N, _1, Flag._Z_N, Alu::inc),
  INX(Reg.X, AluSrc.NON, AluSrc.REG, OutSrc.ALU, Reg.X, _0, Flag._Z_N, Alu::inc),
  INY(Reg.Y, AluSrc.NON, AluSrc.REG, OutSrc.ALU, Reg.Y, _0, Flag._Z_N, Alu::inc),
  JMP(Reg.Z, AluSrc.ZZZ, AluSrc.ZZZ, OutSrc.ZZZ, Reg.Z, _0, Flag.NON_, Alu::nop),
  JSR(Reg.Z, AluSrc.ZZZ, AluSrc.ZZZ, OutSrc.ZZZ, Reg.Z, _0, Flag.NON_, Alu::nop),
  LDA(Reg.N, AluSrc.NON, AluSrc.NON, OutSrc.MEM, Reg.A, _0, Flag._Z_N, Alu::nop),
  LDX(Reg.N, AluSrc.NON, AluSrc.NON, OutSrc.MEM, Reg.X, _0, Flag._Z_N, Alu::nop),
  LDY(Reg.N, AluSrc.NON, AluSrc.NON, OutSrc.MEM, Reg.Y, _0, Flag._Z_N, Alu::nop),
  LSR(Reg.Z, AluSrc.ZZZ, AluSrc.ZZZ, OutSrc.ZZZ, Reg.Z, _0, Flag.CZ_N, Alu::lsr),
  NOP(Reg.N, AluSrc.NON, AluSrc.NON, OutSrc.NON, Reg.N, _0, Flag.NON_, Alu::nop),
  ORA(Reg.A, AluSrc.REG, AluSrc.MEM, OutSrc.ALU, Reg.A, _0, Flag._Z_N, Alu::ora),
  PHA(Reg.Z, AluSrc.ZZZ, AluSrc.ZZZ, OutSrc.ZZZ, Reg.Z, _0, Flag.NON_, Alu::nop),
  PHP(Reg.Z, AluSrc.ZZZ, AluSrc.ZZZ, OutSrc.ZZZ, Reg.Z, _0, Flag.NON_, Alu::nop),
  PLA(Reg.Z, AluSrc.ZZZ, AluSrc.ZZZ, OutSrc.ZZZ, Reg.Z, _0, Flag._Z_N, Alu::nop),
  PLP(Reg.Z, AluSrc.ZZZ, AluSrc.ZZZ, OutSrc.ZZZ, Reg.Z, _0, Flag.NON_, Alu::nop),
  ROL(Reg.Z, AluSrc.ZZZ, AluSrc.ZZZ, OutSrc.ZZZ, Reg.Z, _0, Flag.CZ_N, Alu::rol),
  ROR(Reg.Z, AluSrc.ZZZ, AluSrc.ZZZ, OutSrc.ZZZ, Reg.Z, _0, Flag.CZ_N, Alu::ror),
  RTI(Reg.Z, AluSrc.ZZZ, AluSrc.ZZZ, OutSrc.ZZZ, Reg.Z, _0, Flag.NON_, Alu::nop),
  RTS(Reg.Z, AluSrc.ZZZ, AluSrc.ZZZ, OutSrc.ZZZ, Reg.Z, _0, Flag.NON_, Alu::nop),
  SBC(Reg.A, AluSrc.REG, AluSrc.MEM, OutSrc.ALU, Reg.A, _0, Flag.CZVN, Alu::sbc),
  SEC(Reg.Z, AluSrc.ZZZ, AluSrc.ZZZ, OutSrc.ZZZ, Reg.Z, _0, Flag.C1__, Alu::nop),
  SED(Reg.Z, AluSrc.ZZZ, AluSrc.ZZZ, OutSrc.ZZZ, Reg.Z, _0, Flag.D1__, Alu::nop),
  SEI(Reg.Z, AluSrc.ZZZ, AluSrc.ZZZ, OutSrc.ZZZ, Reg.Z, _0, Flag.I1__, Alu::nop),
  STA(Reg.A, AluSrc.NON, AluSrc.NON, OutSrc.REG, Reg.N, _1, Flag.NON_, Alu::nop),
  STX(Reg.X, AluSrc.NON, AluSrc.NON, OutSrc.REG, Reg.N, _1, Flag.NON_, Alu::nop),
  STY(Reg.Y, AluSrc.NON, AluSrc.NON, OutSrc.REG, Reg.N, _1, Flag.NON_, Alu::nop),
  TAX(Reg.A, AluSrc.NON, AluSrc.NON, OutSrc.REG, Reg.X, _0, Flag._Z_N, Alu::nop),
  TAY(Reg.A, AluSrc.NON, AluSrc.NON, OutSrc.REG, Reg.Y, _0, Flag._Z_N, Alu::nop),
  TSX(Reg.S, AluSrc.NON, AluSrc.NON, OutSrc.REG, Reg.X, _0, Flag._Z_N, Alu::nop),
  TXA(Reg.X, AluSrc.NON, AluSrc.NON, OutSrc.REG, Reg.A, _0, Flag._Z_N, Alu::nop),
  TXS(Reg.X, AluSrc.NON, AluSrc.NON, OutSrc.REG, Reg.S, _0, Flag.NON_, Alu::nop),
  TYA(Reg.Y, AluSrc.NON, AluSrc.NON, OutSrc.REG, Reg.A, _0, Flag._Z_N, Alu::nop)
}

