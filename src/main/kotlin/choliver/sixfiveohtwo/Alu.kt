package choliver.sixfiveohtwo

class Alu {

  data class Input(
    val a: UByte = 0u,
    val b: UByte = 0u,
    val c: Boolean = false,
    val d: Boolean = false
  )

  data class Output(
    val x: UByte = 0u,
    val c: Boolean = false,
    val v: Boolean = false
  )

  // TODO - decimal mode
  fun adc(inp: Input): Output {
    val raw = inp.a + inp.b + if (inp.c) 1u else 0u
    val result = raw.toUByte()
    val sameOperandSigns = (inp.a.isNegative() == inp.b.isNegative())
    val differentResultSign = (inp.a.isNegative() != result.isNegative())
    return Output(
      x = result,
      c = (raw and 0x100u) != 0u,
      v = sameOperandSigns && differentResultSign
    )
  }

  fun sbc(inp: Input) = adc(inp.copy(b = inp.b.inv()))

  fun dec(inp: Input) = Output(x = (inp.a - 1u).toUByte())

  fun inc(inp: Input) = Output(x = (inp.a + 1u).toUByte())

  fun and(inp: Input) = Output(x = (inp.a and inp.b).toUByte())

  fun eor(inp: Input) = Output(x = (inp.a xor inp.b).toUByte())

  fun ora(inp: Input) = Output(x = (inp.a or inp.b).toUByte())

  fun asl(inp: Input) = Output(
    x = (inp.a * 2u).toUByte(),
    c = !(inp.a and 0x80u).isZero()
  )

  fun lsr(inp: Input) = Output(
    x = (inp.a / 2u).toUByte(),
    c = !(inp.a and 0x01u).isZero()
  )

  fun rol(inp: Input) = Output(
    x = (inp.a * 2u or if (inp.c) 1u else 0u).toUByte(),
    c = !(inp.a and 0x80u).isZero()
  )

  fun ror(inp: Input) = Output(
    x = (inp.a / 2u or if (inp.c) 0x80u else 0u).toUByte(),
    c = !(inp.a and 0x01u).isZero()
  )

  private fun UByte.isZero() = this == 0.toUByte()
  private fun UByte.isNegative() = this >= 0x80u
}
