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

  @Test
  fun asl() {
    forOpcode(Alu::asl) {
      assertEquals(s.copy(A = 0x2Au, Z = false, N = false, C = false), s.copy(A = 0x15u))
      assertEquals(s.copy(A = 0xAAu, Z = false, N = true, C = false), s.copy(A = 0x55u))
      assertEquals(s.copy(A = 0x20u, Z = false, N = false, C = true), s.copy(A = 0x90u))
      assertEquals(s.copy(A = 0x80u, Z = false, N = true, C = true), s.copy(A = 0xC0u))
      assertEquals(s.copy(A = 0x00u, Z = true, N = false, C = true), s.copy(A = 0x80u))
    }
  }

  @Test
  fun lsr() {
    forOpcode(Alu::lsr) {
      assertEquals(s.copy(A = 0x55u, Z = false, N = false, C = false), s.copy(A = 0xAAu))
      assertEquals(s.copy(A = 0x01u, Z = false, N = false, C = true), s.copy(A = 0x03u))
      assertEquals(s.copy(A = 0x00u, Z = true, N = false, C = true), s.copy(A = 0x01u))
    }
  }

  @Test
  fun rol() {
    forOpcode(Alu::rol) {
      assertEquals(s.copy(A = 0x2Au, Z = false, N = false, C = false), s.copy(A = 0x15u, C = false))
      assertEquals(s.copy(A = 0x2Bu, Z = false, N = false, C = false), s.copy(A = 0x15u, C = true))
      assertEquals(s.copy(A = 0xAAu, Z = false, N = true, C = false), s.copy(A = 0x55u, C = false))
      assertEquals(s.copy(A = 0x20u, Z = false, N = false, C = true), s.copy(A = 0x90u, C = false))
      assertEquals(s.copy(A = 0x80u, Z = false, N = true, C = true), s.copy(A = 0xC0u, C = false))
      assertEquals(s.copy(A = 0x00u, Z = true, N = false, C = true), s.copy(A = 0x80u, C = false))
    }
  }

  @Test
  fun ror() {
    forOpcode(Alu::ror) {
      assertEquals(s.copy(A = 0x55u, Z = false, N = false, C = false), s.copy(A = 0xAAu, C = false))
      assertEquals(s.copy(A = 0xD5u, Z = false, N = true, C = false), s.copy(A = 0xAAu, C = true))
      assertEquals(s.copy(A = 0x01u, Z = false, N = false, C = true), s.copy(A = 0x03u, C = false))
      assertEquals(s.copy(A = 0x00u, Z = true, N = false, C = true), s.copy(A = 0x01u, C = false))
    }
  }
}
