package choliver.sixfiveohtwo

import choliver.sixfiveohtwo.AddrMode.*
import choliver.sixfiveohtwo.AddressMode.Implied
import choliver.sixfiveohtwo.AddressMode.IndexedIndirect
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
  IMMEDIATE,
  IMPLIED,
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
  N
}

data class Yeah(
  val op: Opcode,
  val memSrc: MemSrc,
  val regSink: Reg,
  val addrMode: AddrMode
)

val ENCODINGS = mapOf<UInt8, Yeah>(
  0x00.u8() to Yeah(BRK, MemSrc.N, Reg.N, IMPLIED),

  0x81.u8() to Yeah(STA, MemSrc.A, Reg.N, INDEXED_INDIRECT),
  0x84.u8() to Yeah(STY, MemSrc.Y, Reg.N, ZERO_PAGE),
  0x85.u8() to Yeah(STA, MemSrc.A, Reg.N, ZERO_PAGE),
  0x86.u8() to Yeah(STX, MemSrc.X, Reg.N, ZERO_PAGE),
  // TODO - 0x88 -> DEY
  // TODO - 0x8A -> TXA
  0x8C.u8() to Yeah(STY, MemSrc.Y, Reg.N, ABSOLUTE),
  0x8D.u8() to Yeah(STA, MemSrc.A, Reg.N, ABSOLUTE),
  0x8E.u8() to Yeah(STX, MemSrc.X, Reg.N, ABSOLUTE),

  // TODO - 0x90 -> BCC
  0x91.u8() to Yeah(STA, MemSrc.A, Reg.N, INDIRECT_INDEXED),
  0x94.u8() to Yeah(STY, MemSrc.Y, Reg.N, ZERO_PAGE_X),
  0x95.u8() to Yeah(STA, MemSrc.A, Reg.N, ZERO_PAGE_X),
  0x96.u8() to Yeah(STX, MemSrc.X, Reg.N, ZERO_PAGE_Y),
  // TODO - 0x98 -> TYA
  0x99.u8() to Yeah(STA, MemSrc.A, Reg.N, ABSOLUTE_Y),
  // TODO - 0x9A -> TXS
  0x9D.u8() to Yeah(STA, MemSrc.A, Reg.N, ABSOLUTE_X),

  0xA0.u8() to Yeah(LDY, MemSrc.N, Reg.Y, IMMEDIATE),
  0xA1.u8() to Yeah(LDA, MemSrc.N, Reg.A, INDEXED_INDIRECT),
  0xA2.u8() to Yeah(LDX, MemSrc.N, Reg.X, IMMEDIATE),
  0xA4.u8() to Yeah(LDY, MemSrc.N, Reg.Y, ZERO_PAGE),
  0xA5.u8() to Yeah(LDA, MemSrc.N, Reg.A, ZERO_PAGE),
  0xA6.u8() to Yeah(LDX, MemSrc.N, Reg.X, ZERO_PAGE),
  // TODO - 0xA8 -> TAY
  0xA9.u8() to Yeah(LDA, MemSrc.N, Reg.A, IMMEDIATE),
  // TODO - 0xAA -> TAX
  0xAC.u8() to Yeah(LDY, MemSrc.N, Reg.Y, ABSOLUTE),
  0xAD.u8() to Yeah(LDA, MemSrc.N, Reg.A, ABSOLUTE),
  0xAE.u8() to Yeah(LDX, MemSrc.N, Reg.X, ABSOLUTE),

  // TODO - 0xB0 -> BCS
  0xB1.u8() to Yeah(LDA, MemSrc.N, Reg.A, INDIRECT_INDEXED),
  0xB4.u8() to Yeah(LDY, MemSrc.N, Reg.Y, ZERO_PAGE_X),
  0xB5.u8() to Yeah(LDA, MemSrc.N, Reg.A, ZERO_PAGE_X),
  0xB6.u8() to Yeah(LDX, MemSrc.N, Reg.X, ZERO_PAGE_Y),
  // TODO - 0xB8 -> CLV
  0xB9.u8() to Yeah(LDA, MemSrc.N, Reg.A, ABSOLUTE_Y),
  // TODO - 0xBA -> TSX
  0xBC.u8() to Yeah(LDY, MemSrc.N, Reg.Y, ABSOLUTE_X),
  0xBD.u8() to Yeah(LDA, MemSrc.N, Reg.A, ABSOLUTE_X),
  0xBE.u8() to Yeah(LDX, MemSrc.N, Reg.X, ABSOLUTE_Y)
)

