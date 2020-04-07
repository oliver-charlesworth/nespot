package choliver.sixfiveohtwo.cpu

import choliver.sixfiveohtwo.assertForAddressModes
import choliver.sixfiveohtwo.model.Opcode
import choliver.sixfiveohtwo.model.Opcode.*
import choliver.sixfiveohtwo.model.State
import choliver.sixfiveohtwo.utils._0
import choliver.sixfiveohtwo.utils._1
import org.junit.jupiter.api.Test

class FlagsTest {
  @Test
  fun clc() {
    assertFlagModified(CLC, _0) { with(C = it) }
  }

  @Test
  fun cld() {
    assertFlagModified(CLD, _0) { with(D = it) }
  }

  @Test
  fun cli() {
    assertFlagModified(CLI, _0) { with(I = it) }
  }

  @Test
  fun clv() {
    assertFlagModified(CLV, _0) { with(V = it) }
  }

  @Test
  fun sec() {
    assertFlagModified(SEC, _1) { with(C = it) }
  }

  @Test
  fun sed() {
    assertFlagModified(SED, _1) { with(D = it) }
  }

  @Test
  fun sei() {
    assertFlagModified(SEI, _1) { with(I = it) }
  }

  private fun assertFlagModified(op: Opcode, expected: Boolean, state: State.(b: Boolean) -> State) {
    assertForAddressModes(op, initState = { state(_1) }, expectedState = { state(expected) })
    assertForAddressModes(op, initState = { state(_0) }, expectedState = { state(expected) })
  }
}
