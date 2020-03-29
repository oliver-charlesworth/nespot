package choliver.sixfiveohtwo.alu

import choliver.sixfiveohtwo.State
import org.junit.jupiter.api.Test

class BitwiseTest {
  private val alu = Alu()

  @Test
  fun and() {
    assertEqualsIDCV(State(A = 0x01u, Z = false, N = false), State(A = 0x11u)) { alu.and(it, 0x23u) }
    assertEqualsIDCV(State(A = 0x00u, Z = true, N = false), State(A = 0x11u)) { alu.and(it,0x22u) }
    assertEqualsIDCV(State(A = 0x81u, Z = false, N = true), State(A = 0x81u)) { alu.and(it, 0x83u) }
  }

  @Test
  fun ora() {
    assertEqualsIDCV(State(A = 0x33u, Z = false, N = false), State(A = 0x11u)) { alu.ora(it, 0x23u) }
    assertEqualsIDCV(State(A = 0x00u, Z = true, N = false), State(A = 0x00u)) { alu.ora(it, 0x00u) }
    assertEqualsIDCV(State(A = 0x83u, Z = false, N = true), State(A = 0x81u)) { alu.ora(it, 0x83u) }
  }

  @Test
  fun eor() {
    assertEqualsIDCV(State(A = 0x32u, Z = false, N = false), State(A = 0x11u)) { alu.eor(it, 0x23u) }
    assertEqualsIDCV(State(A = 0x00u, Z = true, N = false), State(A = 0x11u)) { alu.eor(it, 0x11u) }
    assertEqualsIDCV(State(A = 0x82u, Z = false, N = true), State(A = 0x81u)) { alu.eor(it, 0x03u) }
  }
}
