package cpu

import choliver.sixfiveohtwo.AddressMode.Immediate
import choliver.sixfiveohtwo.Opcode
import choliver.sixfiveohtwo._0
import choliver.sixfiveohtwo._1
import forOpcode
import org.junit.jupiter.api.Test

class BitwiseTest {
  // TODO - address modes
  // TODO - ASL, LSR, ROL, ROR

  @Test
  fun and() {
    forOpcode(Opcode.AND) {
      assertEquals(s.with(A = 0x01u, Z = _0, N = _0), s.with(A = 0x11u), Immediate(0x23u))
      assertEquals(s.with(A = 0x00u, Z = _1, N = _0), s.with(A = 0x11u), Immediate(0x22u))
      assertEquals(s.with(A = 0x81u, Z = _0, N = _1), s.with(A = 0x81u), Immediate(0x83u))
    }
  }

  @Test
  fun ora() {
    forOpcode(Opcode.ORA) {
      assertEquals(s.with(A = 0x33u, Z = _0, N = _0), s.with(A = 0x11u), Immediate(0x23u))
      assertEquals(s.with(A = 0x00u, Z = _1, N = _0), s.with(A = 0x00u), Immediate(0x00u))
      assertEquals(s.with(A = 0x83u, Z = _0, N = _1), s.with(A = 0x81u), Immediate(0x83u))
    }
  }

  @Test
  fun eor() {
    forOpcode(Opcode.EOR) {
      assertEquals(s.with(A = 0x32u, Z = _0, N = _0), s.with(A = 0x11u), Immediate(0x23u))
      assertEquals(s.with(A = 0x00u, Z = _1, N = _0), s.with(A = 0x11u), Immediate(0x11u))
      assertEquals(s.with(A = 0x82u, Z = _0, N = _1), s.with(A = 0x81u), Immediate(0x03u))
    }
  }
}
