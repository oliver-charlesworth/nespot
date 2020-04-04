package choliver.sixfiveohtwo.cpu

import choliver.sixfiveohtwo.assertForAddressModes
import choliver.sixfiveohtwo.AddrMode.*
import choliver.sixfiveohtwo._0
import choliver.sixfiveohtwo._1
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test

class BitwiseTest {
  // TODO - ASL, LSR, ROL, ROR

  @Test
  fun and() {
    val ops = mapOf(
      IMMEDIATE to 0x29,
      ZERO_PAGE to 0X25,
      ZERO_PAGE_X to 0x35,
      ABSOLUTE to 0x2D,
      ABSOLUTE_X to 0x3D,
      ABSOLUTE_Y to 0x39,
      INDEXED_INDIRECT to 0x21,
      INDIRECT_INDEXED to 0x31
    )

    assertForAddressModes(
      ops,
      operand = 0x23,
      originalState = { with(A = 0x11u) },
      expectedState = { with(A = 0x01u, Z = _0, N = _0) }
    )
    assertForAddressModes(
      ops,
      operand = 0x22,
      originalState = { with(A = 0x11u) },
      expectedState = { with(A = 0x00u, Z = _1, N = _0) }
    )
    assertForAddressModes(
      ops,
      operand = 0x83,
      originalState = { with(A = 0x81u) },
      expectedState = { with(A = 0x81u, Z = _0, N = _1) }
    )
  }

  @Test
  fun ora() {
    val ops = mapOf(
      IMMEDIATE to 0x09,
      ZERO_PAGE to 0X05,
      ZERO_PAGE_X to 0x15,
      ABSOLUTE to 0x0D,
      ABSOLUTE_X to 0x1D,
      ABSOLUTE_Y to 0x19,
      INDEXED_INDIRECT to 0x01,
      INDIRECT_INDEXED to 0x11
    )

    assertForAddressModes(
      ops,
      operand = 0x23,
      originalState = { with(A = 0x11u) },
      expectedState = { with(A = 0x33u, Z = _0, N = _0) }
    )
    assertForAddressModes(
      ops,
      operand = 0x00,
      originalState = { with(A = 0x00u) },
      expectedState = { with(A = 0x00u, Z = _1, N = _0) }
    )
    assertForAddressModes(
      ops,
      operand = 0x83,
      originalState = { with(A = 0x81u) },
      expectedState = { with(A = 0x83u, Z = _0, N = _1) }
    )
  }

  @Test
  fun eor() {
    val ops = mapOf(
      IMMEDIATE to 0x49,
      ZERO_PAGE to 0X45,
      ZERO_PAGE_X to 0x55,
      ABSOLUTE to 0x4D,
      ABSOLUTE_X to 0x5D,
      ABSOLUTE_Y to 0x59,
      INDEXED_INDIRECT to 0x41,
      INDIRECT_INDEXED to 0x51
    )

    assertForAddressModes(
      ops,
      operand = 0x23,
      originalState = { with(A = 0x11u) },
      expectedState = { with(A = 0x32u, Z = _0, N = _0) }
    )
    assertForAddressModes(
      ops,
      operand = 0x11,
      originalState = { with(A = 0x11u) },
      expectedState = { with(A = 0x00u, Z = _1, N = _0) }
    )
    assertForAddressModes(
      ops,
      operand = 0x03,
      originalState = { with(A = 0x81u) },
      expectedState = { with(A = 0x82u, Z = _0, N = _1) }
    )
  }

  @Test
  @Disabled
  fun bit() {
    val ops = mapOf(
      ZERO_PAGE to 0x24,
      ABSOLUTE to 0x2C
    )

    assertForAddressModes(
      ops,
      operand = 0x23,
      originalState = { with(A = 0x11u) },
      expectedState = { with(A = 0x11u, Z = _0, N = _0, V = _0) }
    )
    assertForAddressModes(
      ops,
      operand = 0x22,
      originalState = { with(A = 0x11u) },
      expectedState = { with(A = 0x11u, Z = _1, N = _0, V = _0) }
    )
    assertForAddressModes(
      ops,
      operand = 0x83,
      originalState = { with(A = 0x81u) },
      expectedState = { with(A = 0x81u, Z = _0, N = _1, V = _0) }
    )
    assertForAddressModes(
      ops,
      operand = 0x43,
      originalState = { with(A = 0x41u) },
      expectedState = { with(A = 0x41u, Z = _0, N = _0, V = _1) }
    )
  }
}
