package choliver.sixfiveohtwo.alu

import org.junit.jupiter.api.Test

class TransferTest {
  @Test
  fun tax() {
    forOpcode(Alu::tax) {
      assertEquals(s.copy(A = 0x11u, X = 0x11u, Z = false, N = false), s.copy(A = 0x11u, X = 0x22u))
      assertEquals(s.copy(A = 0x00u, X = 0x00u, Z = true, N = false), s.copy(A = 0x00u, X = 0x22u))
      assertEquals(s.copy(A = 0xFFu, X = 0xFFu, Z = false, N = true), s.copy(A = 0xFFu, X = 0x22u))
    }
  }

  @Test
  fun tay() {
    forOpcode(Alu::tay) {
      assertEquals(s.copy(A = 0x11u, Y = 0x11u, Z = false, N = false), s.copy(A = 0x11u, Y = 0x22u))
      assertEquals(s.copy(A = 0x00u, Y = 0x00u, Z = true, N = false), s.copy(A = 0x00u, Y = 0x22u))
      assertEquals(s.copy(A = 0xFFu, Y = 0xFFu, Z = false, N = true), s.copy(A = 0xFFu, Y = 0x22u))
    }
  }

  @Test
  fun tsx() {
    forOpcode(Alu::tsx) {
      assertEquals(s.copy(S = 0x11u, X = 0x11u, Z = false, N = false), s.copy(S = 0x11u, X = 0x22u))
      assertEquals(s.copy(S = 0x00u, X = 0x00u, Z = true, N = false), s.copy(S = 0x00u, X = 0x22u))
      assertEquals(s.copy(S = 0xFFu, X = 0xFFu, Z = false, N = true), s.copy(S = 0xFFu, X = 0x22u))
    }
  }

  @Test
  fun txa() {
    forOpcode(Alu::txa) {
      assertEquals(s.copy(A = 0x11u, X = 0x11u, Z = false, N = false), s.copy(A = 0x22u, X = 0x11u))
      assertEquals(s.copy(A = 0x00u, X = 0x00u, Z = true, N = false), s.copy(A = 0x22u, X = 0x00u))
      assertEquals(s.copy(A = 0xFFu, X = 0xFFu, Z = false, N = true), s.copy(A = 0x22u, X = 0xFFu))
    }
  }

  @Test
  fun txs() {
    // Note - doesn't affect ZN
    forOpcode(Alu::txs) {
      assertEquals(s.copy(S = 0x11u, X = 0x11u), s.copy(S = 0x22u, X = 0x11u))
      assertEquals(s.copy(S = 0x00u, X = 0x00u), s.copy(S = 0x22u, X = 0x00u))
      assertEquals(s.copy(S = 0xFFu, X = 0xFFu), s.copy(S = 0x22u, X = 0xFFu))
    }
  }

  @Test
  fun tya() {
    forOpcode(Alu::tya) {
      assertEquals(s.copy(A = 0x11u, Y = 0x11u, Z = false, N = false), s.copy(A = 0x22u, Y = 0x11u))
      assertEquals(s.copy(A = 0x00u, Y = 0x00u, Z = true, N = false), s.copy(A = 0x22u, Y = 0x00u))
      assertEquals(s.copy(A = 0xFFu, Y = 0xFFu, Z = false, N = true), s.copy(A = 0x22u, Y = 0xFFu))
    }
  }
}
