package choliver.sixfiveohtwo

import choliver.sixfiveohtwo.AluMode.*

class Alu {

  data class Input(
    val a: UInt8 = 0u,
    val b: UInt8 = 0u,
    val c: Boolean = false,
    val d: Boolean = false
  )

  data class Output(
    val q: UInt8 = 0u,
    val c: Boolean = false,
    val v: Boolean = false
  )

  fun execute(mode: AluMode, inp: Input) = when (mode) {
    NOP -> Output(q = inp.b)
    ADC -> adc(inp)
    SBC -> adc(inp.copy(b = inp.b.inv()))
    DEC -> Output(q = (inp.b - 1u).u8())
    INC -> Output(q = (inp.b + 1u).u8())
    AND -> Output(q = (inp.a and inp.b))
    EOR -> Output(q = (inp.a xor inp.b))
    ORA -> Output(q = (inp.a or inp.b))
    ASL -> Output(
      q = (inp.a * 2u).u8(),
      c = !(inp.a and 0x80u).isZero()
    )
    LSR -> Output(
      q = (inp.a / 2u).u8(),
      c = !(inp.a and 0x01u).isZero()
    )
    ROL -> Output(
      q = (inp.a * 2u or if (inp.c) 1u else 0u).u8(),
      c = !(inp.a and 0x80u).isZero()
    )
    ROR -> Output(
      q = (inp.a / 2u or if (inp.c) 0x80u else 0u).u8(),
      c = !(inp.a and 0x01u).isZero()
    )
    BIT -> Output(
      q = (inp.a and inp.b),
      v = (inp.b and 0x40u) != 0.u8()
    )
  }

  // TODO - decimal mode
  private fun adc(inp: Input): Output {
    val raw = inp.a + inp.b + if (inp.c) 1u else 0u
    val result = raw.u8()
    val sameOperandSigns = (inp.a.isNegative() == inp.b.isNegative())
    val differentResultSign = (inp.a.isNegative() != result.isNegative())
    return Output(
      q = result,
      c = (raw and 0x100u) != 0u,
      v = sameOperandSigns && differentResultSign
    )
  }
}
