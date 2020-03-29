package choliver.sixfiveohtwo.alu

import org.junit.jupiter.api.Test

class ModifyFlagsTest {
  @Test
  fun clc() {
    forOpcode(Alu::clc) {
      assertEquals(s.copy(C = false), s.copy(C = false))
      assertEquals(s.copy(C = false), s.copy(C = true))
    }
  }

  @Test
  fun cld() {
    forOpcode(Alu::cld) {
      assertEquals(s.copy(D = false), s.copy(D = false))
      assertEquals(s.copy(D = false), s.copy(D = true))
    }
  }

  @Test
  fun cli() {
    forOpcode(Alu::cli) {
      assertEquals(s.copy(I = false), s.copy(I = false))
      assertEquals(s.copy(I = false), s.copy(I = true))
    }
  }

  @Test
  fun clv() {
    forOpcode(Alu::clv) {
      assertEquals(s.copy(V = false), s.copy(V = false))
      assertEquals(s.copy(V = false), s.copy(V = true))
    }
  }

  @Test
  fun sec() {
    forOpcode(Alu::sec) {
      assertEquals(s.copy(C = true), s.copy(C = false))
      assertEquals(s.copy(C = true), s.copy(C = true))
    }
  }

  @Test
  fun sed() {
    forOpcode(Alu::sed) {
      assertEquals(s.copy(D = true), s.copy(D = false))
      assertEquals(s.copy(D = true), s.copy(D = true))
    }
  }

  @Test
  fun sei() {
    forOpcode(Alu::sei) {
      assertEquals(s.copy(I = true), s.copy(I = false))
      assertEquals(s.copy(I = true), s.copy(I = true))
    }
  }
}
