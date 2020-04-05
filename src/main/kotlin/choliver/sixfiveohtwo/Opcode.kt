package choliver.sixfiveohtwo

import choliver.sixfiveohtwo.AddrMode.*

enum class AddrMode {
  ACCUMULATOR,
  IMMEDIATE,
  IMPLIED,
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

private val ENCS_STANDARD = mapOf(
  INDEXED_INDIRECT to 0x01,
  ZERO_PAGE to 0x05,
  IMMEDIATE to 0x09,
  ABSOLUTE to 0x0D,
  INDIRECT_INDEXED to 0x11,
  ZERO_PAGE_X to 0x15,
  ABSOLUTE_Y to 0x19,
  ABSOLUTE_X to 0x1D
)

private val ENCS_INC_DEC = mapOf(
  ZERO_PAGE to 0x06,
  ABSOLUTE to 0x0E,
  ZERO_PAGE_X to 0x16,
  ABSOLUTE_X to 0x1E
)

private val ENCS_SHIFT = ENCS_INC_DEC + mapOf(
  ACCUMULATOR to 0x0A
)

enum class Opcode(
  val encodings: Map<AddrMode, UInt8>
) {
  ADC(standard(0x60)),
  AND(standard(0x20)),
  ASL(shift(0x00)),
  BCC(map8(RELATIVE to 0x90)),
  BCS(map8(RELATIVE to 0xB0)),
  BEQ(map8(RELATIVE to 0xF0)),
  BIT(map8(
    ZERO_PAGE to 0x24,
    ABSOLUTE to 0x2C
  )),
  BMI(map8(RELATIVE to 0x30)),
  BNE(map8(RELATIVE to 0xD0)),
  BPL(map8(RELATIVE to 0x10)),
  BRK(0x00),  // TODO - test
  BVC(map8(RELATIVE to 0x50)),
  BVS(map8(RELATIVE to 0x70)),
  CLC(0x18),
  CLD(0xD8),
  CLI(0x58),
  CLV(0xB8),
  CMP(standard(0xC0)),
  CPX(map8(
    IMMEDIATE to 0xE0,
    ZERO_PAGE to 0xE4,
    ABSOLUTE to 0xEC
  )),
  CPY(map8(
    IMMEDIATE to 0xC0,
    ZERO_PAGE to 0xC4,
    ABSOLUTE to 0xCC
  )),
  DEC(incDec(0xC0)),
  DEX(0xCA),
  DEY(0x88),
  EOR(standard(0x40)),
  INC(incDec(0xE0)),
  INX(0xE8),
  INY(0xC8),
  JMP(map8(
    ABSOLUTE to 0x4C,
    INDIRECT to 0x6C
  )),
  JSR(map8(ABSOLUTE to 0x20)),
  LDA(standard(0xA0)),
  LDX(map8(
    IMMEDIATE to 0xA2,
    ZERO_PAGE to 0xA6,
    ABSOLUTE to 0xAE,
    ZERO_PAGE_Y to 0xB6,
    ABSOLUTE_Y to 0xBE
  )),
  LDY(map8(
    IMMEDIATE to 0xA0,
    ZERO_PAGE to 0xA4,
    ABSOLUTE to 0xAC,
    ZERO_PAGE_X to 0xB4,
    ABSOLUTE_X to 0xBC
  )),
  LSR(shift(0x40)),
  NOP(0xEA),
  ORA(standard(0x00)),
  PHA(0x48),
  PHP(0x08),
  PLA(0x68),
  PLP(0x28),
  ROL(shift(0x20)),
  ROR(shift(0x60)),
  RTI(0x40),    // TODO - test
  RTS(0x60),
  SBC(standard(0xE0)),
  SEC(0x38),
  SED(0xF8),
  SEI(0x78),
  STA(standard(0x80) - IMMEDIATE),
  STX(map8(
    ZERO_PAGE to 0x86,
    ABSOLUTE to 0x8E,
    ZERO_PAGE_Y to 0x96
  )),
  STY(map8(
    ZERO_PAGE to 0x84,
    ABSOLUTE to 0x8C,
    ZERO_PAGE_X to 0x94
  )),
  TAX(0xAA),
  TAY(0xA8),
  TSX(0xBA),
  TXA(0x8A),
  TXS(0x9A),
  TYA(0x98);

  constructor(enc: Int) : this(map8(IMPLIED to enc))
}

private fun standard(base: Int) = ENCS_STANDARD.encode(base)

private fun incDec(base: Int) = ENCS_INC_DEC.encode(base)

private fun shift(base: Int) = ENCS_SHIFT.encode(base)

private fun Map<AddrMode, Int>.encode(base: Int) = entries
  .associate { (k, v) -> k to (v + base).u8() }

// TODO - this is gross
private fun map8(vararg pairs: Pair<AddrMode, Int>) = pairs.associate { (k, v) -> k to v.u8() }
