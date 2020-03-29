package choliver.sixfiveohtwo.alu

import choliver.sixfiveohtwo.State
import choliver.sixfiveohtwo.alu.Flag.*
import org.junit.jupiter.api.Test


class ArithmeticTest {
  @Test
  fun adc() {
    withInvariants(I, D) {
      assertEquals(State(A = 0x60u, V = false, C = false, N = false), State(A = 0x50u)) { alu.adc(it, 0x10u) }
      assertEquals(State(A = 0xE0u, V = false, C = false, N = true), State(A = 0x50u)) { alu.adc(it, 0x90u) }
      assertEquals(State(A = 0x20u, V = false, C = true, N = false), State(A = 0x50u)) { alu.adc(it, 0xD0u) }
      assertEquals(State(A = 0xA0u, V = false, C = true, N = true), State(A = 0xD0u)) { alu.adc(it, 0xD0u) }
      // {V = true, C = false, N = false} not possible
      assertEquals(State(A = 0xA0u, V = true, C = false, N = true), State(A = 0x50u)) { alu.adc(it, 0x50u) }
      assertEquals(State(A = 0x60u, V = true, C = true, N = false), State(A = 0xD0u)) { alu.adc(it, 0x90u) }
      // {V = true, C = true, N = true} not possible
      assertEquals(State(A = 0x00u, V = false, C = true, N = false, Z = true), State(A = 0x01u)) { alu.adc(it, 0xFFu) }
    }
  }

  @Test
  fun dex() {
    withInvariants(I, D, C, V) {
      assertEquals(State(X = 0x01u, Z = false, N = false), State(X = 0x02u)) { alu.dex(it) }
      assertEquals(State(X = 0x00u, Z = true, N = false), State(X = 0x01u)) { alu.dex(it) }
      assertEquals(State(X = 0xFEu, Z = false, N = true), State(X = 0xFFu)) { alu.dex(it) }
    }
  }

  @Test
  fun dey() {
    withInvariants(I, D, C, V) {
      assertEquals(State(Y = 0x01u, Z = false, N = false), State(Y = 0x02u)) { alu.dey(it) }
      assertEquals(State(Y = 0x00u, Z = true, N = false), State(Y = 0x01u)) { alu.dey(it) }
      assertEquals(State(Y = 0xFEu, Z = false, N = true), State(Y = 0xFFu)) { alu.dey(it) }
    }
  }

  @Test
  fun inx() {
    withInvariants(I, D, C, V) {
      assertEquals(State(X = 0x02u, Z = false, N = false), State(X = 0x01u)) { alu.inx(it) }
      assertEquals(State(X = 0x00u, Z = true, N = false), State(X = 0xFFu)) { alu.inx(it) }
      assertEquals(State(X = 0xFFu, Z = false, N = true), State(X = 0xFEu)) { alu.inx(it) }
    }
  }

  @Test
  fun iny() {
    withInvariants(I, D, C, V) {
      assertEquals(State(Y = 0x02u, Z = false, N = false), State(Y = 0x01u)) { alu.iny(it) }
      assertEquals(State(Y = 0x00u, Z = true, N = false), State(Y = 0xFFu)) { alu.iny(it) }
      assertEquals(State(Y = 0xFFu, Z = false, N = true), State(Y = 0xFEu)) { alu.iny(it) }
    }
  }
}
