package cpu

import choliver.sixfiveohtwo.AddressMode.Immediate
import choliver.sixfiveohtwo.Opcode.*
import choliver.sixfiveohtwo.State
import choliver.sixfiveohtwo._0
import choliver.sixfiveohtwo._1
import forOpcode
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource

class ArithmeticTest {
  // TODO - address modes
  // TODO - DEC, INC

  // Each case implemented twice to demonstrate flag setting respects carry in:
  // (1) basic, (2) carry-in set with (operand + 1)
  @ParameterizedTest(name = "carry = {0}")
  @ValueSource(booleans = [_0, _1])
  fun adc(carry: Boolean) {
    fun State.adjust() = if (carry) with(A = (A - 1u).toUByte(), C = _1) else with(C = _0)

    forOpcode(ADC) {
      // +ve + +ve -> +ve
      assertEquals(
        s.with(A = 0x60u, V = _0, C = _0, N = _0, Z = _0),
        s.with(A = 0x50u).adjust(),
        Immediate(0x10u)
      )

      // -ve + -ve => -ve
      assertEquals(
        s.with(A = 0xE0u, V = _0, C = _0, N = _1, Z = _0),
        s.with(A = 0x50u).adjust(),
        Immediate(0x90u)
      )

      // Unsigned carry out
      assertEquals(
        s.with(A = 0x20u, V = _0, C = _1, N = _0, Z = _0),
        s.with(A = 0x50u).adjust(),
        Immediate(0xD0u)
      )

      // Unsigned carry out, result is -ve
      assertEquals(
        s.with(A = 0xA0u, V = _0, C = _1, N = _1, Z = _0),
        s.with(A = 0xD0u).adjust(),
        Immediate(0xD0u)
      )

      // +ve + +ve -> -ve (overflow)
      assertEquals(
        s.with(A = 0xA0u, V = _1, C = _0, N = _1, Z = _0),
        s.with(A = 0x50u).adjust(),
        Immediate(0x50u)
      )

      // -ve + -ve -> +ve (overflow)
      assertEquals(
        s.with(A = 0x60u, V = _1, C = _1, N = _0, Z = _0),
        s.with(A = 0xD0u).adjust(),
        Immediate(0x90u)
      )

      // Result is zero
      assertEquals(
        s.with(A = 0x00u, V = _0, C = _1, N = _0, Z = _1),
        s.with(A = 0x01u).adjust(),
        Immediate(0xFFu)
      )
    }
  }

  // Each case implemented twice to demonstrate flag setting respects borrow in:
  // (1) basic, (2) borrow-in set with (operand + 1)
  @ParameterizedTest(name = "borrow = {0}")
  @ValueSource(booleans = [_0, _1])
  fun sbc(borrow: Boolean) {
    fun State.adjust() = if (borrow) with(A = (A + 1u).toUByte(), C = _0) else with(C = _1)

    forOpcode(SBC) {
      // +ve - -ve -> +ve
      assertEquals(
        s.with(A = 0x60u, V = _0, C = _0, N = _0, Z = _0),
        s.with(A = 0x50u).adjust(),
        Immediate(0xF0u)
      )

      // -ve - +ve => -ve
      assertEquals(
        s.with(A = 0xE0u, V = _0, C = _0, N = _1, Z = _0),
        s.with(A = 0x50u).adjust(),
        Immediate(0x70u)
      )

      // Unsigned carry out
      assertEquals(
        s.with(A = 0x20u, V = _0, C = _1, N = _0, Z = _0),
        s.with(A = 0x50u).adjust(),
        Immediate(0x30u)
      )

      // Unsigned carry out, result is -ve
      assertEquals(
        s.with(A = 0xA0u, V = _0, C = _1, N = _1, Z = _0),
        s.with(A = 0xD0u).adjust(),
        Immediate(0x30u)
      )

      // +ve - -ve -> -ve (overflow)
      assertEquals(
        s.with(A = 0xA0u, V = _1, C = _0, N = _1, Z = _0),
        s.with(A = 0x50u).adjust(),
        Immediate(0xB0u)
      )

      // -ve - +ve -> +ve (overflow)
      assertEquals(
        s.with(A = 0x60u, V = _1, C = _1, N = _0, Z = _0),
        s.with(A = 0xD0u).adjust(),
        Immediate(0x70u)
      )

      // Result is zero
      assertEquals(
        s.with(A = 0x00u, V = _0, C = _1, N = _0, Z = _1),
        s.with(A = 0x01u).adjust(),
        Immediate(0x01u)
      )
    }
  }

  @Test
  fun dex() {
    forOpcode(DEX) {
      assertEquals(s.with(X = 0x01u, Z = _0, N = _0), s.with(X = 0x02u))
      assertEquals(s.with(X = 0x00u, Z = _1, N = _0), s.with(X = 0x01u))
      assertEquals(s.with(X = 0xFEu, Z = _0, N = _1), s.with(X = 0xFFu))
    }
  }

  @Test
  fun dey() {
    forOpcode(DEY) {
      assertEquals(s.with(Y = 0x01u, Z = _0, N = _0), s.with(Y = 0x02u))
      assertEquals(s.with(Y = 0x00u, Z = _1, N = _0), s.with(Y = 0x01u))
      assertEquals(s.with(Y = 0xFEu, Z = _0, N = _1), s.with(Y = 0xFFu))
    }
  }

  @Test
  fun inx() {
    forOpcode(INX) {
      assertEquals(s.with(X = 0x02u, Z = _0, N = _0), s.with(X = 0x01u))
      assertEquals(s.with(X = 0x00u, Z = _1, N = _0), s.with(X = 0xFFu))
      assertEquals(s.with(X = 0xFFu, Z = _0, N = _1), s.with(X = 0xFEu))
    }
  }

  @Test
  fun iny() {
    forOpcode(INY) {
      assertEquals(s.with(Y = 0x02u, Z = _0, N = _0), s.with(Y = 0x01u))
      assertEquals(s.with(Y = 0x00u, Z = _1, N = _0), s.with(Y = 0xFFu))
      assertEquals(s.with(Y = 0xFFu, Z = _0, N = _1), s.with(Y = 0xFEu))
    }
  }
}
