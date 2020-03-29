package choliver.sixfiveohtwo.alu

import org.junit.jupiter.api.Test

class BitwiseTest {
  @Test
  fun and() {
    forOpcode(Alu::and) {
      assertEquals(s.copy(A = 0x01u, Z = _0, N = _0), s.copy(A = 0x11u), 0x23u)
      assertEquals(s.copy(A = 0x00u, Z = _1, N = _0), s.copy(A = 0x11u), 0x22u)
      assertEquals(s.copy(A = 0x81u, Z = _0, N = _1), s.copy(A = 0x81u), 0x83u)
    }
  }

  @Test
  fun ora() {
    forOpcode(Alu::ora) {
      assertEquals(s.copy(A = 0x33u, Z = _0, N = _0), s.copy(A = 0x11u), 0x23u)
      assertEquals(s.copy(A = 0x00u, Z = _1, N = _0), s.copy(A = 0x00u), 0x00u)
      assertEquals(s.copy(A = 0x83u, Z = _0, N = _1), s.copy(A = 0x81u), 0x83u)
    }
  }

  @Test
  fun eor() {
    forOpcode(Alu::eor) {
      assertEquals(s.copy(A = 0x32u, Z = _0, N = _0), s.copy(A = 0x11u), 0x23u)
      assertEquals(s.copy(A = 0x00u, Z = _1, N = _0), s.copy(A = 0x11u), 0x11u)
      assertEquals(s.copy(A = 0x82u, Z = _0, N = _1), s.copy(A = 0x81u), 0x03u)
    }
  }

  @Test
  fun asl() {
    forOpcode(Alu::asl) {
      assertEquals(s.copy(A = 0x2Au, Z = _0, N = _0, C = _0), s.copy(A = 0x15u))
      assertEquals(s.copy(A = 0xAAu, Z = _0, N = _1, C = _0), s.copy(A = 0x55u))
      assertEquals(s.copy(A = 0x20u, Z = _0, N = _0, C = _1), s.copy(A = 0x90u))
      assertEquals(s.copy(A = 0x80u, Z = _0, N = _1, C = _1), s.copy(A = 0xC0u))
      assertEquals(s.copy(A = 0x00u, Z = _1, N = _0, C = _1), s.copy(A = 0x80u))
    }
  }

  @Test
  fun lsr() {
    forOpcode(Alu::lsr) {
      assertEquals(s.copy(A = 0x55u, Z = _0, N = _0, C = _0), s.copy(A = 0xAAu))
      assertEquals(s.copy(A = 0x01u, Z = _0, N = _0, C = _1), s.copy(A = 0x03u))
      assertEquals(s.copy(A = 0x00u, Z = _1, N = _0, C = _1), s.copy(A = 0x01u))
    }
  }

  @Test
  fun rol() {
    forOpcode(Alu::rol) {
      assertEquals(s.copy(A = 0x2Au, Z = _0, N = _0, C = _0), s.copy(A = 0x15u, C = _0))
      assertEquals(s.copy(A = 0x2Bu, Z = _0, N = _0, C = _0), s.copy(A = 0x15u, C = _1))
      assertEquals(s.copy(A = 0xAAu, Z = _0, N = _1, C = _0), s.copy(A = 0x55u, C = _0))
      assertEquals(s.copy(A = 0x20u, Z = _0, N = _0, C = _1), s.copy(A = 0x90u, C = _0))
      assertEquals(s.copy(A = 0x80u, Z = _0, N = _1, C = _1), s.copy(A = 0xC0u, C = _0))
      assertEquals(s.copy(A = 0x00u, Z = _1, N = _0, C = _1), s.copy(A = 0x80u, C = _0))
    }
  }

  @Test
  fun ror() {
    forOpcode(Alu::ror) {
      assertEquals(s.copy(A = 0x55u, Z = _0, N = _0, C = _0), s.copy(A = 0xAAu, C = _0))
      assertEquals(s.copy(A = 0xD5u, Z = _0, N = _1, C = _0), s.copy(A = 0xAAu, C = _1))
      assertEquals(s.copy(A = 0x01u, Z = _0, N = _0, C = _1), s.copy(A = 0x03u, C = _0))
      assertEquals(s.copy(A = 0x00u, Z = _1, N = _0, C = _1), s.copy(A = 0x01u, C = _0))
    }
  }
}
