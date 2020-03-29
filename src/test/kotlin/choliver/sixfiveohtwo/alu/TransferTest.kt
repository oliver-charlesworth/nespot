package choliver.sixfiveohtwo.alu

import choliver.sixfiveohtwo.State
import choliver.sixfiveohtwo.alu.Flag.*
import org.junit.jupiter.api.Test

class TransferTest {
  @Test
  fun tax() {
    forOpcode(Alu::tax, I, D, C, V) {
      assertEquals(State(A = 0x11u, X = 0x11u, Z = false, N = false), State(A = 0x11u, X = 0x22u))
      assertEquals(State(A = 0x00u, X = 0x00u, Z = true, N = false), State(A = 0x00u, X = 0x22u))
      assertEquals(State(A = 0xFFu, X = 0xFFu, Z = false, N = true), State(A = 0xFFu, X = 0x22u))
    }
  }

  @Test
  fun tay() {
    forOpcode(Alu::tay, I, D, C, V) {
      assertEquals(State(A = 0x11u, Y = 0x11u, Z = false, N = false), State(A = 0x11u, Y = 0x22u))
      assertEquals(State(A = 0x00u, Y = 0x00u, Z = true, N = false), State(A = 0x00u, Y = 0x22u))
      assertEquals(State(A = 0xFFu, Y = 0xFFu, Z = false, N = true), State(A = 0xFFu, Y = 0x22u))
    }
  }

  @Test
  fun tsx() {
    forOpcode(Alu::tsx, I, D, C, V) {
      assertEquals(State(S = 0x11u, X = 0x11u, Z = false, N = false), State(S = 0x11u, X = 0x22u))
      assertEquals(State(S = 0x00u, X = 0x00u, Z = true, N = false), State(S = 0x00u, X = 0x22u))
      assertEquals(State(S = 0xFFu, X = 0xFFu, Z = false, N = true), State(S = 0xFFu, X = 0x22u))
    }
  }

  @Test
  fun txa() {
    forOpcode(Alu::txa, I, D, C, V) {
      assertEquals(State(A = 0x11u, X = 0x11u, Z = false, N = false), State(A = 0x22u, X = 0x11u))
      assertEquals(State(A = 0x00u, X = 0x00u, Z = true, N = false), State(A = 0x22u, X = 0x00u))
      assertEquals(State(A = 0xFFu, X = 0xFFu, Z = false, N = true), State(A = 0x22u, X = 0xFFu))
    }
  }

  @Test
  fun txs() {
    // Note - doesn't affect ZN
    forOpcode(Alu::txs, I, D, C, V, Z, N) {
      assertEquals(State(S = 0x11u, X = 0x11u), State(S = 0x22u, X = 0x11u))
      assertEquals(State(S = 0x00u, X = 0x00u), State(S = 0x22u, X = 0x00u))
      assertEquals(State(S = 0xFFu, X = 0xFFu), State(S = 0x22u, X = 0xFFu))
    }
  }

  @Test
  fun tya() {
    forOpcode(Alu::tya, I, D, C, V) {
      assertEquals(State(A = 0x11u, Y = 0x11u, Z = false, N = false), State(A = 0x22u, Y = 0x11u))
      assertEquals(State(A = 0x00u, Y = 0x00u, Z = true, N = false), State(A = 0x22u, Y = 0x00u))
      assertEquals(State(A = 0xFFu, Y = 0xFFu, Z = false, N = true), State(A = 0x22u, Y = 0xFFu))
    }
  }
}
