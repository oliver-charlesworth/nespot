package choliver.nes.sixfiveohtwo.model

import choliver.nes.sixfiveohtwo.model.Operand.Implied

data class Instruction(
  val opcode: Opcode,
  val operand: Operand = Implied
)
