package choliver.sixfiveohtwo

import choliver.sixfiveohtwo.AddrMode.*
import choliver.sixfiveohtwo.Opcode.*

enum class Reg {
  A,
  X,
  Y,
  S,
  P,
  N, // None
  Z  // TODO
}

enum class AluMode {
  NOP,
  ADC,
  SBC,
  DEC,
  INC,
  AND,
  EOR,
  ORA,
  ASL,
  LSR,
  ROL,
  ROR,
  BIT
}

enum class Flag {
  NON_,
  _Z_N,
  CZ_N,
  _ZVN,
  CZVN,
  // TODO - could these be implemented via ALU with a bitmask ?
  C0__,
  C1__,
  I0__,
  I1__,
  D0__,
  D1__,
  V0__
}

enum class Opcode(
  val flag: Flag,
  val aluMode: AluMode
) {
  ADC(Flag.CZVN, AluMode.ADC),
  AND(Flag._Z_N, AluMode.AND),
  ASL(Flag.CZ_N, AluMode.ASL),
  BCC(Flag.NON_, AluMode.NOP),
  BCS(Flag.NON_, AluMode.NOP),
  BEQ(Flag.NON_, AluMode.NOP),
  BIT(Flag._ZVN, AluMode.BIT),
  BMI(Flag.NON_, AluMode.NOP),
  BNE(Flag.NON_, AluMode.NOP),
  BPL(Flag.NON_, AluMode.NOP),
  BRK(Flag.NON_, AluMode.NOP),
  BVC(Flag.NON_, AluMode.NOP),
  BVS(Flag.NON_, AluMode.NOP),
  CLC(Flag.C0__, AluMode.NOP),
  CLD(Flag.D0__, AluMode.NOP),
  CLI(Flag.I0__, AluMode.NOP),
  CLV(Flag.V0__, AluMode.NOP),
  CMP(Flag.CZ_N, AluMode.SBC),
  CPX(Flag.CZ_N, AluMode.SBC),
  CPY(Flag.CZ_N, AluMode.SBC),
  DEC(Flag._Z_N, AluMode.DEC),
  DEX(Flag._Z_N, AluMode.DEC),
  DEY(Flag._Z_N, AluMode.DEC),
  EOR(Flag._Z_N, AluMode.EOR),
  INC(Flag._Z_N, AluMode.INC),
  INX(Flag._Z_N, AluMode.INC),
  INY(Flag._Z_N, AluMode.INC),
  JMP(Flag.NON_, AluMode.NOP),
  JSR(Flag.NON_, AluMode.NOP),
  LDA(Flag._Z_N, AluMode.NOP),
  LDX(Flag._Z_N, AluMode.NOP),
  LDY(Flag._Z_N, AluMode.NOP),
  LSR(Flag.CZ_N, AluMode.LSR),
  NOP(Flag.NON_, AluMode.NOP),
  ORA(Flag._Z_N, AluMode.ORA),
  PHA(Flag.NON_, AluMode.NOP),
  PHP(Flag.NON_, AluMode.NOP),
  PLA(Flag._Z_N, AluMode.NOP),  // The original datasheet claims no flags set, but rest of the world disagrees
  PLP(Flag.NON_, AluMode.NOP),
  ROL(Flag.CZ_N, AluMode.ROL),
  ROR(Flag.CZ_N, AluMode.ROR),
  RTI(Flag.NON_, AluMode.NOP),
  RTS(Flag.NON_, AluMode.NOP),
  SBC(Flag.CZVN, AluMode.SBC),
  SEC(Flag.C1__, AluMode.NOP),
  SED(Flag.D1__, AluMode.NOP),
  SEI(Flag.I1__, AluMode.NOP),
  STA(Flag.NON_, AluMode.NOP),
  STX(Flag.NON_, AluMode.NOP),
  STY(Flag.NON_, AluMode.NOP),
  TAX(Flag._Z_N, AluMode.NOP),
  TAY(Flag._Z_N, AluMode.NOP),
  TSX(Flag._Z_N, AluMode.NOP),
  TXA(Flag._Z_N, AluMode.NOP),
  TXS(Flag.NON_, AluMode.NOP),
  TYA(Flag._Z_N, AluMode.NOP)
}

enum class AddrMode {
  ACCUMULATOR,
  IMMEDIATE,
  IMPLIED,
  STACK,  // TODO - this needs to be internal-only
  INDIRECT,
  ABSOLUTE,
  ABSOLUTE_X,
  ABSOLUTE_Y,
  ZERO_PAGE,
  ZERO_PAGE_X,
  ZERO_PAGE_Y,
  INDEXED_INDIRECT,
  INDIRECT_INDEXED
}

enum class MemSrc {
  A,
  X,
  Y,
  S,
  P,
  R,  // TODO - rename - represents ALU out
  N,
  Z   // TODO
}

data class Yeah(
  val op: Opcode,
  val regSrc: Reg = Reg.N,
  val memSrc: MemSrc = MemSrc.N,
  val regSink: Reg = Reg.N,
  val addrMode: AddrMode = IMPLIED
)

