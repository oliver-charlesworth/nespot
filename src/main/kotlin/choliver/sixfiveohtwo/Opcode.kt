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
  // TODO - could these be implemented via ALU with a bitmask ?
  C0__,
  C1__,
  I0__,
  I1__,
  D0__,
  D1__,
  V0__
}

// TODO - could these be implemented via ALU?
enum class Stack {
  PUSH,
  POP_,
  NONE
}

enum class Opcode(
  val RegIn: Reg,
  val aluA: AluSrc,
  val aluB: AluSrc,
  val out: OutSrc,
  val regOut: Reg,
  val memOut: Boolean,
  val flag: Flag,
  val stack: Stack,
  val aluMode: Alu.(Alu.Input) -> Alu.Output
) {
  ADC(Reg.A, AluSrc.REG, AluSrc.MEM, OutSrc.ALU, Reg.A, _0, Flag.CZVN, Stack.NONE, Alu::adc),
  AND(Reg.A, AluSrc.REG, AluSrc.MEM, OutSrc.ALU, Reg.A, _0, Flag._Z_N, Stack.NONE, Alu::and),
  ASL(Reg.Z, AluSrc.ZZZ, AluSrc.ZZZ, OutSrc.ZZZ, Reg.Z, _0, Flag.CZ_N, Stack.NONE, Alu::asl),
  BCC(Reg.Z, AluSrc.ZZZ, AluSrc.ZZZ, OutSrc.ZZZ, Reg.Z, _0, Flag.NON_, Stack.NONE, Alu::nop),
  BCS(Reg.Z, AluSrc.ZZZ, AluSrc.ZZZ, OutSrc.ZZZ, Reg.Z, _0, Flag.NON_, Stack.NONE, Alu::nop),
  BEQ(Reg.Z, AluSrc.ZZZ, AluSrc.ZZZ, OutSrc.ZZZ, Reg.Z, _0, Flag.NON_, Stack.NONE, Alu::nop),
  BIT(Reg.Z, AluSrc.ZZZ, AluSrc.ZZZ, OutSrc.ZZZ, Reg.Z, _0, Flag._ZVN, Stack.NONE, Alu::nop),
  BMI(Reg.Z, AluSrc.ZZZ, AluSrc.ZZZ, OutSrc.ZZZ, Reg.Z, _0, Flag.NON_, Stack.NONE, Alu::nop),
  BNE(Reg.Z, AluSrc.ZZZ, AluSrc.ZZZ, OutSrc.ZZZ, Reg.Z, _0, Flag.NON_, Stack.NONE, Alu::nop),
  BPL(Reg.Z, AluSrc.ZZZ, AluSrc.ZZZ, OutSrc.ZZZ, Reg.Z, _0, Flag.NON_, Stack.NONE, Alu::nop),
  BRK(Reg.Z, AluSrc.ZZZ, AluSrc.ZZZ, OutSrc.ZZZ, Reg.Z, _0, Flag.NON_, Stack.NONE, Alu::nop),
  BVC(Reg.Z, AluSrc.ZZZ, AluSrc.ZZZ, OutSrc.ZZZ, Reg.Z, _0, Flag.NON_, Stack.NONE, Alu::nop),
  BVS(Reg.Z, AluSrc.ZZZ, AluSrc.ZZZ, OutSrc.ZZZ, Reg.Z, _0, Flag.NON_, Stack.NONE, Alu::nop),
  CLC(Reg.N, AluSrc.NON, AluSrc.NON, OutSrc.NON, Reg.N, _0, Flag.C0__, Stack.NONE, Alu::nop),
  CLD(Reg.N, AluSrc.NON, AluSrc.NON, OutSrc.NON, Reg.N, _0, Flag.D0__, Stack.NONE, Alu::nop),
  CLI(Reg.N, AluSrc.NON, AluSrc.NON, OutSrc.NON, Reg.N, _0, Flag.I0__, Stack.NONE, Alu::nop),
  CLV(Reg.N, AluSrc.NON, AluSrc.NON, OutSrc.NON, Reg.N, _0, Flag.V0__, Stack.NONE, Alu::nop),
  CMP(Reg.A, AluSrc.REG, AluSrc.MEM, OutSrc.NON, Reg.N, _0, Flag.CZ_N, Stack.NONE, Alu::sbc),
  CPX(Reg.A, AluSrc.REG, AluSrc.MEM, OutSrc.NON, Reg.N, _0, Flag.CZ_N, Stack.NONE, Alu::sbc),
  CPY(Reg.A, AluSrc.REG, AluSrc.MEM, OutSrc.NON, Reg.N, _0, Flag.CZ_N, Stack.NONE, Alu::sbc),
  DEC(Reg.N, AluSrc.NON, AluSrc.MEM, OutSrc.ALU, Reg.N, _1, Flag._Z_N, Stack.NONE, Alu::dec),
  DEX(Reg.X, AluSrc.NON, AluSrc.REG, OutSrc.ALU, Reg.X, _0, Flag._Z_N, Stack.NONE, Alu::dec),
  DEY(Reg.Y, AluSrc.NON, AluSrc.REG, OutSrc.ALU, Reg.Y, _0, Flag._Z_N, Stack.NONE, Alu::dec),
  EOR(Reg.A, AluSrc.REG, AluSrc.MEM, OutSrc.ALU, Reg.A, _0, Flag._Z_N, Stack.NONE, Alu::eor),
  INC(Reg.N, AluSrc.NON, AluSrc.MEM, OutSrc.ALU, Reg.N, _1, Flag._Z_N, Stack.NONE, Alu::inc),
  INX(Reg.X, AluSrc.NON, AluSrc.REG, OutSrc.ALU, Reg.X, _0, Flag._Z_N, Stack.NONE, Alu::inc),
  INY(Reg.Y, AluSrc.NON, AluSrc.REG, OutSrc.ALU, Reg.Y, _0, Flag._Z_N, Stack.NONE, Alu::inc),
  JMP(Reg.Z, AluSrc.ZZZ, AluSrc.ZZZ, OutSrc.ZZZ, Reg.Z, _0, Flag.NON_, Stack.NONE, Alu::nop),
  JSR(Reg.Z, AluSrc.ZZZ, AluSrc.ZZZ, OutSrc.ZZZ, Reg.Z, _0, Flag.NON_, Stack.NONE, Alu::nop),
  LDA(Reg.N, AluSrc.NON, AluSrc.NON, OutSrc.MEM, Reg.A, _0, Flag._Z_N, Stack.NONE, Alu::nop),
  LDX(Reg.N, AluSrc.NON, AluSrc.NON, OutSrc.MEM, Reg.X, _0, Flag._Z_N, Stack.NONE, Alu::nop),
  LDY(Reg.N, AluSrc.NON, AluSrc.NON, OutSrc.MEM, Reg.Y, _0, Flag._Z_N, Stack.NONE, Alu::nop),
  LSR(Reg.Z, AluSrc.ZZZ, AluSrc.ZZZ, OutSrc.ZZZ, Reg.Z, _0, Flag.CZ_N, Stack.NONE, Alu::lsr),
  NOP(Reg.N, AluSrc.NON, AluSrc.NON, OutSrc.NON, Reg.N, _0, Flag.NON_, Stack.NONE, Alu::nop),
  ORA(Reg.A, AluSrc.REG, AluSrc.MEM, OutSrc.ALU, Reg.A, _0, Flag._Z_N, Stack.NONE, Alu::ora),
  PHA(Reg.A, AluSrc.NON, AluSrc.NON, OutSrc.REG, Reg.N, _1, Flag.NON_, Stack.PUSH, Alu::nop),
  PHP(Reg.P, AluSrc.NON, AluSrc.NON, OutSrc.REG, Reg.N, _1, Flag.NON_, Stack.PUSH, Alu::nop),
  PLA(Reg.N, AluSrc.NON, AluSrc.NON, OutSrc.MEM, Reg.A, _0, Flag._Z_N, Stack.POP_, Alu::nop),
  PLP(Reg.N, AluSrc.NON, AluSrc.NON, OutSrc.MEM, Reg.P, _0, Flag.NON_, Stack.POP_, Alu::nop),
  ROL(Reg.Z, AluSrc.ZZZ, AluSrc.ZZZ, OutSrc.ZZZ, Reg.Z, _0, Flag.CZ_N, Stack.NONE, Alu::rol),
  ROR(Reg.Z, AluSrc.ZZZ, AluSrc.ZZZ, OutSrc.ZZZ, Reg.Z, _0, Flag.CZ_N, Stack.NONE, Alu::ror),
  RTI(Reg.Z, AluSrc.ZZZ, AluSrc.ZZZ, OutSrc.ZZZ, Reg.Z, _0, Flag.NON_, Stack.NONE, Alu::nop),
  RTS(Reg.Z, AluSrc.ZZZ, AluSrc.ZZZ, OutSrc.ZZZ, Reg.Z, _0, Flag.NON_, Stack.NONE, Alu::nop),
  SBC(Reg.A, AluSrc.REG, AluSrc.MEM, OutSrc.ALU, Reg.A, _0, Flag.CZVN, Stack.NONE, Alu::sbc),
  SEC(Reg.N, AluSrc.NON, AluSrc.NON, OutSrc.NON, Reg.N, _0, Flag.C1__, Stack.NONE, Alu::nop),
  SED(Reg.N, AluSrc.NON, AluSrc.NON, OutSrc.NON, Reg.N, _0, Flag.D1__, Stack.NONE, Alu::nop),
  SEI(Reg.N, AluSrc.NON, AluSrc.NON, OutSrc.NON, Reg.N, _0, Flag.I1__, Stack.NONE, Alu::nop),
  STA(Reg.A, AluSrc.NON, AluSrc.NON, OutSrc.REG, Reg.N, _1, Flag.NON_, Stack.NONE, Alu::nop),
  STX(Reg.X, AluSrc.NON, AluSrc.NON, OutSrc.REG, Reg.N, _1, Flag.NON_, Stack.NONE, Alu::nop),
  STY(Reg.Y, AluSrc.NON, AluSrc.NON, OutSrc.REG, Reg.N, _1, Flag.NON_, Stack.NONE, Alu::nop),
  TAX(Reg.A, AluSrc.NON, AluSrc.NON, OutSrc.REG, Reg.X, _0, Flag._Z_N, Stack.NONE, Alu::nop),
  TAY(Reg.A, AluSrc.NON, AluSrc.NON, OutSrc.REG, Reg.Y, _0, Flag._Z_N, Stack.NONE, Alu::nop),
  TSX(Reg.S, AluSrc.NON, AluSrc.NON, OutSrc.REG, Reg.X, _0, Flag._Z_N, Stack.NONE, Alu::nop),
  TXA(Reg.X, AluSrc.NON, AluSrc.NON, OutSrc.REG, Reg.A, _0, Flag._Z_N, Stack.NONE, Alu::nop),
  TXS(Reg.X, AluSrc.NON, AluSrc.NON, OutSrc.REG, Reg.S, _0, Flag.NON_, Stack.NONE, Alu::nop),
  TYA(Reg.Y, AluSrc.NON, AluSrc.NON, OutSrc.REG, Reg.A, _0, Flag._Z_N, Stack.NONE, Alu::nop)
}

