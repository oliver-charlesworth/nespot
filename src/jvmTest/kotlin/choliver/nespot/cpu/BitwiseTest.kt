package choliver.nespot.cpu

import choliver.nespot.common._0
import choliver.nespot.common._1
import choliver.nespot.cpu.Opcode.*
import org.junit.jupiter.api.Test

class BitwiseTest {
  @Test
  fun and() {
    assertForAddressModes(
      AND,
      target = 0x23,
      initRegs = { with(a = 0x11) },
      expectedRegs = { with(a = 0x01, z = _0, n = _0) }
    )
    assertForAddressModes(
      AND,
      target = 0x22,
      initRegs = { with(a = 0x11) },
      expectedRegs = { with(a = 0x00, z = _1, n = _0) }
    )
    assertForAddressModes(
      AND,
      target = 0x83,
      initRegs = { with(a = 0x81) },
      expectedRegs = { with(a = 0x81, z = _0, n = _1) }
    )
  }

  @Test
  fun ora() {
    assertForAddressModes(
      ORA,
      target = 0x23,
      initRegs = { with(a = 0x11) },
      expectedRegs = { with(a = 0x33, z = _0, n = _0) }
    )
    assertForAddressModes(
      ORA,
      target = 0x00,
      initRegs = { with(a = 0x00) },
      expectedRegs = { with(a = 0x00, z = _1, n = _0) }
    )
    assertForAddressModes(
      ORA,
      target = 0x83,
      initRegs = { with(a = 0x81) },
      expectedRegs = { with(a = 0x83, z = _0, n = _1) }
    )
  }

  @Test
  fun eor() {
    assertForAddressModes(
      EOR,
      target = 0x23,
      initRegs = { with(a = 0x11) },
      expectedRegs = { with(a = 0x32, z = _0, n = _0) }
    )
    assertForAddressModes(
      EOR,
      target = 0x11,
      initRegs = { with(a = 0x11) },
      expectedRegs = { with(a = 0x00, z = _1, n = _0) }
    )
    assertForAddressModes(
      EOR,
      target = 0x03,
      initRegs = { with(a = 0x81) },
      expectedRegs = { with(a = 0x82, z = _0, n = _1) }
    )
  }

  @Test
  fun bit() {
    assertForAddressModes(
      BIT,
      target = 0x23,
      initRegs = { with(a = 0x11) },
      expectedRegs = { with(a = 0x11, z = _0, n = _0, v = _0) }
    )
    assertForAddressModes(
      BIT,
      target = 0x22,
      initRegs = { with(a = 0x11) },
      expectedRegs = { with(a = 0x11, z = _1, n = _0, v = _0) }
    )
    assertForAddressModes(
      BIT,
      target = 0x83,
      initRegs = { with(a = 0x01) },
      expectedRegs = { with(a = 0x01, z = _0, n = _1, v = _0) }
    )
    assertForAddressModes(
      BIT,
      target = 0x43,
      initRegs = { with(a = 0x01) },
      expectedRegs = { with(a = 0x01, z = _0, n = _0, v = _1) }
    )
  }
}
