package choliver.sixfiveohtwo

import choliver.sixfiveohtwo.AddressMode
import choliver.sixfiveohtwo.Opcode

data class Instruction(
  val op: Opcode,
  val addressMode: AddressMode
)
