package choliver.sixfiveohtwo.alu

import choliver.sixfiveohtwo.State
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test


class BitwiseTest {
  private val alu = Alu()

  @Test
  fun and() {
    myAssertEquals(State(A = 0x01u, Z = false, N = false), State(A = 0x11u)) { alu.and(it, 0x23u) }
    myAssertEquals(State(A = 0x00u, Z = true, N = false), State(A = 0x11u)) { alu.and(it,0x22u) }
    myAssertEquals(State(A = 0x81u, Z = false, N = true), State(A = 0x81u)) { alu.and(it, 0x83u) }
  }

  @Test
  fun ora() {
    myAssertEquals(State(A = 0x33u, Z = false, N = false), State(A = 0x11u)) { alu.ora(it, 0x23u) }
    myAssertEquals(State(A = 0x00u, Z = true, N = false), State(A = 0x00u)) { alu.ora(it, 0x00u) }
    myAssertEquals(State(A = 0x83u, Z = false, N = true), State(A = 0x81u)) { alu.ora(it, 0x83u) }
  }

  @Test
  fun eor() {
    myAssertEquals(State(A = 0x32u, Z = false, N = false), State(A = 0x11u)) { alu.eor(it, 0x23u) }
    myAssertEquals(State(A = 0x00u, Z = true, N = false), State(A = 0x11u)) { alu.eor(it, 0x11u) }
    myAssertEquals(State(A = 0x82u, Z = false, N = true), State(A = 0x81u)) { alu.eor(it, 0x03u) }
  }

  private fun myAssertEquals(expected: State, original: State, fn: (State) -> State) {
    (0 until 16).forEach {
      fun State.withFlags() = copy(
        C = (it and 0x01) != 0x00,
        I = (it and 0x02) != 0x00,
        D = (it and 0x04) != 0x00,
        V = (it and 0x08) != 0x00
      )

      assertEquals(expected.withFlags(), fn(original.withFlags()))
    }
  }
}
