package choliver.nes.sixfiveohtwo

import choliver.nes.Data
import choliver.nes.data
import choliver.nes.isBitSet
import choliver.nes.isNeg

class Alu {
  data class Output(
    val q: Data = 0,
    val c: Boolean = false,
    val v: Boolean = false
  )

  // TODO - decimal mode
  fun adc(a: Data, b: Data, c: Boolean, d: Boolean): Output {
    if (d) TODO("Decimal mode not implemented")
    val raw = a + b + c.toInt()
    val result = raw.data()
    val sameOperandSigns = (a.isNeg() == b.isNeg())
    val differentResultSign = (a.isNeg() != result.isNeg())
    return Output(
      q = result,
      c = raw.isBitSet(8),
      v = sameOperandSigns && differentResultSign
    )
  }

  fun asl(q: Data) = Output(
    q = (q shl 1).data(),
    c = q.isBitSet(7)
  )

  fun lsr(q: Data) = Output(
    q = (q shr 1).data(),
    c = q.isBitSet(0)
  )

  fun rol(q: Data, c: Boolean) = Output(
    q = (q shl 1).data() + c.toInt(),
    c = q.isBitSet(7)
  )

  fun ror(q: Data, c: Boolean) = Output(
    q = (q shr 1).data() + (c.toInt() shl 7),
    c = q.isBitSet(0)
  )

  private fun Boolean.toInt() = if (this) 1 else 0
}
