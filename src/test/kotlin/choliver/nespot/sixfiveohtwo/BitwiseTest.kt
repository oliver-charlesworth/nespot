package choliver.nespot.sixfiveohtwo

import choliver.nespot.sixfiveohtwo.assertForAddressModes
import choliver.nespot.sixfiveohtwo.model.Opcode.*
import choliver.nespot.sixfiveohtwo.utils._0
import choliver.nespot.sixfiveohtwo.utils._1
import org.junit.jupiter.api.Test

class BitwiseTest {
  @Test
  fun and() {
    assertForAddressModes(
      AND,
      target = 0x23,
      initState = { with(a = 0x11) },
      expectedState = { with(a = 0x01, z = _0, n = _0) }
    )
    assertForAddressModes(
      AND,
      target = 0x22,
      initState = { with(a = 0x11) },
      expectedState = { with(a = 0x00, z = _1, n = _0) }
    )
    assertForAddressModes(
      AND,
      target = 0x83,
      initState = { with(a = 0x81) },
      expectedState = { with(a = 0x81, z = _0, n = _1) }
    )
  }

  @Test
  fun ora() {
    assertForAddressModes(
      ORA,
      target = 0x23,
      initState = { with(a = 0x11) },
      expectedState = { with(a = 0x33, z = _0, n = _0) }
    )
    assertForAddressModes(
      ORA,
      target = 0x00,
      initState = { with(a = 0x00) },
      expectedState = { with(a = 0x00, z = _1, n = _0) }
    )
    assertForAddressModes(
      ORA,
      target = 0x83,
      initState = { with(a = 0x81) },
      expectedState = { with(a = 0x83, z = _0, n = _1) }
    )
  }

  @Test
  fun eor() {
    assertForAddressModes(
      EOR,
      target = 0x23,
      initState = { with(a = 0x11) },
      expectedState = { with(a = 0x32, z = _0, n = _0) }
    )
    assertForAddressModes(
      EOR,
      target = 0x11,
      initState = { with(a = 0x11) },
      expectedState = { with(a = 0x00, z = _1, n = _0) }
    )
    assertForAddressModes(
      EOR,
      target = 0x03,
      initState = { with(a = 0x81) },
      expectedState = { with(a = 0x82, z = _0, n = _1) }
    )
  }

  @Test
  fun bit() {
    assertForAddressModes(
      BIT,
      target = 0x23,
      initState = { with(a = 0x11) },
      expectedState = { with(a = 0x11, z = _0, n = _0, v = _0) }
    )
    assertForAddressModes(
      BIT,
      target = 0x22,
      initState = { with(a = 0x11) },
      expectedState = { with(a = 0x11, z = _1, n = _0, v = _0) }
    )
    assertForAddressModes(
      BIT,
      target = 0x83,
      initState = { with(a = 0x01) },
      expectedState = { with(a = 0x01, z = _0, n = _1, v = _0) }
    )
    assertForAddressModes(
      BIT,
      target = 0x43,
      initState = { with(a = 0x01) },
      expectedState = { with(a = 0x01, z = _0, n = _0, v = _1) }
    )
  }
}
