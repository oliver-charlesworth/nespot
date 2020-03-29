package choliver.sixfiveohtwo.alu

import choliver.sixfiveohtwo.State
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource


class ArithmeticTest {


  // Each case implemented twice to demonstrate flag setting respects carry in:
  // (1) basic, (2) carry-in set with (operand + 1)
  @ParameterizedTest(name = "carry = {0}")
  @ValueSource(booleans = [_0, _1])
  fun adc(carry: Boolean) {
    fun State.adjust() = if (carry) copy(A = (A - 1u).toUByte(), C = _1) else copy(C = _0)

    forOpcode(Alu::adc) {
      // +ve + +ve -> +ve
      assertEquals(s.copy(A = 0x60u, V = _0, C = _0, N = _0, Z = _0), s.copy(A = 0x50u).adjust(), 0x10u)

      // -ve + -ve => -ve
      assertEquals(s.copy(A = 0xE0u, V = _0, C = _0, N = _1, Z = _0), s.copy(A = 0x50u).adjust(), 0x90u)

      // Unsigned carry out
      assertEquals(s.copy(A = 0x20u, V = _0, C = _1, N = _0, Z = _0), s.copy(A = 0x50u).adjust(), 0xD0u)

      // Unsigned carry out, result is -ve
      assertEquals(s.copy(A = 0xA0u, V = _0, C = _1, N = _1, Z = _0), s.copy(A = 0xD0u).adjust(), 0xD0u)

      // +ve + +ve -> -ve (overflow)
      assertEquals(s.copy(A = 0xA0u, V = _1, C = _0, N = _1, Z = _0), s.copy(A = 0x50u).adjust(), 0x50u)

      // -ve + -ve -> +ve (overflow)
      assertEquals(s.copy(A = 0x60u, V = _1, C = _1, N = _0, Z = _0), s.copy(A = 0xD0u).adjust(), 0x90u)

      // Result is zero
      assertEquals(s.copy(A = 0x00u, V = _0, C = _1, N = _0, Z = _1), s.copy(A = 0x01u).adjust(), 0xFFu)
    }
  }

  // Each case implemented twice to demonstrate flag setting respects borrow in:
  // (1) basic, (2) borrow-in set with (operand + 1)
  @ParameterizedTest(name = "borrow = {0}")
  @ValueSource(booleans = [_0, _1])
  fun sbc(borrow: Boolean) {
    fun State.adjust() = if (borrow) copy(A = (A + 1u).toUByte(), C = _0) else copy(C = _1)

    forOpcode(Alu::sbc) {
      // +ve - -ve -> +ve
      assertEquals(s.copy(A = 0x60u, V = _0, C = _0, N = _0, Z = _0), s.copy(A = 0x50u).adjust(), 0xF0u)

      // -ve - +ve => -ve
      assertEquals(s.copy(A = 0xE0u, V = _0, C = _0, N = _1, Z = _0), s.copy(A = 0x50u).adjust(), 0x70u)

      // Unsigned carry out
      assertEquals(s.copy(A = 0x20u, V = _0, C = _1, N = _0, Z = _0), s.copy(A = 0x50u).adjust(), 0x30u)

      // Unsigned carry out, result is -ve
      assertEquals(s.copy(A = 0xA0u, V = _0, C = _1, N = _1, Z = _0), s.copy(A = 0xD0u).adjust(), 0x30u)

      // +ve - -ve -> -ve (overflow)
      assertEquals(s.copy(A = 0xA0u, V = _1, C = _0, N = _1, Z = _0), s.copy(A = 0x50u).adjust(), 0xB0u)

      // -ve - +ve -> +ve (overflow)
      assertEquals(s.copy(A = 0x60u, V = _1, C = _1, N = _0, Z = _0), s.copy(A = 0xD0u).adjust(), 0x70u)

      // Result is zero
      assertEquals(s.copy(A = 0x00u, V = _0, C = _1, N = _0, Z = _1), s.copy(A = 0x01u).adjust(), 0x01u)
    }
  }

  @Test
  fun dex() {
    forOpcode(Alu::dex) {
      assertEquals(s.copy(X = 0x01u, Z = _0, N = _0), s.copy(X = 0x02u))
      assertEquals(s.copy(X = 0x00u, Z = _1, N = _0), s.copy(X = 0x01u))
      assertEquals(s.copy(X = 0xFEu, Z = _0, N = _1), s.copy(X = 0xFFu))
    }
  }

  @Test
  fun dey() {
    forOpcode(Alu::dey) {
      assertEquals(s.copy(Y = 0x01u, Z = _0, N = _0), s.copy(Y = 0x02u))
      assertEquals(s.copy(Y = 0x00u, Z = _1, N = _0), s.copy(Y = 0x01u))
      assertEquals(s.copy(Y = 0xFEu, Z = _0, N = _1), s.copy(Y = 0xFFu))
    }
  }

  @Test
  fun inx() {
    forOpcode(Alu::inx) {
      assertEquals(s.copy(X = 0x02u, Z = _0, N = _0), s.copy(X = 0x01u))
      assertEquals(s.copy(X = 0x00u, Z = _1, N = _0), s.copy(X = 0xFFu))
      assertEquals(s.copy(X = 0xFFu, Z = _0, N = _1), s.copy(X = 0xFEu))
    }
  }

  @Test
  fun iny() {
    forOpcode(Alu::iny) {
      assertEquals(s.copy(Y = 0x02u, Z = _0, N = _0), s.copy(Y = 0x01u))
      assertEquals(s.copy(Y = 0x00u, Z = _1, N = _0), s.copy(Y = 0xFFu))
      assertEquals(s.copy(Y = 0xFFu, Z = _0, N = _1), s.copy(Y = 0xFEu))
    }
  }
}
