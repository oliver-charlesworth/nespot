import choliver.sixfiveohtwo.*
import choliver.sixfiveohtwo.AddressMode.*
import choliver.sixfiveohtwo.AddressMode.IndexSource.*
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class OperandCalculatorTest {
  private val memory = FakeMemory()
  private val calculator = OperandCalculator(memory)

  @Test
  fun accumulator() {
    assertEquals(
      0x69.toUInt16(),
      calculator.calculate(Accumulator, State(A = 0x69u))
    )
  }

  @Test
  fun implied() {
    assertEquals(
      0.toUInt16(),
      calculator.calculate(Implied, State(A = 0x69u, X = 0x69u, Y = 0x69u, S = 0x69u, PC = 0x6969u))
    )
  }

  @Test
  fun immediate() {
    assertEquals(
      0x69.toUInt16(),
      calculator.calculate(Immediate(0x69u), State())
    )
  }

  @Test
  fun relative() {
    // Positive
    assertEquals(
      0x1320.toUInt16(),
      calculator.calculate(Relative(0x30), State(PC = 0x12F0u))
    )

    // Negative
    assertEquals(
      0x12C0.toUInt16(),
      calculator.calculate(Relative(-0x30), State(PC = 0x12F0u))
    )
  }

  @Test
  fun absolute() {
    memory.store(0x1230u, 0x69u)

    assertEquals(
      0x69.toUInt16(),
      calculator.calculate(Absolute(0x1230u), State())
    )
  }

  @Test
  fun zeroPage() {
    memory.store(0x0030u, 0x69u)

    assertEquals(
      0x69.toUInt16(),
      calculator.calculate(ZeroPage(0x30u), State())
    )
  }

  @Test
  fun absoluteIndexed() {
    memory.store(0x1230u, 0x69u)

    assertEquals(
      0x69.toUInt16(),
      calculator.calculate(AbsoluteIndexed(0x1220u, X), State(X = 0x10u))
    )

    assertEquals(
      0x69.toUInt16(),
      calculator.calculate(AbsoluteIndexed(0x1220u, Y), State(Y = 0x10u))
    )
  }

  @Test
  fun zeroPageIndexed() {
    memory.store(0x0030u, 0x69u)

    assertEquals(
      0x69.toUInt16(),
      calculator.calculate(ZeroPageIndexed(0x20u, X), State(X = 0x10u))
    )

    assertEquals(
      0x69.toUInt16(),
      calculator.calculate(ZeroPageIndexed(0x20u, Y), State(Y = 0x10u))
    )

    // Wraps around within zero page
    assertEquals(
      0x69.toUInt16(),
      calculator.calculate(ZeroPageIndexed(0xF0u, X), State(X = 0x40u))
    )
  }
}

