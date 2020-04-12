package choliver.nes.sixfiveohtwo

import choliver.nes.Data
import choliver.nes.data
import choliver.nes.sixfiveohtwo.model.AddressMode
import choliver.nes.sixfiveohtwo.model.AddressMode.*
import choliver.nes.sixfiveohtwo.model.Opcode
import choliver.nes.sixfiveohtwo.model.Opcode.*

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

val OPCODES_TO_ENCODINGS: Map<Opcode, Map<AddressMode, Data>> = mapOf(
  ADC to standard(0x60),
  AND to standard(0x20),
  ASL to shift(0x00),
  BCC to mapOf(RELATIVE to 0x90),
  BCS to mapOf(RELATIVE to 0xB0),
  BEQ to mapOf(RELATIVE to 0xF0),
  BIT to mapOf(
    ZERO_PAGE to 0x24,
    ABSOLUTE to 0x2C
  ),
  BMI to mapOf(RELATIVE to 0x30),
  BNE to mapOf(RELATIVE to 0xD0),
  BPL to mapOf(RELATIVE to 0x10),
  BRK to mapOf(IMPLIED to 0x00),
  BVC to mapOf(RELATIVE to 0x50),
  BVS to mapOf(RELATIVE to 0x70),
  CLC to mapOf(IMPLIED to 0x18),
  CLD to mapOf(IMPLIED to 0xD8),
  CLI to mapOf(IMPLIED to 0x58),
  CLV to mapOf(IMPLIED to 0xB8),
  CMP to standard(0xC0),
  CPX to mapOf(
    IMMEDIATE to 0xE0,
    ZERO_PAGE to 0xE4,
    ABSOLUTE to 0xEC
  ),
  CPY to mapOf(
    IMMEDIATE to 0xC0,
    ZERO_PAGE to 0xC4,
    ABSOLUTE to 0xCC
  ),
  DEC to incDec(0xC0),
  DEX to mapOf(IMPLIED to 0xCA),
  DEY to mapOf(IMPLIED to 0x88),
  EOR to standard(0x40),
  INC to incDec(0xE0),
  INX to mapOf(IMPLIED to 0xE8),
  INY to mapOf(IMPLIED to 0xC8),
  JMP to mapOf(
    ABSOLUTE to 0x4C,
    INDIRECT to 0x6C
  ),
  JSR to mapOf(ABSOLUTE to 0x20),
  LDA to standard(0xA0),
  LDX to mapOf(
    IMMEDIATE to 0xA2,
    ZERO_PAGE to 0xA6,
    ABSOLUTE to 0xAE,
    ZERO_PAGE_Y to 0xB6,
    ABSOLUTE_Y to 0xBE
  ),
  LDY to mapOf(
    IMMEDIATE to 0xA0,
    ZERO_PAGE to 0xA4,
    ABSOLUTE to 0xAC,
    ZERO_PAGE_X to 0xB4,
    ABSOLUTE_X to 0xBC
  ),
  LSR to shift(0x40),
  NOP to mapOf(IMPLIED to 0xEA),
  ORA to standard(0x00),
  PHA to mapOf(IMPLIED to 0x48),
  PHP to mapOf(IMPLIED to 0x08),
  PLA to mapOf(IMPLIED to 0x68),
  PLP to mapOf(IMPLIED to 0x28),
  ROL to shift(0x20),
  ROR to shift(0x60),
  RTI to mapOf(IMPLIED to 0x40),
  RTS to mapOf(IMPLIED to 0x60),
  SBC to standard(0xE0),
  SEC to mapOf(IMPLIED to 0x38),
  SED to mapOf(IMPLIED to 0xF8),
  SEI to mapOf(IMPLIED to 0x78),
  STA to standard(0x80) - IMMEDIATE,
  STX to mapOf(
    ZERO_PAGE to 0x86,
    ABSOLUTE to 0x8E,
    ZERO_PAGE_Y to 0x96
  ),
  STY to mapOf(
    ZERO_PAGE to 0x84,
    ABSOLUTE to 0x8C,
    ZERO_PAGE_X to 0x94
  ),
  TAX to mapOf(IMPLIED to 0xAA),
  TAY to mapOf(IMPLIED to 0xA8),
  TSX to mapOf(IMPLIED to 0xBA),
  TXA to mapOf(IMPLIED to 0x8A),
  TXS to mapOf(IMPLIED to 0x9A),
  TYA to mapOf(IMPLIED to 0x98)
)

private fun standard(base: Int) = ENCS_STANDARD.encode(base)

private fun incDec(base: Int) = ENCS_INC_DEC.encode(base)

private fun shift(base: Int) = ENCS_SHIFT.encode(base)

private fun Map<AddressMode, Data>.encode(base: Data) = entries
  .associate { (k, v) -> k to (v + base).data() }
