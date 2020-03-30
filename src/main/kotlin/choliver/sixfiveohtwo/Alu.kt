package choliver.sixfiveohtwo

class Alu {

  data class Input(
    val a: UInt8 = 0u,
    val b: UInt8 = 0u,
    val c: Boolean = false,
    val d: Boolean = false
  )

  data class Output(
    val z: UInt8 = 0u,
    val c: Boolean = false,
    val v: Boolean = false
  )

  fun nop(inp: Input) = Output()

  // TODO - decimal mode
  fun adc(inp: Input): Output {
    val raw = inp.a + inp.b + if (inp.c) 1u else 0u
    val result = raw.toUInt8()
    val sameOperandSigns = (inp.a.isNegative() == inp.b.isNegative())
    val differentResultSign = (inp.a.isNegative() != result.isNegative())
    return Output(
      z = result,
      c = (raw and 0x100u) != 0u,
      v = sameOperandSigns && differentResultSign
    )
  }

  fun sbc(inp: Input) = adc(inp.copy(b = inp.b.inv()))

  fun dec(inp: Input) = Output(z = (inp.a - 1u).toUInt8())

  fun inc(inp: Input) = Output(z = (inp.a + 1u).toUInt8())

  fun and(inp: Input) = Output(z = (inp.a and inp.b))

  fun eor(inp: Input) = Output(z = (inp.a xor inp.b))

  fun ora(inp: Input) = Output(z = (inp.a or inp.b))

  fun asl(inp: Input) = Output(
    z = (inp.a * 2u).toUInt8(),
    c = !(inp.a and 0x80u).isZero()
  )

  fun lsr(inp: Input) = Output(
    z = (inp.a / 2u).toUInt8(),
    c = !(inp.a and 0x01u).isZero()
  )

  fun rol(inp: Input) = Output(
    z = (inp.a * 2u or if (inp.c) 1u else 0u).toUInt8(),
    c = !(inp.a and 0x80u).isZero()
  )

  fun ror(inp: Input) = Output(
    z = (inp.a / 2u or if (inp.c) 0x80u else 0u).toUInt8(),
    c = !(inp.a and 0x01u).isZero()
  )
}
