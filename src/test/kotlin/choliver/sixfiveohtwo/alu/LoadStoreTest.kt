package choliver.sixfiveohtwo.alu

import choliver.sixfiveohtwo.State
import choliver.sixfiveohtwo.alu.Flag.*
import org.junit.jupiter.api.Test

class LoadStoreTest {
  @Test
  fun lda() {
    forOpcode(Alu::lda, I, D, C, V) {
      assertEquals(State(A = 0x69u, Z = false, N = false), State(), 0x69u)
      assertEquals(State(A = 0x00u, Z = true, N = false), State(), 0x00u)
      assertEquals(State(A = 0x96u, Z = false, N = true), State(), 0x96u)
    }
  }

  @Test
  fun ldx() {
    forOpcode(Alu::ldx, I, D, C, V) {
      assertEquals(State(X = 0x69u, Z = false, N = false), State(), 0x69u)
      assertEquals(State(X = 0x00u, Z = true, N = false), State(), 0x00u)
      assertEquals(State(X = 0x96u, Z = false, N = true), State(), 0x96u)
    }
  }

  @Test
  fun ldy() {
    forOpcode(Alu::ldy, I, D, C, V) {
      assertEquals(State(Y = 0x69u, Z = false, N = false), State(), 0x69u)
      assertEquals(State(Y = 0x00u, Z = true, N = false), State(), 0x00u)
      assertEquals(State(Y = 0x96u, Z = false, N = true), State(), 0x96u)
    }
  }
}
