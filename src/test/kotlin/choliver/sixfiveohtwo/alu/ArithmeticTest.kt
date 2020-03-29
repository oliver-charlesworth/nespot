package choliver.sixfiveohtwo.alu

import choliver.sixfiveohtwo.State
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test


class ArithmeticTest {
  private val alu = Alu()

  @Test
  fun adc() {
    myAssertEquals(State(A = 0x60u, V = false, C = false, N = false), State(A = 0x50u)) { alu.adc(it, 0x10u) }
    myAssertEquals(State(A = 0xE0u, V = false, C = false, N = true), State(A = 0x50u)) { alu.adc(it, 0x90u) }
    myAssertEquals(State(A = 0x20u, V = false, C = true, N = false), State(A = 0x50u)) { alu.adc(it, 0xD0u) }
    myAssertEquals(State(A = 0xA0u, V = false, C = true, N = true), State(A = 0xD0u)) { alu.adc(it, 0xD0u) }
    // {V = true, C = false, N = false} not possible
    myAssertEquals(State(A = 0xA0u, V = true, C = false, N = true), State(A = 0x50u)) { alu.adc(it, 0x50u) }
    myAssertEquals(State(A = 0x60u, V = true, C = true, N = false), State(A = 0xD0u)) { alu.adc(it, 0x90u) }
    // {V = true, C = true, N = true} not possible
    myAssertEquals(State(A = 0x00u, V = false, C = true, N = false, Z = true), State(A = 0x01u)) { alu.adc(it, 0xFFu) }
  }

  private fun myAssertEquals(expected: State, original: State, fn: (State) -> State) {
    (0 until 4).forEach {
      fun State.withFlags() = copy(
        I = (it and 0x02) != 0x00,
        D = (it and 0x04) != 0x00
      )

      assertEquals(expected.withFlags(), fn(original.withFlags()))
    }
  }
}
