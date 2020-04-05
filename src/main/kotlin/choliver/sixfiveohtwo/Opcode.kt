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

enum class Opcode {
  ADC,
  AND,
  ASL,
  BCC,
  BCS,
  BEQ,
  BIT,
  BMI,
  BNE,
  BPL,
  BRK,
  BVC,
  BVS,
  CLC,
  CLD,
  CLI,
  CLV,
  CMP,
  CPX,
  CPY,
  DEC,
  DEX,
  DEY,
  EOR,
  INC,
  INX,
  INY,
  JMP,
  JSR,
  LDA,
  LDX,
  LDY,
  LSR,
  NOP,
  ORA,
  PHA,
  PHP,
  PLA,
  PLP,
  ROL,
  ROR,
  RTI,
  RTS,
  SBC,
  SEC,
  SED,
  SEI,
  STA,
  STX,
  STY,
  TAX,
  TAY,
  TSX,
  TXA,
  TXS,
  TYA
}

enum class AddrMode {
  ACCUMULATOR,
  IMMEDIATE,
  IMPLIED,
  STACK,  // TODO - this needs to be internal-only
  INDIRECT,
  RELATIVE,
  ABSOLUTE,
  ABSOLUTE_X,
  ABSOLUTE_Y,
  ZERO_PAGE,
  ZERO_PAGE_X,
  ZERO_PAGE_Y,
  INDEXED_INDIRECT,
  INDIRECT_INDEXED
}

