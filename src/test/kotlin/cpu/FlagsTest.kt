package cpu

import choliver.sixfiveohtwo.Opcode
import choliver.sixfiveohtwo._0
import choliver.sixfiveohtwo._1
import forOpcode
import org.junit.jupiter.api.Test

class FlagsTest {
  @Test
  fun clc() {
    forOpcode(Opcode.CLC) {
      assertEquals(s.with(C = _0), s.with(C = _1))
      assertEquals(s.with(C = _0), s.with(C = _0))
    }
  }

  @Test
  fun cld() {
    forOpcode(Opcode.CLD) {
      assertEquals(s.with(D = _0), s.with(D = _1))
      assertEquals(s.with(D = _0), s.with(D = _0))
    }
  }

  @Test
  fun cli() {
    forOpcode(Opcode.CLI) {
      assertEquals(s.with(I = _0), s.with(I = _1))
      assertEquals(s.with(I = _0), s.with(I = _0))
    }
  }

  @Test
  fun clv() {
    forOpcode(Opcode.CLV) {
      assertEquals(s.with(V = _0), s.with(V = _1))
      assertEquals(s.with(V = _0), s.with(V = _0))
    }
  }

  @Test
  fun sec() {
    forOpcode(Opcode.SEC) {
      assertEquals(s.with(C = _1), s.with(C = _1))
      assertEquals(s.with(C = _1), s.with(C = _0))
    }
  }

  @Test
  fun sed() {
    forOpcode(Opcode.SED) {
      assertEquals(s.with(D = _1), s.with(D = _1))
      assertEquals(s.with(D = _1), s.with(D = _0))
    }
  }

  @Test
  fun sei() {
    forOpcode(Opcode.SEI) {
      assertEquals(s.with(I = _1), s.with(I = _1))
      assertEquals(s.with(I = _1), s.with(I = _0))
    }
  }
}
