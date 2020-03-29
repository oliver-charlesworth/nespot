package choliver.sixfiveohtwo.alu

import choliver.sixfiveohtwo.State
import choliver.sixfiveohtwo.alu.Flag.*
import org.junit.jupiter.api.Test

class BitwiseTest {
  @Test
  fun and() {
    forOpcode(Alu::and, I, D, C, V) {
      assertEquals(State(A = 0x01u, Z = false, N = false), State(A = 0x11u), 0x23u)
      assertEquals(State(A = 0x00u, Z = true, N = false), State(A = 0x11u), 0x22u)
      assertEquals(State(A = 0x81u, Z = false, N = true), State(A = 0x81u), 0x83u)
    }
  }

  @Test
  fun ora() {
    forOpcode(Alu::ora, I, D, C, V) {
      assertEquals(State(A = 0x33u, Z = false, N = false), State(A = 0x11u), 0x23u)
      assertEquals(State(A = 0x00u, Z = true, N = false), State(A = 0x00u), 0x00u)
      assertEquals(State(A = 0x83u, Z = false, N = true), State(A = 0x81u), 0x83u)
    }
  }

  @Test
  fun eor() {
    forOpcode(Alu::eor, I, D, C, V) {
      assertEquals(State(A = 0x32u, Z = false, N = false), State(A = 0x11u), 0x23u)
      assertEquals(State(A = 0x00u, Z = true, N = false), State(A = 0x11u), 0x11u)
      assertEquals(State(A = 0x82u, Z = false, N = true), State(A = 0x81u), 0x03u)
    }
  }
}
