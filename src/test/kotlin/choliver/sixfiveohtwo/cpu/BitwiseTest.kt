package choliver.sixfiveohtwo.cpu

import choliver.sixfiveohtwo.model.Opcode.*
import choliver.sixfiveohtwo.assertForAddressModes
import choliver.sixfiveohtwo.utils._0
import choliver.sixfiveohtwo.utils._1
import org.junit.jupiter.api.Test

class BitwiseTest {
  // TODO - ASL, LSR, ROL, ROR

  @Test
  fun and() {
    assertForAddressModes(
      AND,
      target = 0x23,
      initState = { with(A = 0x11u) },
      expectedState = { with(A = 0x01u, Z = _0, N = _0) }
    )
    assertForAddressModes(
      AND,
      target = 0x22,
      initState = { with(A = 0x11u) },
      expectedState = { with(A = 0x00u, Z = _1, N = _0) }
    )
    assertForAddressModes(
      AND,
      target = 0x83,
      initState = { with(A = 0x81u) },
      expectedState = { with(A = 0x81u, Z = _0, N = _1) }
    )
  }

  @Test
  fun ora() {
    assertForAddressModes(
      ORA,
      target = 0x23,
      initState = { with(A = 0x11u) },
      expectedState = { with(A = 0x33u, Z = _0, N = _0) }
    )
    assertForAddressModes(
      ORA,
      target = 0x00,
      initState = { with(A = 0x00u) },
      expectedState = { with(A = 0x00u, Z = _1, N = _0) }
    )
    assertForAddressModes(
      ORA,
      target = 0x83,
      initState = { with(A = 0x81u) },
      expectedState = { with(A = 0x83u, Z = _0, N = _1) }
    )
  }

  @Test
  fun eor() {
    assertForAddressModes(
      EOR,
      target = 0x23,
      initState = { with(A = 0x11u) },
      expectedState = { with(A = 0x32u, Z = _0, N = _0) }
    )
    assertForAddressModes(
      EOR,
      target = 0x11,
      initState = { with(A = 0x11u) },
      expectedState = { with(A = 0x00u, Z = _1, N = _0) }
    )
    assertForAddressModes(
      EOR,
      target = 0x03,
      initState = { with(A = 0x81u) },
      expectedState = { with(A = 0x82u, Z = _0, N = _1) }
    )
  }

  @Test
  fun bit() {
    assertForAddressModes(
      BIT,
      target = 0x23,
      initState = { with(A = 0x11u) },
      expectedState = { with(A = 0x11u, Z = _0, N = _0, V = _0) }
    )
    assertForAddressModes(
      BIT,
      target = 0x22,
      initState = { with(A = 0x11u) },
      expectedState = { with(A = 0x11u, Z = _1, N = _0, V = _0) }
    )
    assertForAddressModes(
      BIT,
      target = 0x83,
      initState = { with(A = 0x81u) },
      expectedState = { with(A = 0x81u, Z = _0, N = _1, V = _0) }
    )
    assertForAddressModes(
      BIT,
      target = 0x43,
      initState = { with(A = 0x41u) },
      expectedState = { with(A = 0x41u, Z = _0, N = _0, V = _1) }
    )
  }
}
