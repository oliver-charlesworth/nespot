package choliver.nes.sixfiveohtwo.model

import choliver.nes.*

data class State(
  val PC: ProgramCounter = ProgramCounter(),
  val A: Data = 0x00,
  val X: Data = 0x00,
  val Y: Data = 0x00,
  val S: Data = 0x00,
  val P: Flags = Flags()
) {
  override fun toString() = "(PC=%s, S=0x%02X, A=0x%02X, X=0x%02X, Y=0x%02X, P=%s)".format(
    PC.toString(),
    S,
    A,
    X,
    Y,
    P.toString()
  )

  /** Like [copy], but allows us to also set individual status flags. */
  fun with(
    PC: ProgramCounter = this.PC,
    A: Data = this.A,
    X: Data = this.X,
    Y: Data = this.Y,
    S: Data = this.S,
    N: Boolean = P.N,
    V: Boolean = P.V,
    D: Boolean = P.D,
    I: Boolean = P.I,
    Z: Boolean = P.Z,
    C: Boolean = P.C
  ) = copy(
    PC = PC,
    A = A,
    X = X,
    Y = Y,
    S = S,
    P = Flags(N = N, V = V, D = D, I = I, Z = Z, C = C)
  )
}

data class ProgramCounter(
  val L: Data = 0x00,
  val H: Data = 0x00
) {
  override fun toString() = "0x%02X%02X".format(H, L)

  operator fun plus(rhs: Int) = (addr() + rhs).toPC()
  operator fun minus(rhs: Int) = (addr() - rhs).toPC()
  operator fun inc() = this + 1

  fun addr(): Address = L + (H shl 8)
}

fun Address.toPC() = ProgramCounter(L = lo(), H = hi())

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

  fun data(): Data = (
    (if (N) 0x80 else 0) or
    (if (V) 0x40 else 0) or
    (if (D) 0x08 else 0) or
    (if (I) 0x04 else 0) or
    (if (Z) 0x02 else 0) or
    (if (C) 0x01 else 0)
  )
}

fun Data.toFlags() = Flags(
  N = isBitSet(7),
  V = isBitSet(6),
  D = isBitSet(3),
  I = isBitSet(2),
  Z = isBitSet(1),
  C = isBitSet(0)
)
