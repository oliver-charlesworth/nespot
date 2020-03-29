package choliver.sixfiveohtwo.alu

import choliver.sixfiveohtwo.State
import choliver.sixfiveohtwo.alu.Flag.*
import org.junit.jupiter.api.Test

class ModifyFlagsTest {
  @Test
  fun clc() {
    withInvariants(Z, I, D, V, N) {
      assertEquals(State(C = false), State(C = false)) { alu.clc(it) }
      assertEquals(State(C = false), State(C = true)) { alu.clc(it) }
    }
  }
}
