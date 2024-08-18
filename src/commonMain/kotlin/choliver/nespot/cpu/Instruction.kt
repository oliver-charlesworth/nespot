package choliver.nespot.cpu

import choliver.nespot.common.format16
import choliver.nespot.common.format8
import choliver.nespot.cpu.Operand.*

data class Instruction(
  val opcode: Opcode,
  val operand: Operand = Implied
) {
  override fun toString() = "${opcode.name.lowercase()}${formatOperand()}"

  private fun formatOperand() = when (operand) {
    is Implied -> ""
    is Accumulator -> " A"
    is Relative -> " ${operand.offset.format8()}"
    is Immediate -> " #${operand.literal.format8()}"
    is ZeroPage -> " ${operand.addr.format8()}"
    is ZeroPageIndexed -> " ${operand.addr.format8()},${operand.source.name}"
    is Absolute -> " ${operand.addr.format16()}"
    is AbsoluteIndexed -> " ${operand.addr.format16()},${operand.source.name}"
    is Indirect -> " (${operand.addr.format16()})"
    is IndexedIndirect -> " (${operand.addr.format8()},X)"
    is IndirectIndexed -> " (${operand.addr.format8()}),Y"
  }
}
