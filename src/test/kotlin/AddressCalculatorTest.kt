import choliver.sixfiveohtwo.*
import choliver.sixfiveohtwo.AddressMode.*
import choliver.sixfiveohtwo.AddressMode.IndexSource.*
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

class AddressCalculatorTest {
  private val memory = FakeMemory()
  private val calc = AddressCalculator(memory)

  @Nested
  inner class Relative {
    @Test
    fun positiveOffset() {
      assertEquals(
        0x1320.toUInt16(),
        calc.calculate(Relative(0x30), State(PC = 0x12F0u))
      )
    }

    @Test
    fun negativeOffset() {
      assertEquals(
        0x12C0.toUInt16(),
        calc.calculate(Relative(-0x30), State(PC = 0x12F0u))
      )
    }
  }

  @Test
  fun absolute() {
    assertEquals(
      0x1230u.toUInt16(),
      calc.calculate(Absolute(0x1230u), State())
    )
  }

  @Test
  fun zeroPage() {
    assertEquals(
      0x0030u.toUInt16(),
      calc.calculate(ZeroPage(0x30u), State())
    )
  }

  @Test
  fun indirect() {
    assertEquals(
      0x1230u.toUInt16(),
      calc.calculate(Indirect(0x1230u), State())
    )
  }

  @Nested
  inner class AbsoluteIndexed {
    @Test
    fun basicX() {
      assertEquals(
        0x1230u.toUInt16(),
        calc.calculate(AbsoluteIndexed(0x1220u, X), State(X = 0x10u))
      )
    }

    @Test
    fun basicY() {
      assertEquals(
        0x1230u.toUInt16(),
        calc.calculate(AbsoluteIndexed(0x1220u, Y), State(Y = 0x10u))
      )
    }
  }

  @Nested
  inner class ZeroPageIndexed {
    @Test
    fun basicX() {
      assertEquals(
        0x0030u.toUInt16(),
        calc.calculate(ZeroPageIndexed(0x20u, X), State(X = 0x10u))
      )
    }

    @Test
    fun basicY() {
      assertEquals(
        0x0030u.toUInt16(),
        calc.calculate(ZeroPageIndexed(0x20u, Y), State(Y = 0x10u))
      )
    }

    @Test
    fun zeroPageWraparound() {
      assertEquals(
        0x0030u.toUInt16(),
        calc.calculate(ZeroPageIndexed(0xF0u, X), State(X = 0x40u))
      )
    }
  }

  @Nested
  inner class IndexedIndirect {
    @Test
    fun basic() {
      memory.store(0x0030u, 0x30u)
      memory.store(0x0031u, 0x12u)

      assertEquals(
        0x1230u.toUInt16(),
        calc.calculate(IndexedIndirect(0x20u), State(X = 0x10u))
      )
    }

    @Test
    fun zeroPageWraparoundOffset() {
      memory.store(0x0030u, 0x30u)
      memory.store(0x0031u, 0x12u)

      assertEquals(
        0x1230u.toUInt16(),
        calc.calculate(IndexedIndirect(0xF0u), State(X = 0x40u))
      )
    }

    @Test
    fun zeroPageWraparoundHighByte() {
      memory.store(0x00FFu, 0x30u)
      memory.store(0x0000u, 0x12u)

      assertEquals(
        0x1230u.toUInt16(),
        calc.calculate(IndexedIndirect(0xFFu), State(X = 0x00u))
      )
    }

  }

  @Nested
  inner class IndirectIndexed {
    @Test
    fun basic() {
      memory.store(0x0030u, 0x30u)
      memory.store(0x0031u, 0x12u)

      assertEquals(
        0x1240u.toUInt16(),
        calc.calculate(IndirectIndexed(0x30u), State(Y = 0x10u))
      )
    }

    @Test
    fun zeroPageWraparoundHighByte() {
      memory.store(0x00FFu, 0x30u)
      memory.store(0x0000u, 0x12u)

      assertEquals(
        0x1240u.toUInt16(),
        calc.calculate(IndirectIndexed(0xFFu), State(Y = 0x10u))
      )
    }
  }

}

