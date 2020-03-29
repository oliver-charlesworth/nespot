package choliver.sixfiveohtwo.alu

import org.junit.jupiter.api.Test

class BitwiseTest {
  @Test
  fun and() {
    forOpcode(Alu::and) {
      assertEquals(s.copy(A = 0x01u, Z = false, N = false), s.copy(A = 0x11u), 0x23u)
      assertEquals(s.copy(A = 0x00u, Z = true, N = false), s.copy(A = 0x11u), 0x22u)
      assertEquals(s.copy(A = 0x81u, Z = false, N = true), s.copy(A = 0x81u), 0x83u)
    }
  }

  @Test
  fun ora() {
    forOpcode(Alu::ora) {
      assertEquals(s.copy(A = 0x33u, Z = false, N = false), s.copy(A = 0x11u), 0x23u)
      assertEquals(s.copy(A = 0x00u, Z = true, N = false), s.copy(A = 0x00u), 0x00u)
      assertEquals(s.copy(A = 0x83u, Z = false, N = true), s.copy(A = 0x81u), 0x83u)
    }
  }

  @Test
  fun eor() {
    forOpcode(Alu::eor) {
      assertEquals(s.copy(A = 0x32u, Z = false, N = false), s.copy(A = 0x11u), 0x23u)
      assertEquals(s.copy(A = 0x00u, Z = true, N = false), s.copy(A = 0x11u), 0x11u)
      assertEquals(s.copy(A = 0x82u, Z = false, N = true), s.copy(A = 0x81u), 0x03u)
    }
  }
}
