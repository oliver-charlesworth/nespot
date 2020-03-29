package choliver.sixfiveohtwo.alu

import choliver.sixfiveohtwo.State

class Alu {
  fun adc(state: State, operand: UByte): State {
    val raw = state.A + operand
    val result = raw.toUByte()
    return state.copy(
      A = result,
      C = (raw and 0x100u) != 0u,
      V = (state.A.isNegative() == operand.isNegative()) && (state.A.isNegative() != result.isNegative())
    ).withZNFromA()
  }

  fun and(state: State, operand: UByte) = state.copy(
    A = state.A and operand
  ).withZNFromA()

  fun asl(state: State, operand: UByte): State = TODO()
  fun bcc(state: State): State = TODO()
  fun bcs(state: State): State = TODO()
  fun beq(state: State): State = TODO()
  fun bit(state: State, operand: UByte): State = TODO()
  fun bmi(state: State): State = TODO()
  fun bne(state: State): State = TODO()
  fun bpl(state: State): State = TODO()
  fun brk(state: State): State = TODO()
  fun bvc(state: State): State = TODO()
  fun bvs(state: State): State = TODO()
  fun clc(state: State): State = TODO()
  fun cld(state: State): State = TODO()
  fun cli(state: State): State = TODO()
  fun clv(state: State): State = TODO()
  fun cmp(state: State, operand: UByte): State = TODO()
  fun cpx(state: State, operand: UByte): State = TODO()
  fun cpy(state: State, operand: UByte): State = TODO()
  fun dec(state: State, operand: UByte): State = TODO()
  fun dex(state: State): State = TODO()
  fun dey(state: State): State = TODO()

  fun eor(state: State, operand: UByte) = state.copy(
    A = state.A xor operand
  ).withZNFromA()

  fun inc(state: State, operand: UByte): State = TODO()
  fun inx(state: State): State = TODO()
  fun iny(state: State): State = TODO()
  fun jmp(state: State, operand: UByte): State = TODO()
  fun jsr(state: State, operand: UByte): State = TODO()
  fun lda(state: State, operand: UByte): State = TODO()
  fun ldx(state: State, operand: UByte): State = TODO()
  fun ldy(state: State, operand: UByte): State = TODO()
  fun lsr(state: State, operand: UByte): State = TODO()
  fun nop(state: State): State = TODO()

  fun ora(state: State, operand: UByte) = state.copy(
    A = state.A or operand
  ).withZNFromA()

  fun pha(state: State): State = TODO()
  fun php(state: State): State = TODO()
  fun pla(state: State): State = TODO()
  fun plp(state: State): State = TODO()
  fun rol(state: State, operand: UByte): State = TODO()
  fun ror(state: State, operand: UByte): State = TODO()
  fun rti(state: State): State = TODO()
  fun rts(state: State): State = TODO()
  fun sbc(state: State, operand: UByte): State = TODO()
  fun sec(state: State): State = TODO()
  fun sed(state: State): State = TODO()
  fun sei(state: State): State = TODO()
  fun sta(state: State): State = TODO()
  fun stx(state: State): State = TODO()
  fun sty(state: State): State = TODO()

  fun tax(state: State) = state.copy(
    X = state.A
  ).withZNFromX()

  fun tay(state: State) = state.copy(
    Y = state.A
  ).withZNFromY()

  fun tsx(state: State) = state.copy(
    X = state.S
  )

  fun txa(state: State) = state.copy(
    A = state.X
  ).withZNFromA()

  fun txs(state: State) = state.copy(
    S = state.X
  )

  fun tya(state: State) = state.copy(
    A = state.Y
  ).withZNFromA()

  private fun State.withZNFromA() = withZNFrom(A)
  private fun State.withZNFromX() = withZNFrom(X)
  private fun State.withZNFromY() = withZNFrom(Y)
  private fun State.withZNFrom(s: UByte) = copy(Z = s.isZero(), N = s.isNegative())

  private fun UByte.isZero() = this == 0.toUByte()
  private fun UByte.isNegative() = this >= 0x80u
}
