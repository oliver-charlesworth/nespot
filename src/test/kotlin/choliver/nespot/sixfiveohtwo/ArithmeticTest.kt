package choliver.nespot.sixfiveohtwo

import choliver.nespot.Data
import choliver.nespot.hi
import choliver.nespot.lo
import choliver.nespot.sixfiveohtwo.model.AddressMode.ACCUMULATOR
import choliver.nespot.sixfiveohtwo.model.Instruction
import choliver.nespot.sixfiveohtwo.model.Opcode
import choliver.nespot.sixfiveohtwo.model.Opcode.*
import choliver.nespot.sixfiveohtwo.model.Operand.Immediate
import choliver.nespot.sixfiveohtwo.model.Operand.ZeroPage
import choliver.nespot.sixfiveohtwo.model.State
import choliver.nespot.sixfiveohtwo.utils._0
import choliver.nespot.sixfiveohtwo.utils._1
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

class ArithmeticTest {
  @Nested
  inner class Adc {
    @Test
    fun resultIsPositive() {
      assertForAddressModesAndCarry(originalA = 0x20, rhs = 0x10, a = 0x30, v = _0, c = _0, n = _0, z = _0)
    }

    @Test
    fun resultIsNegative() {
      assertForAddressModesAndCarry(originalA = 0x20, rhs = 0xD0, a = 0xF0, v = _0, c = _0, n = _1, z = _0)
    }

    @Test
    fun unsignedCarryOutAndPositive() {
      assertForAddressModesAndCarry(originalA = 0x50, rhs = 0xD0, a = 0x20, v = _0, c = _1, n = _0, z = _0)
    }

    @Test
    fun unsignedCarryOutAndNegative() {
      assertForAddressModesAndCarry(originalA = 0xD0, rhs = 0xD0, a = 0xA0, v = _0, c = _1, n = _1, z = _0)
    }

    @Test
    fun overflowToNegative() {
      assertForAddressModesAndCarry(originalA = 0x20, rhs = 0x70, a = 0x90, v = _1, c = _0, n = _1, z = _0)
    }

    @Test
    fun overflowToPositive() {
      assertForAddressModesAndCarry(originalA = 0xE0, rhs = 0x90, a = 0x70, v = _1, c = _1, n = _0, z = _0)
    }

    @Test
    fun resultIsZero() {
      assertForAddressModesAndCarry(originalA = 0x10, rhs = 0xF0, a = 0x00, v = _0, c = _1, n = _0, z = _1)
    }

    @Test
    fun multiByteNoCarry() {
      assertMultiByte(0xCCCC, 0x3333, 0xFFFF)
    }

    @Test
    fun multiByteCarry() {
      assertMultiByte(0xCCCC, 0xAAAA, 0x7776)
    }

    private fun assertMultiByte(a: Int, b: Int, expected: Int) {
      assertCpuEffects(
        instructions = listOf(
          Instruction(LDA, Immediate(a.lo())),
          Instruction(ADC, Immediate(b.lo())),
          Instruction(STA, ZeroPage(0x00)),
          Instruction(LDA, Immediate(a.hi())),
          Instruction(ADC, Immediate(b.lo())),
          Instruction(STA, ZeroPage(0x01))
        ),
        initState = State(),
        expectedStores = listOf(0x00 to expected.lo(), 0x01 to expected.hi())
      )
    }

    // Each case implemented twice to demonstrate flag setting respects carry in:
    // (1) basic, (2) carry-in set with (target + 1)
    private fun assertForAddressModesAndCarry(
      originalA: Data,
      rhs: Data,
      a: Data,
      v: Boolean,
      c: Boolean,
      n: Boolean,
      z: Boolean
    ) {
      // TODO - exercise decimal mode

      assertForAddressModes(
        ADC,
        target = rhs,
        initState = { with(a = originalA, c = _0, d = _0) },
        expectedState = { with(a = a, v = v, c = c, n = n, z = z, d = _0) }
      )

      assertForAddressModes(
        ADC,
        target = rhs,
        initState = { with(a = (originalA - 1), c = _1, d = _0) },
        expectedState = { with(a = a, v = v, c = c, n = n, z = z, d = _0) }
      )
    }
  }

