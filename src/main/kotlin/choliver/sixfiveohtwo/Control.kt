package choliver.sixfiveohtwo

data class Control(
  val aluMode: Alu.(Alu.Input) -> Alu.Output
)
