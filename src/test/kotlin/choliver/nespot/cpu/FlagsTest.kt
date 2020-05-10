package choliver.nespot.cpu

import choliver.nespot.cpu.model.Opcode
import choliver.nespot.cpu.model.Opcode.*
import choliver.nespot.cpu.model.Regs
import choliver.nespot.cpu.utils._0
import choliver.nespot.cpu.utils._1
import org.junit.jupiter.api.Test

class FlagsTest {
  @Test
  fun clc() {
    assertFlagModified(CLC, _0) { with(c = it) }
  }

  @Test
  fun cld() {
    assertFlagModified(CLD, _0) { with(d = it) }
  }

  @Test
  fun cli() {
    assertFlagModified(CLI, _0) { with(i = it) }
  }

  @Test
  fun clv() {
    assertFlagModified(CLV, _0) { with(v = it) }
  }

  @Test
  fun sec() {
    assertFlagModified(SEC, _1) { with(c = it) }
  }

  @Test
  fun sed() {
    assertFlagModified(SED, _1) { with(d = it) }
  }

  @Test
  fun sei() {
    assertFlagModified(SEI, _1) { with(i = it) }
  }

  private fun assertFlagModified(op: Opcode, expected: Boolean, regs: Regs.(b: Boolean) -> Regs) {
    assertForAddressModes(op, initRegs = { regs(_1) }, expectedRegs = { regs(expected) })
    assertForAddressModes(op, initRegs = { regs(_0) }, expectedRegs = { regs(expected) })
  }
}