  @Nested
  inner class Sbc {
    @Test
    fun resultIsPositive() {
      assertForAddressModesAndCarry(originalA = 0x20, rhs = 0xF0, a = 0x30, v = _0, c = _0, n = _0, z = _0)
    }

    @Test
    fun resultIsNegative() {
      assertForAddressModesAndCarry(originalA = 0x20, rhs = 0x30, a = 0xF0, v = _0, c = _0, n = _1, z = _0)
    }

    @Test
    fun unsignedCarryOutAndPositive() {
      assertForAddressModesAndCarry(originalA = 0x50, rhs = 0x30, a = 0x20, v = _0, c = _1, n = _0, z = _0)
    }

    @Test
    fun unsignedCarryOutAndNegative() {
      assertForAddressModesAndCarry(originalA = 0xD0, rhs = 0x30, a = 0xA0, v = _0, c = _1, n = _1, z = _0)
    }

    @Test
    fun overflowToNegative() {
      assertForAddressModesAndCarry(originalA = 0x20, rhs = 0x90, a = 0x90, v = _1, c = _0, n = _1, z = _0)
    }

    @Test
    fun overflowToPositive() {
      assertForAddressModesAndCarry(originalA = 0xE0, rhs = 0x70, a = 0x70, v = _1, c = _1, n = _0, z = _0)
    }

    @Test
    fun resultIsZero() {
      assertForAddressModesAndCarry(originalA = 0x10, rhs = 0x10, a = 0x00, v = _0, c = _1, n = _0, z = _1)
    }

    @Test
    fun multiByteNoBorrow() {
      assertMultiByte(0xAAAA, 0x3333, 0x7777)
    }

    @Test
    fun multiByteBorrow() {
      assertMultiByte(0xAAAA, 0xCCCC, 0xDDDE)
    }

    private fun assertMultiByte(a: Int, b: Int, expected: Int) {
      assertCpuEffects(
        instructions = listOf(
          Instruction(SEC),
          Instruction(LDA, Immediate(a.lo())),
          Instruction(SBC, Immediate(b.lo())),
          Instruction(STA, ZeroPage(0x00)),
          Instruction(LDA, Immediate(a.hi())),
          Instruction(SBC, Immediate(b.lo())),
          Instruction(STA, ZeroPage(0x01))
        ),
        initState = State(),
        expectedStores = listOf(0x00 to expected.lo(), 0x01 to expected.hi())
      )
    }

    // Each case implemented twice to demonstrate flag setting respects borrow in:
    // (1) basic, (2) borrow-in set with (target + 1)
    private fun assertForAddressModesAndCarry(
      originalA: Data,
      rhs: Data,
      a: Data,
      v: Boolean,
      c: Boolean,
      n: Boolean,
      z: Boolean
    ) {
      // TODO - exercise decimal mode

      assertForAddressModes(
        SBC,
        target = rhs,
        initState = { with(a = originalA, c = _1, d = _0) },
        expectedState = { with(a = a, v = v, c = c, n = n, z = z, d = _0) }
      )

      assertForAddressModes(
        SBC,
        target = rhs,
        initState = { with(a = (originalA + 1), c = _0, d = _0) },
        expectedState = { with(a = a, v = v, c = c, n = n, z = z, d = _0) }
      )
    }
  }

  @Nested
  inner class Cmp {
    @Test
    fun greaterThan() {
      assertForAddressModes(
        CMP,
        target = 0xFD,
        initState = { with(a = 0xFE) },
        expectedState = { with(a = 0xFE, c = _1, n = _0, z = _0) }
      )
    }

    @Test
    fun lessThan() {
      assertForAddressModes(
        CMP,
        target = 0xFF,
        initState = { with(a = 0xFE) },
        expectedState = { with(a = 0xFE, c = _0, n = _1, z = _0) }
      )
    }

    @Test
    fun equal() {
      assertForAddressModes(
        CMP,
        target = 0xFE,
        initState = { with(a = 0xFE) },
        expectedState = { with(a = 0xFE, c = _1, n = _0, z = _1) }
      )
    }

    @Test
    fun comparisonIsUnsigned() {
      assertForAddressModes(
        CMP,
        target = 0x7F,
        initState = { with(a = 0x80) },
        expectedState = { with(a = 0x80, c = _1, n = _0, z = _0) } // 0x80 > 0x7F only if unsigned
      )
    }

    @Test
    fun muchGreaterThanSetsN() {
      assertForAddressModes(
        CMP,
        target = 0x00,
        initState = { with(a = 0xFE) },
        expectedState = { with(a = 0xFE, c = _1, n = _1, z = _0) }
      )
    }
  }

  @Nested
  inner class Cpx {
    @Test
    fun greaterThan() {
      assertForAddressModes(
        CPX,
        target = 0xFD,
        initState = { with(x = 0xFE) },
        expectedState = { with(x = 0xFE, c = _1, n = _0, z = _0) }
      )
    }

    // TODO
  }

  @Nested
  inner class Cpy {
    @Test
    fun greaterThan() {
      assertForAddressModes(
        CPY,
        target = 0xFD,
        initState = { with(y = 0xFE) },
        expectedState = { with(y = 0xFE, c = _1, n = _0, z = _0) }
      )
    }

    // TODO
  }

  @Test
  fun dec() {
    assertForAddressModes(
      DEC,
      target = 0x02,
      expectedState = { with(z = _0, n = _0) },
      expectedStores = { listOf(it to 0x01) }
    )
    assertForAddressModes(
      DEC,
      target = 0x01,
      expectedState = { with(z = _1, n = _0) },
      expectedStores = { listOf(it to 0x00) }
    )
    assertForAddressModes(
      DEC,
      target = 0xFF,
      expectedState = { with(z = _0, n = _1) },
      expectedStores = { listOf(it to 0xFE) }
    )
  }

