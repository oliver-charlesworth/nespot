package choliver.sixfiveohtwo

data class State(
  val PC: UInt16 = 0x00u,
  val A: UInt8 = 0x00u,
  val X: UInt8 = 0x00u,
  val Y: UInt8 = 0x00u,
  val S: UInt8 = 0x00u,
  val P: Flags = Flags()
) {
  override fun toString() = "(PC=0x%04X, S=0x%02X, A=0x%02X, X=0x%02X, Y=0x%02X, P=%s)".format(
    PC.toShort(),
    S.toByte(),
    A.toByte(),
    X.toByte(),
    Y.toByte(),
    P.toString()
  )

  /** Like [copy], but allows us to also set individual status flags. */
  fun with(
    A: UInt8 = this.A,
    X: UInt8 = this.X,
    Y: UInt8 = this.Y,
    S: UInt8 = this.S,
    N: Boolean = P.N,
    V: Boolean = P.V,
    D: Boolean = P.D,
    I: Boolean = P.I,
    Z: Boolean = P.Z,
    C: Boolean = P.C
  ) = copy(
    A = A,
    X = X,
    Y = Y,
    S = S,
    P = Flags(N = N, V = V, D = D, I = I, Z = Z, C = C)
  )
}

data class Flags(
  val N: Boolean = false,
  val V: Boolean = false,
  val D: Boolean = false,
  val I: Boolean = false,
  val Z: Boolean = false,
  val C: Boolean = false
) {
  override fun toString() = "%c%c--%c%c%c%c".format(
    if (N) 'N' else '-',
    if (V) 'V' else '-',
    if (D) 'D' else '-',
    if (I) 'I' else '-',
    if (Z) 'Z' else '-',
    if (C) 'C' else '-'
  )

  fun u8() = (
    (if (N) 0x80 else 0) or
    (if (V) 0x40 else 0) or
    (if (D) 0x08 else 0) or
    (if (I) 0x04 else 0) or
    (if (Z) 0x02 else 0) or
    (if (C) 0x01 else 0)
  ).u8()
}

fun UInt8.toFlags() = Flags(
  N = !(this and 0x80u).isZero(),
  V = !(this and 0x40u).isZero(),
  D = !(this and 0x08u).isZero(),
  I = !(this and 0x84u).isZero(),
  Z = !(this and 0x82u).isZero(),
  C = !(this and 0x81u).isZero()
)
