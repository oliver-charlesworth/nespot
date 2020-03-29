package choliver.sixfiveohtwo

data class State(
  val PC: UShort = 0x00u,
  val A: UByte = 0x00u,
  val X: UByte = 0x00u,
  val Y: UByte = 0x00u,
  val S: UByte = 0x00u,
  val C: Boolean = false,
  val Z: Boolean = false,
  val I: Boolean = false,
  val D: Boolean = false,
  val V: Boolean = false,
  val N: Boolean = false
)
