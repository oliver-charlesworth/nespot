package choliver.sixfiveohtwo.alu

import org.junit.jupiter.api.Test

class LoadStoreTest {
  @Test
  fun lda() {
    forOpcode(Alu::lda) {
      assertEquals(s.copy(A = 0x69u, Z = false, N = false), s.copy(), 0x69u)
      assertEquals(s.copy(A = 0x00u, Z = true, N = false), s.copy(), 0x00u)
      assertEquals(s.copy(A = 0x96u, Z = false, N = true), s.copy(), 0x96u)
    }
  }

  @Test
  fun ldx() {
    forOpcode(Alu::ldx) {
      assertEquals(s.copy(X = 0x69u, Z = false, N = false), s.copy(), 0x69u)
      assertEquals(s.copy(X = 0x00u, Z = true, N = false), s.copy(), 0x00u)
      assertEquals(s.copy(X = 0x96u, Z = false, N = true), s.copy(), 0x96u)
    }
  }

  @Test
  fun ldy() {
    forOpcode(Alu::ldy) {
      assertEquals(s.copy(Y = 0x69u, Z = false, N = false), s.copy(), 0x69u)
      assertEquals(s.copy(Y = 0x00u, Z = true, N = false), s.copy(), 0x00u)
      assertEquals(s.copy(Y = 0x96u, Z = false, N = true), s.copy(), 0x96u)
    }
  }
}
