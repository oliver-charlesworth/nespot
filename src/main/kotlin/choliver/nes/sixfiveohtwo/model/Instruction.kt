package choliver.nes.sixfiveohtwo.model

import choliver.nes.sixfiveohtwo.model.Operand.*

data class Instruction(
  val opcode: Opcode,
  val operand: Operand = Implied
) {
  override fun toString() = "${opcode.name.toLowerCase()}${formatOperand()}"

  private fun formatOperand() = when (operand) {
    is Implied -> ""
    is Accumulator -> " A"
    is Relative -> " $%02x".format(operand.offset)
    is Immediate -> " #$%02x".format(operand.literal.toByte())
    is ZeroPage -> " $%02x".format(operand.addr.toByte())
    is ZeroPageIndexed -> " $%02x,%s".format(operand.addr.toByte(), operand.source.name)
    is Absolute -> " $%04x".format(operand.addr.toShort())
    is AbsoluteIndexed -> " $%04x,%s".format(operand.addr.toShort(), operand.source.name)
    is Indirect -> " ($%04x)".format(operand.addr.toShort())
    is IndexedIndirect -> " ($%02x,X)".format(operand.addr.toByte())
    is IndirectIndexed -> " ($%02x),Y".format(operand.addr.toByte())
  }
}
