package choliver.sixfiveohtwo.cpu

import choliver.sixfiveohtwo.*
import choliver.sixfiveohtwo.AddrMode.ACCUMULATOR
import choliver.sixfiveohtwo.Opcode.*
import choliver.sixfiveohtwo.utils._0
import choliver.sixfiveohtwo.utils._1
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

class ArithmeticTest {
  @Nested
  inner class Adc {
    @Test
    fun resultIsPositive() {
      assertForAddressModesAndCarry(operand = 0x10, originalA = 0x20, A = 0x30, V = _0, C = _0, N = _0, Z = _0)
    }

    @Test
    fun resultIsNegative() {
      assertForAddressModesAndCarry(operand = 0xD0, originalA = 0x20, A = 0xF0, V = _0, C = _0, N = _1, Z = _0)
    }

    @Test
    fun unsignedCarryOutAndPositive() {
      assertForAddressModesAndCarry(operand = 0xD0, originalA = 0x50, A = 0x20, V = _0, C = _1, N = _0, Z = _0)
    }

    @Test
    fun unsignedCarryOutAndNegative() {
      assertForAddressModesAndCarry(operand = 0xD0, originalA = 0xD0, A = 0xA0, V = _0, C = _1, N = _1, Z = _0)
    }

    @Test
    fun overflowToNegative() {
      assertForAddressModesAndCarry(operand = 0x70, originalA = 0x20, A = 0x90, V = _1, C = _0, N = _1, Z = _0)
    }

    @Test
    fun overflowToPositive() {
      assertForAddressModesAndCarry(operand = 0x90, originalA = 0xE0, A = 0x70, V = _1, C = _1, N = _0, Z = _0)
    }

    @Test
    fun resultIsZero() {
      assertForAddressModesAndCarry(operand = 0xF0, originalA = 0x10, A = 0x00, V = _0, C = _1, N = _0, Z = _1)
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
      assertMultiByte(expected, listOf(
        enc(0xA9, a.lo().toInt()),  // LDA lo(a)
        enc(0x69, b.lo().toInt()),  // ADC lo(b)
        enc(0x85, 0x00),            // STA #$00
        enc(0xA9, a.hi().toInt()),  // LDA hi(a)
        enc(0x69, b.hi().toInt()),  // ADC #$AA
        enc(0x85, 0x01)             // STA #$00
      ))
    }

    // Each case implemented twice to demonstrate flag setting respects carry in:
    // (1) basic, (2) carry-in set with (operand + 1)
    private fun assertForAddressModesAndCarry(
      operand: Int,
      originalA: Int,
      A: Int,
      V: Boolean,
      C: Boolean,
      N: Boolean,
      Z: Boolean
    ) {
      assertForAddressModes(
        ADC,
        operand = operand,
        initState = { with(A = originalA.u8(), C = _0) },
        expectedState = { with(A = A.u8(), V = V, C = C, N = N, Z = Z) }
      )

      assertForAddressModes(
        ADC,
        operand = operand,
        initState = { with(A = (originalA - 1).u8(), C = _1) },
        expectedState = { with(A = A.u8(), V = V, C = C, N = N, Z = Z) }
      )
    }
  }

  @Nested
  inner class Sbc {
    @Test
    fun resultIsPositive() {
      assertForAddressModesAndCarry(operand = 0xF0, originalA = 0x20, A = 0x30, V = _0, C = _0, N = _0, Z = _0)
    }

    @Test
    fun resultIsNegative() {
      assertForAddressModesAndCarry(operand = 0x30, originalA = 0x20, A = 0xF0, V = _0, C = _0, N = _1, Z = _0)
    }

    @Test
    fun unsignedCarryOutAndPositive() {
      assertForAddressModesAndCarry(operand = 0x30, originalA = 0x50, A = 0x20, V = _0, C = _1, N = _0, Z = _0)
    }

    @Test
    fun unsignedCarryOutAndNegative() {
      assertForAddressModesAndCarry(operand = 0x30, originalA = 0xD0, A = 0xA0, V = _0, C = _1, N = _1, Z = _0)
    }

    @Test
    fun overflowToNegative() {
      assertForAddressModesAndCarry(operand = 0x90, originalA = 0x20, A = 0x90, V = _1, C = _0, N = _1, Z = _0)
    }

    @Test
    fun overflowToPositive() {
      assertForAddressModesAndCarry(operand = 0x70, originalA = 0xE0, A = 0x70, V = _1, C = _1, N = _0, Z = _0)
    }

    @Test
    fun resultIsZero() {
      assertForAddressModesAndCarry(operand = 0x10, originalA = 0x10, A = 0x00, V = _0, C = _1, N = _0, Z = _1)
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
      assertMultiByte(expected, listOf(
        enc(0x38),                  // SEC
        enc(0xA9, a.lo().toInt()),  // LDA lo(a)
        enc(0xE9, b.lo().toInt()),  // SBC lo(b)
        enc(0x85, 0x00),            // STA #$00
        enc(0xA9, a.hi().toInt()),  // LDA hi(a)
        enc(0xE9, b.hi().toInt()),  // SBC lo(b)
        enc(0x85, 0x01)             // STA #$00
      ))
    }

    // Each case implemented twice to demonstrate flag setting respects borrow in:
    // (1) basic, (2) borrow-in set with (operand + 1)
    private fun assertForAddressModesAndCarry(
      operand: Int,
      originalA: Int,
      A: Int,
      V: Boolean,
      C: Boolean,
      N: Boolean,
      Z: Boolean
    ) {
      assertForAddressModes(
        SBC,
        operand = operand,
        initState = { with(A = originalA.u8(), C = _1) },
        expectedState = { with(A = A.u8(), V = V, C = C, N = N, Z = Z) }
      )

      assertForAddressModes(
        SBC,
        operand = operand,
        initState = { with(A = (originalA + 1).u8(), C = _0) },
        expectedState = { with(A = A.u8(), V = V, C = C, N = N, Z = Z) }
      )
    }
  }

