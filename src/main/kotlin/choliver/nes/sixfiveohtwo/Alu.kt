package choliver.nes.sixfiveohtwo

import choliver.nes.sixfiveohtwo.model.UInt8
import choliver.nes.sixfiveohtwo.model.isNegative
import choliver.nes.sixfiveohtwo.model.isZero
import choliver.nes.sixfiveohtwo.model.u8

class Alu {

  data class Output(
    val q: UInt8 = 0u,
    val c: Boolean = false,
    val v: Boolean = false
  )

  // TODO - decimal mode
  fun adc(a: UInt8, b: UInt8, c: Boolean, d: Boolean): Output {
    if (d) TODO("Decimal mode not implemented")
    val raw = a + b + if (c) 1u else 0u
    val result = raw.u8()
    val sameOperandSigns = (a.isNegative() == b.isNegative())
    val differentResultSign = (a.isNegative() != result.isNegative())
    return Output(
      q = result,
      c = (raw and 0x100u) != 0u,
      v = sameOperandSigns && differentResultSign
    )
  }

  fun asl(q: UInt8) = Output(
    q = (q * 2u).u8(),
    c = !(q and 0x80u).isZero()
  )

  fun lsr(q: UInt8) = Output(
    q = (q / 2u).u8(),
    c = !(q and 0x01u).isZero()
  )

  fun rol(q: UInt8, c: Boolean) = Output(
    q = (q * 2u or if (c) 1u else 0u).u8(),
    c = !(q and 0x80u).isZero()
  )

  fun ror(q: UInt8, c: Boolean) = Output(
    q = (q / 2u or if (c) 0x80u else 0u).u8(),
    c = !(q and 0x01u).isZero()
  )
}