enum class AddrMode {
  ACCUMULATOR,
  IMMEDIATE,
  IMPLIED,
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
  val memSrc: MemSrc,
  val regSink: Reg,
  val addrMode: AddrMode
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
    LAYOUT_STD.encodings(0x00) { Yeah(ORA, MemSrc.N, Reg.A, it) } +
    LAYOUT_STD.encodings(0x20) { Yeah(AND, MemSrc.N, Reg.A, it) } +
    LAYOUT_STD.encodings(0x40) { Yeah(EOR, MemSrc.N, Reg.A, it) } +
    LAYOUT_STD.encodings(0x60) { Yeah(ADC, MemSrc.N, Reg.A, it) } +
    LAYOUT_STD.encodings(0x80) { Yeah(STA, MemSrc.A, Reg.N, it) } +  // TODO - no IMMEDIATE
    LAYOUT_STD.encodings(0xA0) { Yeah(LDA, MemSrc.N, Reg.A, it) } +
    LAYOUT_STD.encodings(0xC0) { Yeah(CMP, MemSrc.N, Reg.N, it) } +
    LAYOUT_STD.encodings(0xE0) { Yeah(SBC, MemSrc.N, Reg.A, it) } +

    LAYOUT_SHIFT.encodings(0x00) { Yeah(ASL, MemSrc.Z, Reg.Z, it) } +   // TODO
    LAYOUT_SHIFT.encodings(0x20) { Yeah(ROL, MemSrc.Z, Reg.Z, it) } +   // TODO
    LAYOUT_SHIFT.encodings(0x40) { Yeah(LSR, MemSrc.Z, Reg.Z, it) } +   // TODO
    LAYOUT_SHIFT.encodings(0x60) { Yeah(ROR, MemSrc.Z, Reg.Z, it) } +   // TODO

