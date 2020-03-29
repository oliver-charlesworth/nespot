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
) {
  override fun toString() = "(PC=0x%04X, S=0x%02X, A=0x%02X, X=0x%02X, Y=0x%02X, P=%c%c--%c%c%c%c)".format(
    PC.toShort(),
    S.toByte(),
    A.toByte(),
    X.toByte(),
    Y.toByte(),
    if (N) 'N' else '-',
    if (V) 'V' else '-',
    if (D) 'D' else '-',
    if (I) 'I' else '-',
    if (Z) 'Z' else '-',
    if (C) 'C' else '-'
  )
}