private val LAYOUT_STD = listOf(
  0x01 to INDEXED_INDIRECT,
  0x05 to ZERO_PAGE,
  0x09 to IMMEDIATE,
  0x0D to ABSOLUTE,
  0x11 to INDIRECT_INDEXED,
  0x15 to ZERO_PAGE_X,
  0x19 to ABSOLUTE_Y,
  0x1D to ABSOLUTE_X
)

private val LAYOUT_INC_DEC = listOf(
  0x06 to ZERO_PAGE,
  0x0E to ABSOLUTE,
  0x16 to ZERO_PAGE_X,
  0x1E to ABSOLUTE_X
)

private val LAYOUT_SHIFT = LAYOUT_INC_DEC + listOf(
  0x0A to ACCUMULATOR
)

val ENCODINGS =
  emptyMap<UInt8, Yeah>() +
    LAYOUT_STD.encodings(0x00) { Yeah(ORA, Reg.N, MemSrc.N, Reg.A, it) } +
    LAYOUT_STD.encodings(0x20) { Yeah(AND, Reg.N, MemSrc.N, Reg.A, it) } +
    LAYOUT_STD.encodings(0x40) { Yeah(EOR, Reg.N, MemSrc.N, Reg.A, it) } +
    LAYOUT_STD.encodings(0x60) { Yeah(ADC, Reg.N, MemSrc.N, Reg.A, it) } +
    LAYOUT_STD.encodings(0x80) { Yeah(STA, Reg.N, MemSrc.A, Reg.N, it) } +  // TODO - no IMMEDIATE
    LAYOUT_STD.encodings(0xA0) { Yeah(LDA, Reg.N, MemSrc.N, Reg.A, it) } +
    LAYOUT_STD.encodings(0xC0) { Yeah(CMP, Reg.N, MemSrc.N, Reg.N, it) } +    // TODO - test
    LAYOUT_STD.encodings(0xE0) { Yeah(SBC, Reg.N, MemSrc.N, Reg.A, it) } +

    LAYOUT_SHIFT.encodings(0x00) { Yeah(ASL, Reg.Z, MemSrc.Z, Reg.Z, it) } +   // TODO - test
    LAYOUT_SHIFT.encodings(0x20) { Yeah(ROL, Reg.Z, MemSrc.Z, Reg.Z, it) } +   // TODO - test
    LAYOUT_SHIFT.encodings(0x40) { Yeah(LSR, Reg.Z, MemSrc.Z, Reg.Z, it) } +   // TODO - test
    LAYOUT_SHIFT.encodings(0x60) { Yeah(ROR, Reg.Z, MemSrc.Z, Reg.Z, it) } +   // TODO - test

    LAYOUT_INC_DEC.encodings(0xC0) { Yeah(DEC, Reg.Z, MemSrc.R, Reg.N, it) } +  // TODO - test
    LAYOUT_INC_DEC.encodings(0xE0) { Yeah(INC, Reg.Z, MemSrc.R, Reg.N, it) } +  // TODO - test

    mapOf(
      0x24 to Yeah(BIT, Reg.Z, MemSrc.N, Reg.N, ZERO_PAGE),
      0x2C to Yeah(BIT, Reg.Z, MemSrc.N, Reg.N, ABSOLUTE),

      0xCA to Yeah(DEX, Reg.X, MemSrc.N, Reg.X),
      0x88 to Yeah(DEY, Reg.Y, MemSrc.N, Reg.Y),

      0xE8 to Yeah(INX, Reg.X, MemSrc.N, Reg.X),
      0xC8 to Yeah(INY, Reg.Y, MemSrc.N, Reg.Y),

      0xE0 to Yeah(CPX, Reg.X, MemSrc.N, Reg.N, IMMEDIATE), // TODO - test
      0xE4 to Yeah(CPX, Reg.X, MemSrc.N, Reg.N, ZERO_PAGE), // TODO - test
      0xEC to Yeah(CPX, Reg.X, MemSrc.N, Reg.N, ABSOLUTE),  // TODO - test

      0xC0 to Yeah(CPY, Reg.Y, MemSrc.N, Reg.N, IMMEDIATE), // TODO - test
      0xC4 to Yeah(CPY, Reg.Y, MemSrc.N, Reg.N, ZERO_PAGE), // TODO - test
      0xCC to Yeah(CPY, Reg.Y, MemSrc.N, Reg.N, ABSOLUTE),  // TODO - test

      0x86 to Yeah(STX, Reg.N, MemSrc.X, Reg.N, ZERO_PAGE),
      0x8E to Yeah(STX, Reg.N, MemSrc.X, Reg.N, ABSOLUTE),
      0x96 to Yeah(STX, Reg.N, MemSrc.X, Reg.N, ZERO_PAGE_Y),

      0x84 to Yeah(STY, Reg.N, MemSrc.Y, Reg.N, ZERO_PAGE),
      0x8C to Yeah(STY, Reg.N, MemSrc.Y, Reg.N, ABSOLUTE),
      0x94 to Yeah(STY, Reg.N, MemSrc.Y, Reg.N, ZERO_PAGE_X),

      0xA2 to Yeah(LDX, Reg.N, MemSrc.N, Reg.X, IMMEDIATE),
      0xA6 to Yeah(LDX, Reg.N, MemSrc.N, Reg.X, ZERO_PAGE),
      0xAE to Yeah(LDX, Reg.N, MemSrc.N, Reg.X, ABSOLUTE),
      0xB6 to Yeah(LDX, Reg.N, MemSrc.N, Reg.X, ZERO_PAGE_Y),
      0xBE to Yeah(LDX, Reg.N, MemSrc.N, Reg.X, ABSOLUTE_Y),

      0xA0 to Yeah(LDY, Reg.N, MemSrc.N, Reg.Y, IMMEDIATE),
      0xA4 to Yeah(LDY, Reg.N, MemSrc.N, Reg.Y, ZERO_PAGE),
      0xAC to Yeah(LDY, Reg.N, MemSrc.N, Reg.Y, ABSOLUTE),
      0xB4 to Yeah(LDY, Reg.N, MemSrc.N, Reg.Y, ZERO_PAGE_X),
      0xBC to Yeah(LDY, Reg.N, MemSrc.N, Reg.Y, ABSOLUTE_X),

      0x08 to Yeah(PHP, Reg.N, MemSrc.P, Reg.N, STACK),
      0x28 to Yeah(PLP, Reg.N, MemSrc.N, Reg.P, STACK),
      0x48 to Yeah(PHA, Reg.N, MemSrc.A, Reg.N, STACK),
      0x68 to Yeah(PLA, Reg.N, MemSrc.N, Reg.A, STACK),

      0x18 to Yeah(CLC, Reg.N, MemSrc.N, Reg.N),
      0xD8 to Yeah(CLD, Reg.N, MemSrc.N, Reg.N),
      0x58 to Yeah(CLI, Reg.N, MemSrc.N, Reg.N),
      0xB8 to Yeah(CLV, Reg.N, MemSrc.N, Reg.N),

      0x38 to Yeah(SEC, Reg.N, MemSrc.N, Reg.N),
      0xF8 to Yeah(SED, Reg.N, MemSrc.N, Reg.N),
      0x78 to Yeah(SEI, Reg.N, MemSrc.N, Reg.N),

      0x10 to Yeah(BPL, Reg.N, MemSrc.N, Reg.N),   // TODO - test
      0x30 to Yeah(BMI, Reg.N, MemSrc.N, Reg.N),   // TODO - test
      0x50 to Yeah(BVC, Reg.N, MemSrc.N, Reg.N),   // TODO - test
      0x70 to Yeah(BVS, Reg.N, MemSrc.N, Reg.N),   // TODO - test
      0x90 to Yeah(BCC, Reg.N, MemSrc.N, Reg.N),   // TODO - test
      0xB0 to Yeah(BCS, Reg.N, MemSrc.N, Reg.N),   // TODO - test
      0xD0 to Yeah(BNE, Reg.N, MemSrc.N, Reg.N),   // TODO - test
      0xF0 to Yeah(BEQ, Reg.N, MemSrc.N, Reg.N),   // TODO - test

      0x4C to Yeah(JMP, Reg.Z, MemSrc.N, Reg.Z, ABSOLUTE),  // TODO - test
      0x6C to Yeah(JMP, Reg.Z, MemSrc.N, Reg.Z, INDIRECT),  // TODO - test
      0x20 to Yeah(JSR, Reg.Z, MemSrc.N, Reg.Z, ABSOLUTE),  // TODO - test
      0x40 to Yeah(RTI, Reg.Z, MemSrc.N, Reg.Z),   // TODO - test
      0x60 to Yeah(RTS, Reg.Z, MemSrc.N, Reg.Z),   // TODO - test

      0x8A to Yeah(TXA, Reg.X, MemSrc.N, Reg.A),
      0x98 to Yeah(TYA, Reg.Y, MemSrc.N, Reg.A),
      0x9A to Yeah(TXS, Reg.X, MemSrc.N, Reg.S),
      0xA8 to Yeah(TAY, Reg.A, MemSrc.N, Reg.Y),
      0xAA to Yeah(TAX, Reg.A, MemSrc.N, Reg.X),
      0xBA to Yeah(TSX, Reg.S, MemSrc.N, Reg.X),

      0x00 to Yeah(BRK, Reg.N, MemSrc.N, Reg.N),   // TODO - test
      0xEA to Yeah(NOP, Reg.N, MemSrc.N, Reg.N)
    ).mapKeys { (k, _) -> k.u8() }


private fun List<Pair<Int, AddrMode>>.encodings(base: Int, builder: (AddrMode) -> Yeah) =
  associate { (k, v) -> (k + base).u8() to builder(v) }

