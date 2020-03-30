import choliver.sixfiveohtwo.*
import choliver.sixfiveohtwo.AddressMode.*
import com.nhaarman.mockitokotlin2.doAnswer
import com.nhaarman.mockitokotlin2.whenever
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class OperandCalculatorTest {
  private val memory = object : Memory {
    val map = mutableMapOf<UInt16, UInt8>()

    override fun load(address: UInt16) = map[address] ?: 0u

    override fun store(address: UInt16, data: UInt8) {
      map[address] = data
    }
  }

  private val calculator = OperandCalculator(memory)

  @Test
  fun `accumulator returns value of A`() {
    assertEquals(
      0x69.toUInt16(),
      calculator.calculate(Accumulator, State(A = 0x69u))
    )
  }

  @Test
  fun `implied returns 0 unconditionally`() {
    assertEquals(
      0.toUInt16(),
      calculator.calculate(Implied, State(A = 0x69u, X = 0x69u, Y = 0x69u, S = 0x69u, PC = 0x6969u))
    )
  }

  @Test
  fun `immediate returns immediate value`() {
    assertEquals(
      0x69.toUInt16(),
      calculator.calculate(Immediate(0x69u), State())
    )
  }

  @Test
  fun `relative returns signed offset from PC`() {
    // Positive
    assertEquals(
      0x1326.toUInt16(),
      calculator.calculate(Relative(0x32), State(PC = 0x12F4u))
    )
    // Negative
    assertEquals(
      0x12C2.toUInt16(),
      calculator.calculate(Relative(-0x32), State(PC = 0x12F4u))
    )
  }

  @Test
  fun `absolute returns value at address`() {
    memory.store(0x1234u, 0x69u)

    assertEquals(
      0x69.toUInt16(),
      calculator.calculate(Absolute(0x1234u), State())
    )
  }

  @Test
  fun `zero-page returns value at address`() {
    memory.store(0x0034u, 0x69u)

    assertEquals(
      0x69.toUInt16(),
      calculator.calculate(ZeroPage(0x34u), State())
    )
  }
}
