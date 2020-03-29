package choliver.sixfiveohtwo

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

data class State(
  val PC: UShort = 0x00u,
  val A: UByte = 0x00u,
  val X: UByte = 0x00u,
  val Y: UByte = 0x00u,
  val S: UByte = 0x00u,
  val C: Boolean = false,
  val Z: Boolean = false,
  val I: Boolean = false,
  val D: Boolean = false,
  val V: Boolean = false,
  val N: Boolean = false
)

class Alu {
  fun adc(state: State, operand: UByte): State {
    val raw = state.A + operand
    val result = raw.toUByte()
    return state.copy(
      A = result,
      C = (raw and 0x100u) != 0u,
      Z = result == 0.toUByte(),
      V = (state.A.isNegative() == operand.isNegative()) && (state.A.isNegative() != result.isNegative()),
      N = result.isNegative()
    )
  }

  private fun UByte.isNegative() = this >= 0x80u
}


class AluTest {
  val alu = Alu()

  @Nested
  inner class Adc {
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
}
