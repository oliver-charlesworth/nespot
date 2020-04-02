package cpu

import choliver.sixfiveohtwo.AddressMode.Immediate
import choliver.sixfiveohtwo.Opcode.*
import choliver.sixfiveohtwo.State
import choliver.sixfiveohtwo._0
import choliver.sixfiveohtwo._1
import enc
import forOpcode
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import sweepStates

class ArithmeticTest {
  // TODO - address modes
  // TODO - correct PC change
  // TODO - no memory writes
  // TODO - DEC, INC

  @Nested
  inner class Adc {
    // Each case implemented twice to demonstrate flag setting respects carry in:
    // (1) basic, (2) carry-in set with (operand + 1)
    @ParameterizedTest(name = "carry = {0}")
    @ValueSource(booleans = [_0, _1])
    fun sweepStates(carry: Boolean) {
      fun State.adjust() = if (carry) with(A = (A - 1u).toUByte(), C = _1) else with(C = _0)

      sweepStates {
        // +ve + +ve -> +ve
        assertEquals(
          s.with(A = 0x60u, V = _0, C = _0, N = _0, Z = _0),
          s.with(A = 0x50u).adjust(),
          enc(0x69, 0x10)
        )

        // -ve + -ve => -ve
        assertEquals(
          s.with(A = 0xE0u, V = _0, C = _0, N = _1, Z = _0),
          s.with(A = 0x50u).adjust(),
          enc(0x69, 0x90)
        )

        // Unsigned carry out
        assertEquals(
          s.with(A = 0x20u, V = _0, C = _1, N = _0, Z = _0),
          s.with(A = 0x50u).adjust(),
          enc(0x69, 0xD0)
        )

        // Unsigned carry out, result is -ve
        assertEquals(
          s.with(A = 0xA0u, V = _0, C = _1, N = _1, Z = _0),
          s.with(A = 0xD0u).adjust(),
          enc(0x69, 0xD0)
        )

        // +ve + +ve -> -ve (overflow)
        assertEquals(
          s.with(A = 0xA0u, V = _1, C = _0, N = _1, Z = _0),
          s.with(A = 0x50u).adjust(),
          enc(0x69, 0x50)
        )

        // -ve + -ve -> +ve (overflow)
        assertEquals(
          s.with(A = 0x60u, V = _1, C = _1, N = _0, Z = _0),
          s.with(A = 0xD0u).adjust(),
          enc(0x69, 0x90)
        )

        // Result is zero
        assertEquals(
          s.with(A = 0x00u, V = _0, C = _1, N = _0, Z = _1),
          s.with(A = 0x01u).adjust(),
          enc(0x69, 0xFF)
        )
      }
    }

    // TODO - address modes
  }

  @Nested
  inner class Sbc {
    // Each case implemented twice to demonstrate flag setting respects borrow in:
    // (1) basic, (2) borrow-in set with (operand + 1)
    @ParameterizedTest(name = "borrow = {0}")
    @ValueSource(booleans = [_0, _1])
    fun sweepStates(borrow: Boolean) {
      fun State.adjust() = if (borrow) with(A = (A + 1u).toUByte(), C = _0) else with(C = _1)

      sweepStates {
        // +ve - -ve -> +ve
        assertEquals(
          s.with(A = 0x60u, V = _0, C = _0, N = _0, Z = _0),
          s.with(A = 0x50u).adjust(),
          enc(0xE9, 0xF0)
        )

        // -ve - +ve => -ve
        assertEquals(
          s.with(A = 0xE0u, V = _0, C = _0, N = _1, Z = _0),
          s.with(A = 0x50u).adjust(),
          enc(0xE9, 0x70)
        )

        // Unsigned carry out
        assertEquals(
          s.with(A = 0x20u, V = _0, C = _1, N = _0, Z = _0),
          s.with(A = 0x50u).adjust(),
          enc(0xE9, 0x30)
        )

        // Unsigned carry out, result is -ve
        assertEquals(
          s.with(A = 0xA0u, V = _0, C = _1, N = _1, Z = _0),
          s.with(A = 0xD0u).adjust(),
          enc(0xE9, 0x30)
        )

        // +ve - -ve -> -ve (overflow)
        assertEquals(
          s.with(A = 0xA0u, V = _1, C = _0, N = _1, Z = _0),
          s.with(A = 0x50u).adjust(),
          enc(0xE9, 0xB0)
        )

        // -ve - +ve -> +ve (overflow)
        assertEquals(
          s.with(A = 0x60u, V = _1, C = _1, N = _0, Z = _0),
          s.with(A = 0xD0u).adjust(),
          enc(0xE9, 0x70)
        )

        // Result is zero
        assertEquals(
          s.with(A = 0x00u, V = _0, C = _1, N = _0, Z = _1),
          s.with(A = 0x01u).adjust(),
          enc(0xE9, 0x01)
        )
      }
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
