package choliver.sixfiveohtwo.alu

import choliver.sixfiveohtwo.State
import choliver.sixfiveohtwo.alu.Flag.*
import org.junit.jupiter.api.Test

class TransferTest {
  @Test
  fun tax() {
    withInvariants(I, D, C, V) {
      assertEquals(State(A = 0x11u, X = 0x11u, Z = false, N = false), State(A = 0x11u, X = 0x22u), alu::tax)
      assertEquals(State(A = 0x00u, X = 0x00u, Z = true, N = false), State(A = 0x00u, X = 0x22u), alu::tax)
      assertEquals(State(A = 0xFFu, X = 0xFFu, Z = false, N = true), State(A = 0xFFu, X = 0x22u), alu::tax)
    }
  }

  @Test
  fun tay() {
    withInvariants(I, D, C, V) {
      assertEquals(State(A = 0x11u, Y = 0x11u, Z = false, N = false), State(A = 0x11u, Y = 0x22u), alu::tay)
      assertEquals(State(A = 0x00u, Y = 0x00u, Z = true, N = false), State(A = 0x00u, Y = 0x22u), alu::tay)
      assertEquals(State(A = 0xFFu, Y = 0xFFu, Z = false, N = true), State(A = 0xFFu, Y = 0x22u), alu::tay)
    }
  }

  @Test
  fun tsx() {
    withInvariants(I, D, C, V) {
      assertEquals(State(S = 0x11u, X = 0x11u, Z = false, N = false), State(S = 0x11u, X = 0x22u), alu::tsx)
      assertEquals(State(S = 0x00u, X = 0x00u, Z = true, N = false), State(S = 0x00u, X = 0x22u), alu::tsx)
      assertEquals(State(S = 0xFFu, X = 0xFFu, Z = false, N = true), State(S = 0xFFu, X = 0x22u), alu::tsx)
    }
  }

  @Test
  fun txa() {
    withInvariants(I, D, C, V) {
      assertEquals(State(A = 0x11u, X = 0x11u, Z = false, N = false), State(A = 0x22u, X = 0x11u), alu::txa)
      assertEquals(State(A = 0x00u, X = 0x00u, Z = true, N = false), State(A = 0x22u, X = 0x00u), alu::txa)
      assertEquals(State(A = 0xFFu, X = 0xFFu, Z = false, N = true), State(A = 0x22u, X = 0xFFu), alu::txa)
    }
  }

  @Test
  fun txs() {
    // Note - doesn't affect ZN
    withInvariants(I, D, C, V, Z, N) {
      assertEquals(State(S = 0x11u, X = 0x11u), State(S = 0x22u, X = 0x11u), alu::txs)
      assertEquals(State(S = 0x00u, X = 0x00u), State(S = 0x22u, X = 0x00u), alu::txs)
      assertEquals(State(S = 0xFFu, X = 0xFFu), State(S = 0x22u, X = 0xFFu), alu::txs)
    }
  }

  @Test
  fun tya() {
    withInvariants(I, D, C, V) {
      assertEquals(State(A = 0x11u, Y = 0x11u, Z = false, N = false), State(A = 0x22u, Y = 0x11u), alu::tya)
      assertEquals(State(A = 0x00u, Y = 0x00u, Z = true, N = false), State(A = 0x22u, Y = 0x00u), alu::tya)
      assertEquals(State(A = 0xFFu, Y = 0xFFu, Z = false, N = true), State(A = 0x22u, Y = 0xFFu), alu::tya)
    }
  }
}
