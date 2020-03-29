package choliver.sixfiveohtwo.alu

import choliver.sixfiveohtwo.State
import choliver.sixfiveohtwo.alu.Flag.*
import org.junit.jupiter.api.Test

class ModifyFlagsTest {
  @Test
  fun clc() {
    forOpcode(Alu::clc, Z, I, D, V, N) {
      assertEquals(State(C = false), State(C = false))
      assertEquals(State(C = false), State(C = true))
    }
  }

  @Test
  fun cld() {
    forOpcode(Alu::cld, Z, I, C, V, N) {
      assertEquals(State(D = false), State(D = false))
      assertEquals(State(D = false), State(D = true))
    }
  }

  @Test
  fun cli() {
    forOpcode(Alu::cli, Z, D, C, V, N) {
      assertEquals(State(I = false), State(I = false))
      assertEquals(State(I = false), State(I = true))
    }
  }

  @Test
  fun clv() {
    forOpcode(Alu::clv, Z, I, D, C, N) {
      assertEquals(State(V = false), State(V = false))
      assertEquals(State(V = false), State(V = true))
    }
  }

  @Test
  fun sec() {
    forOpcode(Alu::sec, Z, I, D, V, N) {
      assertEquals(State(C = true), State(C = false))
      assertEquals(State(C = true), State(C = true))
    }
  }

  @Test
  fun sed() {
    forOpcode(Alu::sed, Z, I, C, V, N) {
      assertEquals(State(D = true), State(D = false))
      assertEquals(State(D = true), State(D = true))
    }
  }

  @Test
  fun sei() {
    forOpcode(Alu::sei, Z, D, C, V, N) {
      assertEquals(State(I = true), State(I = false))
      assertEquals(State(I = true), State(I = true))
    }
  }
}
