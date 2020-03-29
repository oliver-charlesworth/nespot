package choliver.sixfiveohtwo.alu

import org.junit.jupiter.api.Test


class ArithmeticTest {
  @Test
  fun adc() {
    forOpcode(Alu::adc) {
      assertEquals(s.copy(A = 0x60u, V = false, C = false, N = false, Z = false), s.copy(A = 0x50u), 0x10u)
      assertEquals(s.copy(A = 0xE0u, V = false, C = false, N = true, Z = false), s.copy(A = 0x50u), 0x90u)
      assertEquals(s.copy(A = 0x20u, V = false, C = true, N = false, Z = false), s.copy(A = 0x50u), 0xD0u)
      assertEquals(s.copy(A = 0xA0u, V = false, C = true, N = true, Z = false), s.copy(A = 0xD0u), 0xD0u)
      // {V = true, C = false, N = false} not possible
      assertEquals(s.copy(A = 0xA0u, V = true, C = false, N = true, Z = false), s.copy(A = 0x50u), 0x50u)
      assertEquals(s.copy(A = 0x60u, V = true, C = true, N = false, Z = false), s.copy(A = 0xD0u), 0x90u)
      // {V = true, C = true, N = true} not possible
      assertEquals(s.copy(A = 0x00u, V = false, C = true, N = false, Z = true), s.copy(A = 0x01u), 0xFFu)
    }
  }

  @Test
  fun dex() {
    forOpcode(Alu::dex) {
      assertEquals(s.copy(X = 0x01u, Z = false, N = false), s.copy(X = 0x02u))
      assertEquals(s.copy(X = 0x00u, Z = true, N = false), s.copy(X = 0x01u))
      assertEquals(s.copy(X = 0xFEu, Z = false, N = true), s.copy(X = 0xFFu))
    }
  }

  @Test
  fun dey() {
    forOpcode(Alu::dey) {
      assertEquals(s.copy(Y = 0x01u, Z = false, N = false), s.copy(Y = 0x02u))
      assertEquals(s.copy(Y = 0x00u, Z = true, N = false), s.copy(Y = 0x01u))
      assertEquals(s.copy(Y = 0xFEu, Z = false, N = true), s.copy(Y = 0xFFu))
    }
  }

  @Test
  fun inx() {
    forOpcode(Alu::inx) {
      assertEquals(s.copy(X = 0x02u, Z = false, N = false), s.copy(X = 0x01u))
      assertEquals(s.copy(X = 0x00u, Z = true, N = false), s.copy(X = 0xFFu))
      assertEquals(s.copy(X = 0xFFu, Z = false, N = true), s.copy(X = 0xFEu))
    }
  }

  @Test
  fun iny() {
    forOpcode(Alu::iny) {
      assertEquals(s.copy(Y = 0x02u, Z = false, N = false), s.copy(Y = 0x01u))
      assertEquals(s.copy(Y = 0x00u, Z = true, N = false), s.copy(Y = 0xFFu))
      assertEquals(s.copy(Y = 0xFFu, Z = false, N = true), s.copy(Y = 0xFEu))
    }
  }
}
