package choliver.sixfiveohtwo.alu

import choliver.sixfiveohtwo.State
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test


class AdcTest {
  private val alu = Alu()

  @Test
  fun `does basic addition`() {
    assertEquals(
      State(A = 0x33u),
      alu.adc(State(A = 0x11u), 0x22u)
    )
  }
  @Test
  fun `doesn't affect I, D`() {
    assertEquals(
      State(A = 0x33u, I = true, D = true),
      alu.adc(State(A = 0x11u, I = true, D = true), 0x22u)
    )
  }

  @Test
  fun `sets C, V, N appropriately`() {
    assertEquals(State(A = 0x60u, V = false, C = false, N = false), alu.adc(State(A = 0x50u), 0x10u))
    assertEquals(State(A = 0xE0u, V = false, C = false, N = true), alu.adc(State(A = 0x50u), 0x90u))
    assertEquals(State(A = 0x20u, V = false, C = true, N = false), alu.adc(State(A = 0x50u), 0xD0u))
    assertEquals(State(A = 0xA0u, V = false, C = true, N = true), alu.adc(State(A = 0xD0u), 0xD0u))
    // {V = true, C = false, N = false} not possible
    assertEquals(State(A = 0xA0u, V = true, C = false, N = true), alu.adc(State(A = 0x50u), 0x50u))
    assertEquals(State(A = 0x60u, V = true, C = true, N = false), alu.adc(State(A = 0xD0u), 0x90u))
    // {V = true, C = true, N = true} not possible
  }

  @Test
  fun `sets Z if zero`() {
    assertEquals(
      State(A = 0x00u, Z = true, C = true),  // C also gets set
      alu.adc(State(A = 0x01u), 0xFFu)
    )
  }
}
