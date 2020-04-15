package choliver.nes.sixfiveohtwo

import choliver.nes.Memory
import choliver.nes.sixfiveohtwo.model.Operand.*
import choliver.nes.sixfiveohtwo.model.Operand.IndexSource.X
import choliver.nes.sixfiveohtwo.model.Operand.IndexSource.Y
import choliver.nes.sixfiveohtwo.model.State
import choliver.nes.sixfiveohtwo.model.toPC
import com.nhaarman.mockitokotlin2.doReturn
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.whenever
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

class AddressCalculatorTest {
  private val memory = mock<Memory>()
  private val calc = AddressCalculator(memory)

  @Nested
  inner class Relative {
    @Test
    fun positiveOffset() {
      assertEquals(
        0x1320,
        calc.calculate(Relative(0x30), State(PC = 0x12F0.toPC()))
      )
    }

    @Test
    fun negativeOffset() {
      assertEquals(
        0x12C0,
        calc.calculate(Relative(0xD0), State(PC = 0x12F0.toPC()))
      )
    }
  }

  @Test
  fun absolute() {
    assertEquals(
      0x1230,
      calc.calculate(Absolute(0x1230), State())
    )
  }

  @Test
  fun zeroPage() {
    assertEquals(
      0x0030,
      calc.calculate(ZeroPage(0x30), State())
    )
  }

  @Test
  fun indirect() {
    whenever(memory.load(0x40FF)) doReturn 0x30
    whenever(memory.load(0x4100)) doReturn 0x12

    assertEquals(
      0x1230,
      calc.calculate(Indirect(0x40FF), State())
    )
  }

  @Nested
  inner class AbsoluteIndexed {
    @Test
    fun basicX() {
      assertEquals(
        0x1230,
        calc.calculate(AbsoluteIndexed(0x1220, X), State(X = 0x10))
      )
    }

    @Test
    fun basicY() {
      assertEquals(
        0x1230,
        calc.calculate(AbsoluteIndexed(0x1220, Y), State(Y = 0x10))
      )
    }
  }

  @Nested
  inner class ZeroPageIndexed {
    @Test
    fun basicX() {
      assertEquals(
        0x0030,
        calc.calculate(ZeroPageIndexed(0x20, X), State(X = 0x10))
      )
    }

    @Test
    fun basicY() {
      assertEquals(
        0x0030,
        calc.calculate(ZeroPageIndexed(0x20, Y), State(Y = 0x10))
      )
    }

    @Test
    fun zeroPageWraparound() {
      assertEquals(
        0x0030,
        calc.calculate(ZeroPageIndexed(0xF0, X), State(X = 0x40))
      )
    }
  }

  @Nested
  inner class IndexedIndirect {
    @Test
    fun basic() {
      whenever(memory.load(0x0030)) doReturn 0x30
      whenever(memory.load(0x0031)) doReturn 0x12

      assertEquals(
        0x1230,
        calc.calculate(IndexedIndirect(0x20), State(X = 0x10))
      )
    }

    @Test
    fun zeroPageWraparoundOffset() {
      whenever(memory.load(0x0030)) doReturn 0x30
      whenever(memory.load(0x0031)) doReturn 0x12

      assertEquals(
        0x1230,
        calc.calculate(IndexedIndirect(0xF0), State(X = 0x40))
      )
    }

    @Test
    fun zeroPageWraparoundHighByte() {
      whenever(memory.load(0x00FF)) doReturn 0x30
      whenever(memory.load(0x0000)) doReturn 0x12

      assertEquals(
        0x1230,
        calc.calculate(IndexedIndirect(0xFF), State(X = 0x00))
      )
    }

  }

  @Nested
  inner class IndirectIndexed {
    @Test
    fun basic() {
      whenever(memory.load(0x0030)) doReturn 0x30
      whenever(memory.load(0x0031)) doReturn 0x12

      assertEquals(
        0x1240,
        calc.calculate(IndirectIndexed(0x30), State(Y = 0x10))
      )
    }

    @Test
    fun zeroPageWraparoundHighByte() {
      whenever(memory.load(0x00FF)) doReturn 0x30
      whenever(memory.load(0x0000)) doReturn 0x12

      assertEquals(
        0x1240,
        calc.calculate(IndirectIndexed(0xFF), State(Y = 0x10))
      )
    }
  }

}