    LAYOUT_INC_DEC.encodings(0xC0) { Yeah(DEC, MemSrc.R, Reg.N, it) } +
    LAYOUT_INC_DEC.encodings(0xE0) { Yeah(INC, MemSrc.R, Reg.N, it) } +

    mapOf(
      0x24 to Yeah(BIT, MemSrc.N, Reg.N, ZERO_PAGE),
      0x2C to Yeah(BIT, MemSrc.N, Reg.N, ABSOLUTE),

      0xCA to Yeah(DEX, MemSrc.N, Reg.X, IMPLIED),  // TODO
      0x88 to Yeah(DEY, MemSrc.N, Reg.Y, IMPLIED),  // TODO

      0xE8 to Yeah(INX, MemSrc.N, Reg.X, IMPLIED),  // TODO
      0xC8 to Yeah(INY, MemSrc.N, Reg.Y, IMPLIED),  // TODO

      0xE0 to Yeah(CPX, MemSrc.N, Reg.N, IMMEDIATE),
      0xE4 to Yeah(CPX, MemSrc.N, Reg.N, ZERO_PAGE),
      0xEC to Yeah(CPX, MemSrc.N, Reg.N, ABSOLUTE),

      0xC0 to Yeah(CPY, MemSrc.N, Reg.N, IMMEDIATE),
      0xC4 to Yeah(CPY, MemSrc.N, Reg.N, ZERO_PAGE),
      0xCC to Yeah(CPY, MemSrc.N, Reg.N, ABSOLUTE),

      0x86 to Yeah(STX, MemSrc.X, Reg.N, ZERO_PAGE),
      0x8E to Yeah(STX, MemSrc.X, Reg.N, ABSOLUTE),
      0x96 to Yeah(STX, MemSrc.X, Reg.N, ZERO_PAGE_Y),

      0x84 to Yeah(STY, MemSrc.Y, Reg.N, ZERO_PAGE),
      0x8C to Yeah(STY, MemSrc.Y, Reg.N, ABSOLUTE),
      0x94 to Yeah(STY, MemSrc.Y, Reg.N, ZERO_PAGE_X),

      0xA2 to Yeah(LDX, MemSrc.N, Reg.X, IMMEDIATE),
      0xA6 to Yeah(LDX, MemSrc.N, Reg.X, ZERO_PAGE),
      0xAE to Yeah(LDX, MemSrc.N, Reg.X, ABSOLUTE),
      0xB6 to Yeah(LDX, MemSrc.N, Reg.X, ZERO_PAGE_Y),
      0xBE to Yeah(LDX, MemSrc.N, Reg.X, ABSOLUTE_Y),

      0xA0 to Yeah(LDY, MemSrc.N, Reg.Y, IMMEDIATE),
      0xA4 to Yeah(LDY, MemSrc.N, Reg.Y, ZERO_PAGE),
      0xAC to Yeah(LDY, MemSrc.N, Reg.Y, ABSOLUTE),
      0xB4 to Yeah(LDY, MemSrc.N, Reg.Y, ZERO_PAGE_X),
      0xBC to Yeah(LDY, MemSrc.N, Reg.Y, ABSOLUTE_X),

      0x08 to Yeah(PHP, MemSrc.Z, Reg.Z, IMPLIED),  // TODO
      0x28 to Yeah(PLP, MemSrc.Z, Reg.Z, IMPLIED),  // TODO
      0x48 to Yeah(PHA, MemSrc.Z, Reg.Z, IMPLIED),  // TODO
      0x68 to Yeah(PLA, MemSrc.Z, Reg.Z, IMPLIED),  // TODO

      0x18 to Yeah(CLC, MemSrc.N, Reg.N, IMPLIED),
      0xD8 to Yeah(CLD, MemSrc.N, Reg.N, IMPLIED),
      0x58 to Yeah(CLI, MemSrc.N, Reg.N, IMPLIED),
      0xB8 to Yeah(CLV, MemSrc.N, Reg.N, IMPLIED),

      0x38 to Yeah(SEC, MemSrc.N, Reg.N, IMPLIED),
      0xF8 to Yeah(SED, MemSrc.N, Reg.N, IMPLIED),
      0x78 to Yeah(SEI, MemSrc.N, Reg.N, IMPLIED),

      0x10 to Yeah(BPL, MemSrc.N, Reg.N, IMPLIED),
      0x30 to Yeah(BMI, MemSrc.N, Reg.N, IMPLIED),
      0x50 to Yeah(BVC, MemSrc.N, Reg.N, IMPLIED),
      0x70 to Yeah(BVS, MemSrc.N, Reg.N, IMPLIED),
      0x90 to Yeah(BCC, MemSrc.N, Reg.N, IMPLIED),
      0xB0 to Yeah(BCS, MemSrc.N, Reg.N, IMPLIED),
      0xD0 to Yeah(BNE, MemSrc.N, Reg.N, IMPLIED),
      0xF0 to Yeah(BEQ, MemSrc.N, Reg.N, IMPLIED),

      0x4C to Yeah(JMP, MemSrc.N, Reg.Z, ABSOLUTE),  // TODO
      0x6C to Yeah(JMP, MemSrc.N, Reg.Z, INDIRECT),  // TODO
      0x20 to Yeah(JSR, MemSrc.N, Reg.Z, ABSOLUTE),  // TODO
      0x40 to Yeah(RTI, MemSrc.N, Reg.Z, IMPLIED),  // TODO
      0x60 to Yeah(RTS, MemSrc.N, Reg.Z, IMPLIED),  // TODO

      0x98 to Yeah(TYA, MemSrc.N, Reg.A, IMPLIED),  // TODO
      0xA8 to Yeah(TAY, MemSrc.N, Reg.Y, IMPLIED),  // TODO
      0x8A to Yeah(TXA, MemSrc.N, Reg.A, IMPLIED),  // TODO
      0x9A to Yeah(TXS, MemSrc.N, Reg.S, IMPLIED),  // TODO
      0xAA to Yeah(TAX, MemSrc.N, Reg.X, IMPLIED),  // TODO
      0xBA to Yeah(TSX, MemSrc.N, Reg.X, IMPLIED),  // TODO

      0x00 to Yeah(BRK, MemSrc.N, Reg.N, IMPLIED),
      0xEA to Yeah(NOP, MemSrc.N, Reg.N, IMPLIED)
    ).mapKeys { (k, _) -> k.u8() }


private fun List<Pair<Int, AddrMode>>.encodings(base: Int, builder: (AddrMode) -> Yeah) =
  associate { (k, v) -> (k + base).u8() to builder(v) }

