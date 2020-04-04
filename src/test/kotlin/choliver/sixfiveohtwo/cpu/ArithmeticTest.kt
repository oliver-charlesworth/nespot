package choliver.sixfiveohtwo.cpu

import choliver.sixfiveohtwo.*
import choliver.sixfiveohtwo.AddrMode.*
import choliver.sixfiveohtwo.utils._0
import choliver.sixfiveohtwo.utils._1
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

class ArithmeticTest {
  // TODO - correct PC change
  // TODO - DEC, INC

  @Nested
  inner class Adc {
    private val ops = mapOf(
      IMMEDIATE to 0x69,
      ZERO_PAGE to 0X65,
      ZERO_PAGE_X to 0x75,
      ABSOLUTE to 0x6D,
      ABSOLUTE_X to 0x7D,
      ABSOLUTE_Y to 0x79,
      INDEXED_INDIRECT to 0x61,
      INDIRECT_INDEXED to 0x71
    )

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
        enc(0xA9, a and 0xFF),  // LDA lo(a)
        enc(0x69, b and 0xFF),  // ADC lo(b)
        enc(0x85, 0x00),        // STA #$00
        enc(0xA9, a / 256),     // LDA hi(a)
        enc(0x69, b / 256),     // ADC #$AA
        enc(0x85, 0x01)         // STA #$00
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
        ops,
        operand = operand,
        originalState = { with(A = originalA.u8(), C = _0) },
        expectedState = { with(A = A.u8(), V = V, C = C, N = N, Z = Z) }
      )

      assertForAddressModes(
        ops,
        operand = operand,
        originalState = { with(A = (originalA - 1).u8(), C = _1) },
        expectedState = { with(A = A.u8(), V = V, C = C, N = N, Z = Z) }
      )
    }
  }

  @Nested
  inner class Sbc {
    private val ops = mapOf(
      IMMEDIATE to 0xE9,
      ZERO_PAGE to 0XE5,
      ZERO_PAGE_X to 0xF5,
      ABSOLUTE to 0xED,
      ABSOLUTE_X to 0xFD,
      ABSOLUTE_Y to 0xF9,
      INDEXED_INDIRECT to 0xE1,
      INDIRECT_INDEXED to 0xF1
    )

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
        enc(0x38),              // SEC
        enc(0xA9, a and 0xFF),  // LDA lo(a)
        enc(0xE9, b and 0xFF),  // SBC lo(b)
        enc(0x85, 0x00),        // STA #$00
        enc(0xA9, a / 256),     // LDA hi(a)
        enc(0xE9, b / 256),     // SBC lo(b)
        enc(0x85, 0x01)         // STA #$00
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
        ops,
        operand = operand,
        originalState = { with(A = originalA.u8(), C = _1) },
        expectedState = { with(A = A.u8(), V = V, C = C, N = N, Z = Z) }
      )

      assertForAddressModes(
        ops,
        operand = operand,
        originalState = { with(A = (originalA + 1).u8(), C = _0) },
        expectedState = { with(A = A.u8(), V = V, C = C, N = N, Z = Z) }
      )
    }
  }

  @Nested
  @Disabled // TODO
  inner class Cmp {
    private val ops = mapOf(
      IMMEDIATE to 0xC9,
      ZERO_PAGE to 0XC5,
      ZERO_PAGE_X to 0xD5,
      ABSOLUTE to 0xCD,
      ABSOLUTE_X to 0xDD,
      ABSOLUTE_Y to 0xD9,
      INDEXED_INDIRECT to 0xC1,
      INDIRECT_INDEXED to 0xD1
    )

    // TODO - demonstrate that we ignore carry-in

    @Test
    fun lessThan() {
      assertForAddressModes(
        ops,
        operand = 0x30,
        originalState = { with(A = 0x20u) },
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
  fun dex() {
    val ops = mapOf(IMPLIED to 0xCA)

    assertForAddressModes(
      ops,
      originalState = { with(X = 0x02u) },
      expectedState = { with(X = 0x01u, Z = _0, N = _0) }
    )
    assertForAddressModes(
      ops,
      originalState = { with(X = 0x01u) },
      expectedState = { with(X = 0x00u, Z = _1, N = _0) }
    )
    assertForAddressModes(
      ops,
      originalState = { with(X = 0xFFu) },
      expectedState = { with(X = 0xFEu, Z = _0, N = _1) }
    )
  }

  @Test
  fun dey() {
    val ops = mapOf(IMPLIED to 0x88)

    assertForAddressModes(
      ops,
      originalState = { with(Y = 0x02u) },
      expectedState = { with(Y = 0x01u, Z = _0, N = _0) }
    )
    assertForAddressModes(
      ops,
      originalState = { with(Y = 0x01u) },
      expectedState = { with(Y = 0x00u, Z = _1, N = _0) }
    )
    assertForAddressModes(
      ops,
      originalState = { with(Y = 0xFFu) },
      expectedState = { with(Y = 0xFEu, Z = _0, N = _1) }
    )
  }

  @Test
  fun inx() {
    val ops = mapOf(IMPLIED to 0xE8)

    assertForAddressModes(
      ops,
      originalState = { with(X = 0x01u) },
      expectedState = { with(X = 0x02u, Z = _0, N = _0) }
    )
    assertForAddressModes(
      ops,
      originalState = { with(X = 0xFFu) },
      expectedState = { with(X = 0x00u, Z = _1, N = _0) }
    )
    assertForAddressModes(
      ops,
      originalState = { with(X = 0xFEu) },
      expectedState = { with(X = 0xFFu, Z = _0, N = _1) }
    )
  }

  @Test
  fun iny() {
    val ops = mapOf(IMPLIED to 0xC8)

    assertForAddressModes(
      ops,
      originalState = { with(Y = 0x01u) },
      expectedState = { with(Y = 0x02u, Z = _0, N = _0) }
    )
    assertForAddressModes(
      ops,
      originalState = { with(Y = 0xFFu) },
      expectedState = { with(Y = 0x00u, Z = _1, N = _0) }
    )
    assertForAddressModes(
      ops,
      originalState = { with(Y = 0xFEu) },
      expectedState = { with(Y = 0xFFu, Z = _0, N = _1) }
    )
  }

  private fun assertMultiByte(expected: Int, instructions: List<Array<UInt8>>) {
    val mem = FakeMemory()
    val cpu = Cpu(mem)

    instructions.fold(State()) { state, enc -> cpu.execute(enc, state) }

    assertEquals(
      mapOf(0x0000 to (expected and 0xFF), 0x0001 to (expected / 256)).toMemTypes(),
      mem.stores
    )
  }
}
