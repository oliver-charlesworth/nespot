package choliver.nespot.debugger

import choliver.nespot.common.Address
import choliver.nespot.cpu.AddressMode.*
import choliver.nespot.cpu.Instruction
import choliver.nespot.cpu.Operand.*
import choliver.nespot.cpu.Operand.IndexSource.X
import choliver.nespot.cpu.Operand.IndexSource.Y
import choliver.nespot.nes.Nes

internal data class DecodedInstruction(
  val instruction: Instruction,
  val nextPc: Address
)

internal fun Nes.Diagnostics.currentInstruction() = instructionAt(cpu.regs.pc)

internal fun Nes.Diagnostics.instructionAt(pc: Address) = decodeInstruction(pc).instruction

internal fun Nes.Diagnostics.decodeInstruction(pc: Address): DecodedInstruction {
  val decoded = cpu.decodeAt(pc)

  val operand = when (decoded.addressMode) {
    ACCUMULATOR -> Accumulator
    IMMEDIATE -> Immediate(decoded.addr)
    IMPLIED -> Implied
    INDIRECT -> Indirect(decoded.addr)
    RELATIVE -> Relative(decoded.addr)
    ABSOLUTE -> Absolute(decoded.addr)
    ABSOLUTE_X -> AbsoluteIndexed(decoded.addr, X)
    ABSOLUTE_Y -> AbsoluteIndexed(decoded.addr, Y)
    ZERO_PAGE -> ZeroPage(decoded.addr)
    ZERO_PAGE_X -> ZeroPageIndexed(decoded.addr, X)
    ZERO_PAGE_Y -> ZeroPageIndexed(decoded.addr, Y)
    INDEXED_INDIRECT -> IndexedIndirect(decoded.addr)
    INDIRECT_INDEXED -> IndirectIndexed(decoded.addr)
  }

  return DecodedInstruction(
    instruction = Instruction(decoded.opcode, operand),
    nextPc = decoded.nextPc
  )
}
