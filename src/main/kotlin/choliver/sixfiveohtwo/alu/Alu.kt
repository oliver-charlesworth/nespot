package choliver.sixfiveohtwo.alu

import choliver.sixfiveohtwo.State

class Alu {
  fun adc(state: State, operand: UByte): State {
    val raw = state.A + operand
    val result = raw.toUByte()
    return state.copy(
      A = result,
      C = (raw and 0x100u) != 0u,
      Z = result.isZero(),
      V = (state.A.isNegative() == operand.isNegative()) && (state.A.isNegative() != result.isNegative()),
      N = result.isNegative()
    )
  }

  fun and(state: State, operand: UByte): State = TODO()
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
  fun eor(state: State, operand: UByte): State = TODO()
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
  fun ora(state: State, operand: UByte): State = TODO()
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
    X = state.A,
    Z = state.A.isZero(),
    N = state.A.isNegative()
  )

  fun tay(state: State) = state.copy(
    Y = state.A,
    Z = state.A.isZero(),
    N = state.A.isNegative()
  )

  fun tsx(state: State) = state.copy(
    X = state.S
  )

  fun txa(state: State) = state.copy(
    A = state.X,
    Z = state.X.isZero(),
    N = state.X.isNegative()
  )

  fun txs(state: State) = state.copy(
    S = state.X
  )

  fun tya(state: State) = state.copy(
    A = state.Y,
    Z = state.Y.isZero(),
    N = state.Y.isNegative()
  )

  private fun UByte.isZero() = this == 0.toUByte()
  private fun UByte.isNegative() = this >= 0x80u
}
