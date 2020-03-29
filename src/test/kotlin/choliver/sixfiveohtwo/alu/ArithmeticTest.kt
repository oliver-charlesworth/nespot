package choliver.sixfiveohtwo.alu

import choliver.sixfiveohtwo.State
import choliver.sixfiveohtwo.alu.Flag.*
import org.junit.jupiter.api.Test


class ArithmeticTest {
  @Test
  fun adc() {
    forOpcode(Alu::adc, I, D) {
      assertEquals(State(A = 0x60u, V = false, C = false, N = false), State(A = 0x50u), 0x10u)
      assertEquals(State(A = 0xE0u, V = false, C = false, N = true), State(A = 0x50u), 0x90u)
      assertEquals(State(A = 0x20u, V = false, C = true, N = false), State(A = 0x50u), 0xD0u)
      assertEquals(State(A = 0xA0u, V = false, C = true, N = true), State(A = 0xD0u), 0xD0u)
      // {V = true, C = false, N = false} not possible
      assertEquals(State(A = 0xA0u, V = true, C = false, N = true), State(A = 0x50u), 0x50u)
      assertEquals(State(A = 0x60u, V = true, C = true, N = false), State(A = 0xD0u), 0x90u)
      // {V = true, C = true, N = true} not possible
      assertEquals(State(A = 0x00u, V = false, C = true, N = false, Z = true), State(A = 0x01u), 0xFFu)
    }
  }

  @Test
  fun dex() {
    forOpcode(Alu::dex, I, D, C, V) {
      assertEquals(State(X = 0x01u, Z = false, N = false), State(X = 0x02u))
      assertEquals(State(X = 0x00u, Z = true, N = false), State(X = 0x01u))
      assertEquals(State(X = 0xFEu, Z = false, N = true), State(X = 0xFFu))
    }
  }

  @Test
  fun dey() {
    forOpcode(Alu::dey, I, D, C, V) {
      assertEquals(State(Y = 0x01u, Z = false, N = false), State(Y = 0x02u))
      assertEquals(State(Y = 0x00u, Z = true, N = false), State(Y = 0x01u))
      assertEquals(State(Y = 0xFEu, Z = false, N = true), State(Y = 0xFFu))
    }
  }

  @Test
  fun inx() {
    forOpcode(Alu::inx, I, D, C, V) {
      assertEquals(State(X = 0x02u, Z = false, N = false), State(X = 0x01u))
      assertEquals(State(X = 0x00u, Z = true, N = false), State(X = 0xFFu))
      assertEquals(State(X = 0xFFu, Z = false, N = true), State(X = 0xFEu))
    }
  }

  @Test
  fun iny() {
    forOpcode(Alu::iny, I, D, C, V) {
      assertEquals(State(Y = 0x02u, Z = false, N = false), State(Y = 0x01u))
      assertEquals(State(Y = 0x00u, Z = true, N = false), State(Y = 0xFFu))
      assertEquals(State(Y = 0xFFu, Z = false, N = true), State(Y = 0xFEu))
    }
  }
}