  @Test
  fun dex() {
    assertForAddressModes(
      DEX,
      initState = { with(x = 0x02) },
      expectedState = { with(x = 0x01, z = _0, n = _0) }
    )
    assertForAddressModes(
      DEX,
      initState = { with(x = 0x01) },
      expectedState = { with(x = 0x00, z = _1, n = _0) }
    )
    assertForAddressModes(
      DEX,
      initState = { with(x = 0xFF) },
      expectedState = { with(x = 0xFE, z = _0, n = _1) }
    )
  }

  @Test
  fun dey() {
    assertForAddressModes(
      DEY,
      initState = { with(y = 0x02) },
      expectedState = { with(y = 0x01, z = _0, n = _0) }
    )
    assertForAddressModes(
      DEY,
      initState = { with(y = 0x01) },
      expectedState = { with(y = 0x00, z = _1, n = _0) }
    )
    assertForAddressModes(
      DEY,
      initState = { with(y = 0xFF) },
      expectedState = { with(y = 0xFE, z = _0, n = _1) }
    )
  }

  @Test
  fun inc() {
    assertForAddressModes(
      INC,
      target = 0x01,
      expectedState = { with(z = _0, n = _0) },
      expectedStores = { listOf(it to 0x02) }
    )
    assertForAddressModes(
      INC,
      target = 0xFF,
      expectedState = { with(z = _1, n = _0) },
      expectedStores = { listOf(it to 0x00) }
    )
    assertForAddressModes(
      INC,
      target = 0xFE,
      expectedState = { with(z = _0, n = _1) },
      expectedStores = { listOf(it to 0xFF) }
    )
  }

  @Test
  fun inx() {
    assertForAddressModes(
      INX,
      initState = { with(x = 0x01) },
      expectedState = { with(x = 0x02, z = _0, n = _0) }
    )
    assertForAddressModes(
      INX,
      initState = { with(x = 0xFF) },
      expectedState = { with(x = 0x00, z = _1, n = _0) }
    )
    assertForAddressModes(
      INX,
      initState = { with(x = 0xFE) },
      expectedState = { with(x = 0xFF, z = _0, n = _1) }
    )
  }

  @Test
  fun iny() {
    assertForAddressModes(
      INY,
      initState = { with(y = 0x01) },
      expectedState = { with(y = 0x02, z = _0, n = _0) }
    )
    assertForAddressModes(
      INY,
      initState = { with(y = 0xFF) },
      expectedState = { with(y = 0x00, z = _1, n = _0) }
    )
    assertForAddressModes(
      INY,
      initState = { with(y = 0xFE) },
      expectedState = { with(y = 0xFF, z = _0, n = _1) }
    )
  }

  @Test
  fun asl() {
    assertShift(ASL, 0x02, 0x04) { with(c = _0, z = _0, n = _0) }
    assertShift(ASL, 0x40, 0x80) { with(c = _0, z = _0, n = _1) }
    assertShift(ASL, 0x88, 0x10) { with(c = _1, z = _0, n = _0) }
    assertShift(ASL, 0x80, 0x00) { with(c = _1, z = _1, n = _0) }
  }

  @Test
  fun lsr() {
    assertShift(LSR, 0x80, 0x40) { with(c = _0, z = _0, n = _0) }
    assertShift(LSR, 0x01, 0x00) { with(c = _1, z = _1, n = _0) }
    assertShift(LSR, 0x00, 0x00) { with(c = _0, z = _1, n = _0) }
  }

  @Test
  fun rol() {
    assertShift(ROL, 0x02, 0x04, { with(c = _0) }) { with(c = _0, z = _0, n = _0) }
    assertShift(ROL, 0x40, 0x80, { with(c = _0) }) { with(c = _0, z = _0, n = _1) }
    assertShift(ROL, 0x88, 0x10, { with(c = _0) }) { with(c = _1, z = _0, n = _0) }
    assertShift(ROL, 0x80, 0x00, { with(c = _0) }) { with(c = _1, z = _1, n = _0) }
    assertShift(ROL, 0x02, 0x05, { with(c = _1) }) { with(c = _0, z = _0, n = _0) }
  }

  @Test
  fun ror() {
    assertShift(ROR, 0x80, 0x40, { with(c = _0) }) { with(c = _0, z = _0, n = _0) }
    assertShift(ROR, 0x01, 0x00, { with(c = _0) }) { with(c = _1, z = _1, n = _0) }
    assertShift(ROR, 0x00, 0x00, { with(c = _0) }) { with(c = _0, z = _1, n = _0) }
    assertShift(ROR, 0x80, 0xC0, { with(c = _1) }) { with(c = _0, z = _0, n = _1) }
  }

  // Accumulator mode is a special case, so handle it separately
  private fun assertShift(
    op: Opcode,
    target: Data,
    expected: Data,
    initState: State.() -> State = { this },
    expectedState: State.() -> State
  ) {
    assertForAddressModes(
      op,
      modes = setOf(ACCUMULATOR),
      initState = { with(a = target).initState() },
      expectedState = { with(a = expected).expectedState() }
    )

    assertForAddressModes(
      op,
      modes = OPCODES_TO_ENCODINGS[op]!!.keys - ACCUMULATOR,
      target = target,
      initState = initState,
      expectedState = expectedState,
      expectedStores = { listOf(it to expected) }
    )
  }
}
