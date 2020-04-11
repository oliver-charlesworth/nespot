package choliver.nes.sixfiveohtwo.cpu

import choliver.nes.sixfiveohtwo.OPCODES_TO_ENCODINGS
import choliver.nes.sixfiveohtwo.assertCpuEffects
import choliver.nes.sixfiveohtwo.assertForAddressModes
import choliver.nes.sixfiveohtwo.model.*
import choliver.nes.sixfiveohtwo.model.AddressMode.ACCUMULATOR
import choliver.nes.sixfiveohtwo.model.Opcode.*
import choliver.nes.sixfiveohtwo.model.Operand.Immediate
import choliver.nes.sixfiveohtwo.model.Operand.ZeroPage
import choliver.nes.sixfiveohtwo.utils._0
import choliver.nes.sixfiveohtwo.utils._1
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

class ArithmeticTest {
  @Nested
  inner class Adc {
    @Test
    fun resultIsPositive() {
      assertForAddressModesAndCarry(target = 0x10, originalA = 0x20, A = 0x30, V = _0, C = _0, N = _0, Z = _0)
    }

    @Test
    fun resultIsNegative() {
      assertForAddressModesAndCarry(target = 0xD0, originalA = 0x20, A = 0xF0, V = _0, C = _0, N = _1, Z = _0)
    }

    @Test
    fun unsignedCarryOutAndPositive() {
      assertForAddressModesAndCarry(target = 0xD0, originalA = 0x50, A = 0x20, V = _0, C = _1, N = _0, Z = _0)
    }

    @Test
    fun unsignedCarryOutAndNegative() {
      assertForAddressModesAndCarry(target = 0xD0, originalA = 0xD0, A = 0xA0, V = _0, C = _1, N = _1, Z = _0)
    }

    @Test
    fun overflowToNegative() {
      assertForAddressModesAndCarry(target = 0x70, originalA = 0x20, A = 0x90, V = _1, C = _0, N = _1, Z = _0)
    }

    @Test
    fun overflowToPositive() {
      assertForAddressModesAndCarry(target = 0x90, originalA = 0xE0, A = 0x70, V = _1, C = _1, N = _0, Z = _0)
    }

    @Test
    fun resultIsZero() {
      assertForAddressModesAndCarry(target = 0xF0, originalA = 0x10, A = 0x00, V = _0, C = _1, N = _0, Z = _1)
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
          Instruction(STA, ZeroPage(0x00u)),
          Instruction(LDA, Immediate(a.hi())),
          Instruction(ADC, Immediate(b.lo())),
          Instruction(STA, ZeroPage(0x01u))
        ),
        initState = State(),
        expectedStores = mapOf(0x00 to expected.lo().toInt(), 0x01 to expected.hi().toInt())
      )
    }

    // Each case implemented twice to demonstrate flag setting respects carry in:
    // (1) basic, (2) carry-in set with (target + 1)
    private fun assertForAddressModesAndCarry(
      target: Int,
      originalA: Int,
      A: Int,
      V: Boolean,
      C: Boolean,
      N: Boolean,
      Z: Boolean
    ) {
      // TODO - exercise decimal mode

      assertForAddressModes(
        ADC,
        target = target,
        initState = { with(A = originalA.u8(), C = _0, D = _0) },
        expectedState = { with(A = A.u8(), V = V, C = C, N = N, Z = Z, D = _0) }
      )

      assertForAddressModes(
        ADC,
        target = target,
        initState = { with(A = (originalA - 1).u8(), C = _1, D = _0) },
        expectedState = { with(A = A.u8(), V = V, C = C, N = N, Z = Z, D = _0) }
      )
    }
  }

  @Nested
  inner class Sbc {
    @Test
    fun resultIsPositive() {
      assertForAddressModesAndCarry(target = 0xF0, originalA = 0x20, A = 0x30, V = _0, C = _0, N = _0, Z = _0)
    }

    @Test
    fun resultIsNegative() {
      assertForAddressModesAndCarry(target = 0x30, originalA = 0x20, A = 0xF0, V = _0, C = _0, N = _1, Z = _0)
    }

    @Test
    fun unsignedCarryOutAndPositive() {
      assertForAddressModesAndCarry(target = 0x30, originalA = 0x50, A = 0x20, V = _0, C = _1, N = _0, Z = _0)
    }

    @Test
    fun unsignedCarryOutAndNegative() {
      assertForAddressModesAndCarry(target = 0x30, originalA = 0xD0, A = 0xA0, V = _0, C = _1, N = _1, Z = _0)
    }

    @Test
    fun overflowToNegative() {
      assertForAddressModesAndCarry(target = 0x90, originalA = 0x20, A = 0x90, V = _1, C = _0, N = _1, Z = _0)
    }

    @Test
    fun overflowToPositive() {
      assertForAddressModesAndCarry(target = 0x70, originalA = 0xE0, A = 0x70, V = _1, C = _1, N = _0, Z = _0)
    }

    @Test
    fun resultIsZero() {
      assertForAddressModesAndCarry(target = 0x10, originalA = 0x10, A = 0x00, V = _0, C = _1, N = _0, Z = _1)
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
          Instruction(STA, ZeroPage(0x00u)),
          Instruction(LDA, Immediate(a.hi())),
          Instruction(SBC, Immediate(b.lo())),
          Instruction(STA, ZeroPage(0x01u))
        ),
        initState = State(),
        expectedStores = mapOf(0x00 to expected.lo().toInt(), 0x01 to expected.hi().toInt())
      )
    }

    // Each case implemented twice to demonstrate flag setting respects borrow in:
    // (1) basic, (2) borrow-in set with (target + 1)
    private fun assertForAddressModesAndCarry(
      target: Int,
      originalA: Int,
      A: Int,
      V: Boolean,
      C: Boolean,
      N: Boolean,
      Z: Boolean
    ) {
      // TODO - exercise decimal mode

      assertForAddressModes(
        SBC,
        target = target,
        initState = { with(A = originalA.u8(), C = _1, D = _0) },
        expectedState = { with(A = A.u8(), V = V, C = C, N = N, Z = Z, D = _0) }
      )

      assertForAddressModes(
        SBC,
        target = target,
        initState = { with(A = (originalA + 1).u8(), C = _0, D = _0) },
        expectedState = { with(A = A.u8(), V = V, C = C, N = N, Z = Z, D = _0) }
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
        initState = { with(A = 0xFEu) },
        expectedState = { with(A = 0xFEu, C = _1, N = _0, Z = _0) }
      )
    }

    @Test
    fun lessThan() {
      assertForAddressModes(
        CMP,
        target = 0xFF,
        initState = { with(A = 0xFEu) },
        expectedState = { with(A = 0xFEu, C = _0, N = _1, Z = _0) }
      )
    }

    @Test
    fun equal() {
      assertForAddressModes(
        CMP,
        target = 0xFE,
        initState = { with(A = 0xFEu) },
        expectedState = { with(A = 0xFEu, C = _1, N = _0, Z = _1) }
      )
    }

    @Test
    fun comparisonIsUnsigned() {
      assertForAddressModes(
        CMP,
        target = 0x7F,
        initState = { with(A = 0x80u) },
        expectedState = { with(A = 0x80u, C = _1, N = _0, Z = _0) } // 0x80 > 0x7F only if unsigned
      )
    }

    @Test
    fun muchGreaterThanSetsN() {
      assertForAddressModes(
        CMP,
        target = 0x00,
        initState = { with(A = 0xFEu) },
        expectedState = { with(A = 0xFEu, C = _1, N = _1, Z = _0) }
      )
    }
  }

  @Nested
  inner class Cpx {
    @Test
    fun greaterThan() {
      assertForAddressModes(
        CPX,
        initState = { with(A = 0xFEu, X = 0xFDu) },
        expectedState = { with(A = 0xFEu, X = 0xFDu, C = _1, N = _0, Z = _0) }
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
        initState = { with(A = 0xFEu, Y = 0xFDu) },
        expectedState = { with(A = 0xFEu, Y = 0xFDu, C = _1, N = _0, Z = _0) }
      )
    }

    // TODO
  }

  @Test
  fun dec() {
    assertForAddressModes(
      DEC,
      target = 0x02,
      expectedState = { with(Z = _0, N = _0) },
      expectedStores = { mapOf(it to 0x01) }
    )
    assertForAddressModes(
      DEC,
      target = 0x01,
      expectedState = { with(Z = _1, N = _0) },
      expectedStores = { mapOf(it to 0x00) }
    )
    assertForAddressModes(
      DEC,
      target = 0xFF,
      expectedState = { with(Z = _0, N = _1) },
      expectedStores = { mapOf(it to 0xFE) }
    )
  }

  @Test
  fun dex() {
    assertForAddressModes(
      DEX,
      initState = { with(X = 0x02u) },
      expectedState = { with(X = 0x01u, Z = _0, N = _0) }
    )
    assertForAddressModes(
      DEX,
      initState = { with(X = 0x01u) },
      expectedState = { with(X = 0x00u, Z = _1, N = _0) }
    )
    assertForAddressModes(
      DEX,
      initState = { with(X = 0xFFu) },
      expectedState = { with(X = 0xFEu, Z = _0, N = _1) }
    )
  }

  @Test
  fun dey() {
    assertForAddressModes(
      DEY,
      initState = { with(Y = 0x02u) },
      expectedState = { with(Y = 0x01u, Z = _0, N = _0) }
    )
    assertForAddressModes(
      DEY,
      initState = { with(Y = 0x01u) },
      expectedState = { with(Y = 0x00u, Z = _1, N = _0) }
    )
    assertForAddressModes(
      DEY,
      initState = { with(Y = 0xFFu) },
      expectedState = { with(Y = 0xFEu, Z = _0, N = _1) }
    )
  }

  @Test
  fun inc() {
    assertForAddressModes(
      INC,
      target = 0x01,
      expectedState = { with(Z = _0, N = _0) },
      expectedStores = { mapOf(it to 0x02) }
    )
    assertForAddressModes(
      INC,
      target = 0xFF,
      expectedState = { with(Z = _1, N = _0) },
      expectedStores = { mapOf(it to 0x00) }
    )
    assertForAddressModes(
      INC,
      target = 0xFE,
      expectedState = { with(Z = _0, N = _1) },
      expectedStores = { mapOf(it to 0xFF) }
    )
  }

  @Test
  fun inx() {
    assertForAddressModes(
      INX,
      initState = { with(X = 0x01u) },
      expectedState = { with(X = 0x02u, Z = _0, N = _0) }
    )
    assertForAddressModes(
      INX,
      initState = { with(X = 0xFFu) },
      expectedState = { with(X = 0x00u, Z = _1, N = _0) }
    )
    assertForAddressModes(
      INX,
      initState = { with(X = 0xFEu) },
      expectedState = { with(X = 0xFFu, Z = _0, N = _1) }
    )
  }

  @Test
  fun iny() {
    assertForAddressModes(
      INY,
      initState = { with(Y = 0x01u) },
      expectedState = { with(Y = 0x02u, Z = _0, N = _0) }
    )
    assertForAddressModes(
      INY,
      initState = { with(Y = 0xFFu) },
      expectedState = { with(Y = 0x00u, Z = _1, N = _0) }
    )
    assertForAddressModes(
      INY,
      initState = { with(Y = 0xFEu) },
      expectedState = { with(Y = 0xFFu, Z = _0, N = _1) }
    )
  }

  @Test
  fun asl() {
    assertShift(ASL, 0x02, 0x04) { with(C = _0, Z = _0, N = _0) }
    assertShift(ASL, 0x40, 0x80) { with(C = _0, Z = _0, N = _1) }
    assertShift(ASL, 0x88, 0x10) { with(C = _1, Z = _0, N = _0) }
    assertShift(ASL, 0x80, 0x00) { with(C = _1, Z = _1, N = _0) }
  }

  @Test
  fun lsr() {
    assertShift(LSR, 0x80, 0x40) { with(C = _0, Z = _0, N = _0) }
    assertShift(LSR, 0x01, 0x00) { with(C = _1, Z = _1, N = _0) }
    assertShift(LSR, 0x00, 0x00) { with(C = _0, Z = _1, N = _0) }
  }

  @Test
  fun rol() {
    assertShift(ROL, 0x02, 0x04, { with(C = _0) }) { with(C = _0, Z = _0, N = _0) }
    assertShift(ROL, 0x40, 0x80, { with(C = _0) }) { with(C = _0, Z = _0, N = _1) }
    assertShift(ROL, 0x88, 0x10, { with(C = _0) }) { with(C = _1, Z = _0, N = _0) }
    assertShift(ROL, 0x80, 0x00, { with(C = _0) }) { with(C = _1, Z = _1, N = _0) }
    assertShift(ROL, 0x02, 0x05, { with(C = _1) }) { with(C = _0, Z = _0, N = _0) }
  }

  @Test
  fun ror() {
    assertShift(ROR, 0x80, 0x40, { with(C = _0) }) { with(C = _0, Z = _0, N = _0) }
    assertShift(ROR, 0x01, 0x00, { with(C = _0) }) { with(C = _1, Z = _1, N = _0) }
    assertShift(ROR, 0x00, 0x00, { with(C = _0) }) { with(C = _0, Z = _1, N = _0) }
    assertShift(ROR, 0x80, 0xC0, { with(C = _1) }) { with(C = _0, Z = _0, N = _1) }
  }

  // Accumulator mode is a special case, so handle it separately
  private fun assertShift(
    op: Opcode,
    target: Int,
    expected: Int,
    initState: State.() -> State = { this },
    expectedState: State.() -> State
  ) {
    assertForAddressModes(
      op,
      modes = setOf(ACCUMULATOR),
      initState = { with(A = target.u8()).initState() },
      expectedState = { with(A = expected.u8()).expectedState() }
    )

    assertForAddressModes(
      op,
      modes = OPCODES_TO_ENCODINGS[op]!!.keys - ACCUMULATOR,
      target = target,
      initState = initState,
      expectedState = expectedState,
      expectedStores = { mapOf(it to expected) }
    )
  }
}
