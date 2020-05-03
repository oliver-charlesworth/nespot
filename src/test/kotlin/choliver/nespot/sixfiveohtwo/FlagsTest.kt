package choliver.nespot.sixfiveohtwo

import choliver.nespot.sixfiveohtwo.model.Opcode
import choliver.nespot.sixfiveohtwo.model.Opcode.*
import choliver.nespot.sixfiveohtwo.model.State
import choliver.nespot.sixfiveohtwo.utils._0
import choliver.nespot.sixfiveohtwo.utils._1
import org.junit.jupiter.api.Test

class FlagsTest {
  @Test
  fun clc() {
    assertFlagModified(CLC, _0) { with(c =  it) }
  }

  @Test
  fun cld() {
    assertFlagModified(CLD, _0) { with(d =  it) }
  }

  @Test
  fun cli() {
    assertFlagModified(CLI, _0) { with(i =  it) }
  }

  @Test
  fun clv() {
    assertFlagModified(CLV, _0) { with(v =  it) }
  }

  @Test
  fun sec() {
    assertFlagModified(SEC, _1) { with(c =  it) }
  }

  @Test
  fun sed() {
    assertFlagModified(SED, _1) { with(d =  it) }
  }

  @Test
  fun sei() {
    assertFlagModified(SEI, _1) { with(i =  it) }
  }

  private fun assertFlagModified(op: Opcode, expected: Boolean, state: State.(b: Boolean) -> State) {
    assertForAddressModes(op, initState = { state(_1) }, expectedState = { state(expected) })
    assertForAddressModes(op, initState = { state(_0) }, expectedState = { state(expected) })
  }
}