// TODO - rename
data class Yeah(
  val op: Opcode,
  val regSrc: Reg = Reg.N,
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
    LAYOUT_STD.encodings(0x00) { Yeah(ORA, Reg.N, it) } +
    LAYOUT_STD.encodings(0x20) { Yeah(AND, Reg.N, it) } +
    LAYOUT_STD.encodings(0x40) { Yeah(EOR, Reg.N, it) } +
    LAYOUT_STD.encodings(0x60) { Yeah(ADC, Reg.N, it) } +
    LAYOUT_STD.encodings(0x80) { Yeah(STA, Reg.N, it) } +  // TODO - no IMMEDIATE
    LAYOUT_STD.encodings(0xA0) { Yeah(LDA, Reg.N, it) } +
    LAYOUT_STD.encodings(0xC0) { Yeah(CMP, Reg.N, it) } +    // TODO - test
    LAYOUT_STD.encodings(0xE0) { Yeah(SBC, Reg.N, it) } +

    LAYOUT_SHIFT.encodings(0x00) { Yeah(ASL, Reg.Z, it) } +   // TODO - test
    LAYOUT_SHIFT.encodings(0x20) { Yeah(ROL, Reg.Z, it) } +   // TODO - test
    LAYOUT_SHIFT.encodings(0x40) { Yeah(LSR, Reg.Z, it) } +   // TODO - test
    LAYOUT_SHIFT.encodings(0x60) { Yeah(ROR, Reg.Z, it) } +   // TODO - test

    LAYOUT_INC_DEC.encodings(0xC0) { Yeah(DEC, Reg.N, it) } +
    LAYOUT_INC_DEC.encodings(0xE0) { Yeah(INC, Reg.N, it) } +

    mapOf(
      0x24 to Yeah(BIT, Reg.N, ZERO_PAGE),
      0x2C to Yeah(BIT, Reg.N, ABSOLUTE),

      0xCA to Yeah(DEX, Reg.X),
      0x88 to Yeah(DEY, Reg.Y),

      0xE8 to Yeah(INX, Reg.X),
      0xC8 to Yeah(INY, Reg.Y),

      0xE0 to Yeah(CPX, Reg.X, IMMEDIATE), // TODO - test
      0xE4 to Yeah(CPX, Reg.X, ZERO_PAGE), // TODO - test
      0xEC to Yeah(CPX, Reg.X, ABSOLUTE),  // TODO - test

      0xC0 to Yeah(CPY, Reg.Y, IMMEDIATE), // TODO - test
      0xC4 to Yeah(CPY, Reg.Y, ZERO_PAGE), // TODO - test
      0xCC to Yeah(CPY, Reg.Y, ABSOLUTE),  // TODO - test

      0x86 to Yeah(STX, Reg.N, ZERO_PAGE),
      0x8E to Yeah(STX, Reg.N, ABSOLUTE),
      0x96 to Yeah(STX, Reg.N, ZERO_PAGE_Y),

      0x84 to Yeah(STY, Reg.N, ZERO_PAGE),
      0x8C to Yeah(STY, Reg.N, ABSOLUTE),
      0x94 to Yeah(STY, Reg.N, ZERO_PAGE_X),

      0xA2 to Yeah(LDX, Reg.N, IMMEDIATE),
      0xA6 to Yeah(LDX, Reg.N, ZERO_PAGE),
      0xAE to Yeah(LDX, Reg.N, ABSOLUTE),
      0xB6 to Yeah(LDX, Reg.N, ZERO_PAGE_Y),
      0xBE to Yeah(LDX, Reg.N, ABSOLUTE_Y),

      0xA0 to Yeah(LDY, Reg.N, IMMEDIATE),
      0xA4 to Yeah(LDY, Reg.N, ZERO_PAGE),
      0xAC to Yeah(LDY, Reg.N, ABSOLUTE),
      0xB4 to Yeah(LDY, Reg.N, ZERO_PAGE_X),
      0xBC to Yeah(LDY, Reg.N, ABSOLUTE_X),

      0x08 to Yeah(PHP, Reg.N, STACK),
      0x28 to Yeah(PLP, Reg.N, STACK),
      0x48 to Yeah(PHA, Reg.N, STACK),
      0x68 to Yeah(PLA, Reg.N, STACK),

      0x18 to Yeah(CLC, Reg.N),
      0xD8 to Yeah(CLD, Reg.N),
      0x58 to Yeah(CLI, Reg.N),
      0xB8 to Yeah(CLV, Reg.N),

      0x38 to Yeah(SEC, Reg.N),
      0xF8 to Yeah(SED, Reg.N),
      0x78 to Yeah(SEI, Reg.N),

      0x10 to Yeah(BPL, Reg.N, RELATIVE),
      0x30 to Yeah(BMI, Reg.N, RELATIVE),
      0x50 to Yeah(BVC, Reg.N, RELATIVE),
      0x70 to Yeah(BVS, Reg.N, RELATIVE),
      0x90 to Yeah(BCC, Reg.N, RELATIVE),
      0xB0 to Yeah(BCS, Reg.N, RELATIVE),
      0xD0 to Yeah(BNE, Reg.N, RELATIVE),
      0xF0 to Yeah(BEQ, Reg.N, RELATIVE),

      0x4C to Yeah(JMP, Reg.N, ABSOLUTE),
      0x6C to Yeah(JMP, Reg.Z, INDIRECT),
      0x20 to Yeah(JSR, Reg.N, ABSOLUTE),
      0x40 to Yeah(RTI, Reg.Z),   // TODO - test
      0x60 to Yeah(RTS, Reg.Z),

      0x8A to Yeah(TXA, Reg.X),
      0x98 to Yeah(TYA, Reg.Y),
      0x9A to Yeah(TXS, Reg.X),
      0xA8 to Yeah(TAY, Reg.A),
      0xAA to Yeah(TAX, Reg.A),
      0xBA to Yeah(TSX, Reg.S),

      0x00 to Yeah(BRK, Reg.N),   // TODO - test
      0xEA to Yeah(NOP, Reg.N)
    ).mapKeys { (k, _) -> k.u8() }


private fun List<Pair<Int, AddrMode>>.encodings(base: Int, builder: (AddrMode) -> Yeah) =
  associate { (k, v) -> (k + base).u8() to builder(v) }

