package choliver.nespot.cpu

import choliver.nespot.Data
import choliver.nespot.cpu.model.AddressMode
import choliver.nespot.cpu.model.AddressMode.*
import choliver.nespot.cpu.model.Opcode
import choliver.nespot.cpu.model.Opcode.*
import choliver.nespot.cpu.model.Operand.IndexSource
import choliver.nespot.cpu.model.Operand.IndexSource.X
import choliver.nespot.cpu.model.Operand.IndexSource.Y

data class EncodingInfo(
  val encoding: Data,
  val numCycles: Int,
  val extraCycleForPageCrossing: Boolean
)

val OPCODES_TO_ENCODINGS: Map<Opcode, Map<AddressMode, EncodingInfo>> = mapOf(
  ADC to standard(0x60),
  AND to standard(0x20),
  ASL to shift(0x00),
  BCC to branch(0x90),
  BCS to branch(0xB0),
  BEQ to branch(0xF0),
  BIT to mapOf(
    ZERO_PAGE to e(0x24, 3),
    ABSOLUTE to e(0x2C, 4)
  ),
  BMI to branch(0x30),
  BNE to branch(0xD0),
  BPL to branch(0x10),
  BRK to implied(0x00, 7),
  BVC to branch(0x50),
  BVS to branch(0x70),
  CLC to implied(0x18, 2),
  CLD to implied(0xD8, 2),
  CLI to implied(0x58, 2),
  CLV to implied(0xB8, 2),
  CMP to standard(0xC0),
  CPX to cmp(0xE0),
  CPY to cmp(0xC0),
  DEC to incDec(0xC0),
  DEX to implied(0xCA, 2),
  DEY to implied(0x88, 2),
  EOR to standard(0x40),
  INC to incDec(0xE0),
  INX to implied(0xE8, 2),
  INY to implied(0xC8, 2),
  JMP to mapOf(
    ABSOLUTE to e(0x4C, 3),
    INDIRECT to e(0x6C, 5)
  ),
  JSR to absolute(0x20, 6),
  LDA to standard(0xA0),
  LDX to load(0xA2, Y),
  LDY to load(0xA0, X),
  LSR to shift(0x40),
  NOP to implied(0xEA, 2),
  ORA to standard(0x00),
  PHA to implied(0x48, 3),
  PHP to implied(0x08, 3),
  PLA to implied(0x68, 4),
  PLP to implied(0x28, 4),
  ROL to shift(0x20),
  ROR to shift(0x60),
  RTI to implied(0x40, 6),
  RTS to implied(0x60, 6),
  SBC to standard(0xE0),
  SEC to implied(0x38, 2),
  SED to implied(0xF8, 2),
  SEI to implied(0x78, 2),
  STA to store(0x80),
  STX to store(0x86, Y),
  STY to store(0x84, X),
  TAX to implied(0xAA, 2),
  TAY to implied(0xA8, 2),
  TSX to implied(0xBA, 2),
  TXA to implied(0x8A, 2),
  TXS to implied(0x9A, 2),
  TYA to implied(0x98, 2)
)

private fun standard(base: Int) = mapOf(
  INDEXED_INDIRECT to e(0x01, 6),
  ZERO_PAGE to e(0x05, 3),
  IMMEDIATE to e(0x09, 2),
  ABSOLUTE to e(0x0D, 4),
  INDIRECT_INDEXED to e(0x11, 5, extraCycleForPageCrossing = true),
  ZERO_PAGE_X to e(0x15, 4),
  ABSOLUTE_Y to e(0x19, 4, extraCycleForPageCrossing = true),
  ABSOLUTE_X to e(0x1D, 4, extraCycleForPageCrossing = true)
).encode(base)

private fun store(base: Int) = mapOf(
  INDEXED_INDIRECT to e(0x01, 6),
  ZERO_PAGE to e(0x05, 3),
  ABSOLUTE to e(0x0D, 4),
  INDIRECT_INDEXED to e(0x11, 6),
  ZERO_PAGE_X to e(0x15, 4),
  ABSOLUTE_Y to e(0x19, 5),
  ABSOLUTE_X to e(0x1D, 5)
).encode(base)

private fun incDec(base: Int) = mapOf(
  ZERO_PAGE to e(0x06, 5),
  ABSOLUTE to e(0x0E, 6),
  ZERO_PAGE_X to e(0x16, 6),
  ABSOLUTE_X to e(0x1E, 7)
).encode(base)

private fun shift(base: Int) = incDec(base) + mapOf(
  ACCUMULATOR to e(0x0A, 2)
).encode(base)

private fun cmp(base: Int) = mapOf(
  IMMEDIATE to e(0x00, 2),
  ZERO_PAGE to e(0x04, 3),
  ABSOLUTE to e(0x0C, 4)
).encode(base)

private fun load(base: Int, source: IndexSource) = mapOf(
  IMMEDIATE to e(0x00, 2),
  ZERO_PAGE to e(0x04, 3),
  ABSOLUTE to e(0x0C, 4),
  zeroPageIndexedMode(source) to e(0x14, 4),
  absoluteIndexedMode(source) to e(0x1C, 4, extraCycleForPageCrossing = true)
).encode(base)

private fun store(base: Int, source: IndexSource) = mapOf(
  ZERO_PAGE to e(0x00, 3),
  ABSOLUTE to e(0x08, 4),
  zeroPageIndexedMode(source) to e(0x10, 4)
).encode(base)

private fun absoluteIndexedMode(source: IndexSource) = when (source) {
  X -> ABSOLUTE_X
  Y -> ABSOLUTE_Y
}
private fun zeroPageIndexedMode(source: IndexSource) = when (source) {
  X -> ZERO_PAGE_X
  Y -> ZERO_PAGE_Y
}

private fun branch(enc: Int) = mapOf(RELATIVE to e(enc, 2))

private fun implied(enc: Int, numCycles: Int) = mapOf(IMPLIED to e(enc, numCycles))

private fun absolute(enc: Int, numCycles: Int) = mapOf(ABSOLUTE to e(enc, numCycles))

private fun e(
  encoding: Data,
  numCycles: Int,
  extraCycleForPageCrossing: Boolean = false
) = EncodingInfo(encoding, numCycles, extraCycleForPageCrossing)

private fun Map<AddressMode, EncodingInfo>.encode(base: Data) = entries
  .associate { (k, v) -> k to v.copy(encoding = v.encoding + base) }