  @Nested
  @Disabled // TODO
  inner class Cmp {
    // TODO - demonstrate that we ignore carry-in

    @Test
    fun lessThan() {
      assertForAddressModes(
        CMP,
        operand = 0x30,
        initState = { with(A = 0x20u) },
        expectedState = { with(A = 0x20u, C = _0, N = _0, Z = _0) }
      )
    }

//    @Test
//    fun resultIsNegative() {
//      assertForAddressModesAndCarry(operand = 0x30, originalA = 0x20, A = 0xF0, V = _0, C = _0, N = _1, Z = _0)
//    }
//
//    @Test
//    fun unsignedCarryOutAndPositive() {
//      assertForAddressModesAndCarry(operand = 0x30, originalA = 0x50, A = 0x20, V = _0, C = _1, N = _0, Z = _0)
//    }
//
//    @Test
//    fun unsignedCarryOutAndNegative() {
//      assertForAddressModesAndCarry(operand = 0x30, originalA = 0xD0, A = 0xA0, V = _0, C = _1, N = _1, Z = _0)
//    }
//
//    @Test
//    fun overflowToNegative() {
//      assertForAddressModesAndCarry(operand = 0x90, originalA = 0x20, A = 0x90, V = _1, C = _0, N = _1, Z = _0)
//    }
//
//    @Test
//    fun overflowToPositive() {
//      assertForAddressModesAndCarry(operand = 0x70, originalA = 0xE0, A = 0x70, V = _1, C = _1, N = _0, Z = _0)
//    }
//
//    @Test
//    fun resultIsZero() {
//      assertForAddressModesAndCarry(operand = 0x10, originalA = 0x10, A = 0x00, V = _0, C = _1, N = _0, Z = _1)
//    }
  }

  // TODO - CPX, CPY

  @Test
  fun dec() {
    assertForAddressModes(
      DEC,
      operand = 0x02,
      expectedState = { with(Z = _0, N = _0) },
      expectedStores = { mapOf(it to 0x01) }
    )
    assertForAddressModes(
      DEC,
      operand = 0x01,
      expectedState = { with(Z = _1, N = _0) },
      expectedStores = { mapOf(it to 0x00) }
    )
    assertForAddressModes(
      DEC,
      operand = 0xFF,
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
      operand = 0x01,
      expectedState = { with(Z = _0, N = _0) },
      expectedStores = { mapOf(it to 0x02) }
    )
    assertForAddressModes(
      INC,
      operand = 0xFF,
      expectedState = { with(Z = _1, N = _0) },
      expectedStores = { mapOf(it to 0x00) }
    )
    assertForAddressModes(
      INC,
      operand = 0xFE,
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
    operand: Int,
    expected: Int,
    initState: State.() -> State = { this },
    expectedState: State.() -> State
  ) {
    assertForAddressModes(
      mapOf(ACCUMULATOR to op.encodings[ACCUMULATOR]!!),
      initState = { with(A = operand.u8()).initState() },
      expectedState = { with(A = expected.u8()).expectedState() }
    )

    assertForAddressModes(
      op.encodings - ACCUMULATOR,
      operand = operand,
      initState = initState,
      expectedState = expectedState,
      expectedStores = { mapOf(it to expected) }
    )
  }

  private fun assertMultiByte(expected: Int, instructions: List<Array<UInt8>>) {
    val mem = FakeMemory()
    val cpu = Cpu(mem)

    instructions.fold(State()) { state, enc -> cpu.execute(enc, state) }

    mem.assertStores(mapOf(0x0000 to expected.lo().toInt(), 0x0001 to expected.hi().toInt()))
  }
}
