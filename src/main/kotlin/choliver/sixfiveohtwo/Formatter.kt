package choliver.sixfiveohtwo

import choliver.sixfiveohtwo.model.Instruction
import choliver.sixfiveohtwo.model.Operand

fun Instruction.format() = "${opcode.name}${format(operand)}"

private fun format(operand: Operand) = when (operand) {
  is Operand.Implied -> ""
  is Operand.Accumulator -> " A"
  is Operand.Relative -> " $%02x".format(operand.offset)
  is Operand.Immediate -> " #$%02x".format(operand.literal.toByte())
  is Operand.ZeroPage -> " $%02x".format(operand.address.toByte())
  is Operand.ZeroPageIndexed -> " $%02x,%s".format(operand.address.toByte(), operand.source.name)
  is Operand.Absolute -> " $%04x".format(operand.address.toShort())
  is Operand.AbsoluteIndexed -> " $%04x,%s".format(operand.address.toShort(), operand.source.name)
  is Operand.Indirect -> " ($%04x)".format(operand.address.toShort())
  is Operand.IndexedIndirect -> " ($%02x,X)".format(operand.address.toByte())
  is Operand.IndirectIndexed -> " ($%02x),Y".format(operand.address.toByte())
}
