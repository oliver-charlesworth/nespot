package choliver.nes.sixfiveohtwo.cpu

import choliver.nes.sixfiveohtwo.assertForAddressModes
import choliver.nes.sixfiveohtwo.model.Opcode.*
import choliver.nes.sixfiveohtwo.utils._0
import choliver.nes.sixfiveohtwo.utils._1
import org.junit.jupiter.api.Test

class BitwiseTest {
  @Test
  fun and() {
    assertForAddressModes(
      AND,
      target = 0x23,
      initState = { with(A = 0x11) },
      expectedState = { with(A = 0x01, Z = _0, N = _0) }
    )
    assertForAddressModes(
      AND,
      target = 0x22,
      initState = { with(A = 0x11) },
      expectedState = { with(A = 0x00, Z = _1, N = _0) }
    )
    assertForAddressModes(
      AND,
      target = 0x83,
      initState = { with(A = 0x81) },
      expectedState = { with(A = 0x81, Z = _0, N = _1) }
    )
  }

  @Test
  fun ora() {
    assertForAddressModes(
      ORA,
      target = 0x23,
      initState = { with(A = 0x11) },
      expectedState = { with(A = 0x33, Z = _0, N = _0) }
    )
    assertForAddressModes(
      ORA,
      target = 0x00,
      initState = { with(A = 0x00) },
      expectedState = { with(A = 0x00, Z = _1, N = _0) }
    )
    assertForAddressModes(
      ORA,
      target = 0x83,
      initState = { with(A = 0x81) },
      expectedState = { with(A = 0x83, Z = _0, N = _1) }
    )
  }

  @Test
  fun eor() {
    assertForAddressModes(
      EOR,
      target = 0x23,
      initState = { with(A = 0x11) },
      expectedState = { with(A = 0x32, Z = _0, N = _0) }
    )
    assertForAddressModes(
      EOR,
      target = 0x11,
      initState = { with(A = 0x11) },
      expectedState = { with(A = 0x00, Z = _1, N = _0) }
    )
    assertForAddressModes(
      EOR,
      target = 0x03,
      initState = { with(A = 0x81) },
      expectedState = { with(A = 0x82, Z = _0, N = _1) }
    )
  }

  @Test
  fun bit() {
    assertForAddressModes(
      BIT,
      target = 0x23,
      initState = { with(A = 0x11) },
      expectedState = { with(A = 0x11, Z = _0, N = _0, V = _0) }
    )
    assertForAddressModes(
      BIT,
      target = 0x22,
      initState = { with(A = 0x11) },
      expectedState = { with(A = 0x11, Z = _1, N = _0, V = _0) }
    )
    assertForAddressModes(
      BIT,
      target = 0x83,
      initState = { with(A = 0x81) },
      expectedState = { with(A = 0x81, Z = _0, N = _1, V = _0) }
    )
    assertForAddressModes(
      BIT,
      target = 0x43,
      initState = { with(A = 0x41) },
      expectedState = { with(A = 0x41, Z = _0, N = _0, V = _1) }
    )
  }
}
