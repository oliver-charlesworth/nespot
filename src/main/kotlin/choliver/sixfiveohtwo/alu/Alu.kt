package choliver.sixfiveohtwo.alu

import choliver.sixfiveohtwo.State

class Alu {

  // --- Arithmetic --- //

  // TODO - decimal mode
  fun adc(state: State, operand: UByte): State {
    val raw = state.A + operand + if (state.C) 1u else 0u
    val result = raw.toUByte()
    val sameOperandSigns = (state.A.isNegative() == operand.isNegative())
    val differentResultSign = (state.A.isNegative() != result.isNegative())
    return state.withA(result).copy(
      C = (raw and 0x100u) != 0u,
      V = sameOperandSigns && differentResultSign
    )
  }

  fun sbc(state: State, operand: UByte) = adc(state, operand.inv())

  fun dec(state: State, operand: UByte): State = TODO()
  fun dex(state: State) = state.withX((state.X - 1u).toUByte())
  fun dey(state: State) = state.withY((state.Y - 1u).toUByte())

  fun inc(state: State, operand: UByte): State = TODO()
  fun inx(state: State) = state.withX((state.X + 1u).toUByte())
  fun iny(state: State) = state.withY((state.Y + 1u).toUByte())

  // --- Comparison --- //

  fun bit(state: State, operand: UByte): State = TODO()
  fun cmp(state: State, operand: UByte): State = TODO()
  fun cpx(state: State, operand: UByte): State = TODO()
  fun cpy(state: State, operand: UByte): State = TODO()

  // --- Bitwise --- //

  fun and(state: State, operand: UByte) = state.withA(state.A and operand)
  fun eor(state: State, operand: UByte) = state.withA(state.A xor operand)
  fun ora(state: State, operand: UByte) = state.withA(state.A or operand)

  // TODO - implement M version
  fun asl(state: State) = state
    .withA((state.A * 2u).toUByte())
    .copy(
      C = !(state.A and 0x80u).isZero()
    )

  // TODO - implement M version
  fun lsr(state: State) = state
    .withA((state.A / 2u).toUByte())
    .copy(
      C = !(state.A and 0x01u).isZero()
    )

  // TODO - implement M version
  fun rol(state: State) = state
    .withA((state.A * 2u or if (state.C) 1u else 0u).toUByte())
    .copy(
      C = !(state.A and 0x80u).isZero()
    )

  // TODO - implement M version
  fun ror(state: State) = state
    .withA((state.A / 2u or if (state.C) 0x80u else 0u).toUByte())
    .copy(
      C = !(state.A and 0x01u).isZero()
    )

  // --- Branch --- //

  fun bcc(state: State): State = TODO()
  fun bcs(state: State): State = TODO()
  fun beq(state: State): State = TODO()
  fun bmi(state: State): State = TODO()
  fun bne(state: State): State = TODO()
  fun bpl(state: State): State = TODO()
  fun bvc(state: State): State = TODO()
  fun bvs(state: State): State = TODO()
  fun jmp(state: State, operand: UByte): State = TODO()
  fun jsr(state: State, operand: UByte): State = TODO()
  fun rti(state: State): State = TODO()
  fun rts(state: State): State = TODO()

  // --- Modify flags --- //

  fun clc(state: State) = state.copy(C = false)
  fun cld(state: State) = state.copy(D = false)
  fun cli(state: State) = state.copy(I = false)
  fun clv(state: State) = state.copy(V = false)
  fun sec(state: State) = state.copy(C = true)
  fun sed(state: State) = state.copy(D = true)
  fun sei(state: State) = state.copy(I = true)

  // --- Load / store --- //

  fun lda(state: State, operand: UByte) = state.withA(operand)
  fun ldx(state: State, operand: UByte) = state.withX(operand)
  fun ldy(state: State, operand: UByte) = state.withY(operand)

  fun sta(state: State): State = TODO()
  fun stx(state: State): State = TODO()
  fun sty(state: State): State = TODO()

  // --- Register transfer --- //

  fun tax(state: State) = state.withX(state.A)
  fun tay(state: State) = state.withY(state.A)
  fun tsx(state: State) = state.withX(state.S)
  fun txa(state: State) = state.withA(state.X)
  fun txs(state: State) = state.withS(state.X)
  fun tya(state: State) = state.withA(state.Y)

  // --- Push / pull --- //

  fun pha(state: State): State = TODO()
  fun php(state: State): State = TODO()
  fun pla(state: State): State = TODO()
  fun plp(state: State): State = TODO()

  // --- Uncategorised --- //

  fun brk(state: State): State = TODO()
  fun nop(state: State): State = TODO()

  // --- Helpers --- //

  private fun State.withA(A: UByte) = copy(A = A).withZNFrom(A)
  private fun State.withX(X: UByte) = copy(X = X).withZNFrom(X)
  private fun State.withY(Y: UByte) = copy(Y = Y).withZNFrom(Y)
  private fun State.withS(S: UByte) = copy(S = S)

  private fun State.withZNFrom(s: UByte) = copy(Z = s.isZero(), N = s.isNegative())

  private fun UByte.isZero() = this == 0.toUByte()
  private fun UByte.isNegative() = this >= 0x80u
}
