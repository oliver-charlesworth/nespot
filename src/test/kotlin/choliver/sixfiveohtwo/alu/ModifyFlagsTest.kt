package choliver.sixfiveohtwo.alu

import org.junit.jupiter.api.Test

class ModifyFlagsTest {
  @Test
  fun clc() {
    forOpcode(Alu::clc) {
      assertEquals(s.copy(C = _0), s.copy(C = _0))
      assertEquals(s.copy(C = _0), s.copy(C = _1))
    }
  }

  @Test
  fun cld() {
    forOpcode(Alu::cld) {
      assertEquals(s.copy(D = _0), s.copy(D = _0))
      assertEquals(s.copy(D = _0), s.copy(D = _1))
    }
  }

  @Test
  fun cli() {
    forOpcode(Alu::cli) {
      assertEquals(s.copy(I = _0), s.copy(I = _0))
      assertEquals(s.copy(I = _0), s.copy(I = _1))
    }
  }

  @Test
  fun clv() {
    forOpcode(Alu::clv) {
      assertEquals(s.copy(V = _0), s.copy(V = _0))
      assertEquals(s.copy(V = _0), s.copy(V = _1))
    }
  }

  @Test
  fun sec() {
    forOpcode(Alu::sec) {
      assertEquals(s.copy(C = _1), s.copy(C = _0))
      assertEquals(s.copy(C = _1), s.copy(C = _1))
    }
  }

  @Test
  fun sed() {
    forOpcode(Alu::sed) {
      assertEquals(s.copy(D = _1), s.copy(D = _0))
      assertEquals(s.copy(D = _1), s.copy(D = _1))
    }
  }

  @Test
  fun sei() {
    forOpcode(Alu::sei) {
      assertEquals(s.copy(I = _1), s.copy(I = _0))
      assertEquals(s.copy(I = _1), s.copy(I = _1))
    }
  }
}
