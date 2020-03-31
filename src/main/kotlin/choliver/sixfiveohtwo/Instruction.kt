package choliver.sixfiveohtwo

data class Instruction(
  val op: Opcode,
  val addressMode: AddressMode = AddressMode.Implied
)
