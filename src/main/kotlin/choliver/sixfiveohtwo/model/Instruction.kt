package choliver.sixfiveohtwo.model

import choliver.sixfiveohtwo.model.Operand.Implied

data class Instruction(
  val opcode: Opcode,
  val operand: Operand = Implied
)
