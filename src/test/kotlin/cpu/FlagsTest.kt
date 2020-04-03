package cpu

import assertForAddressModes
import choliver.sixfiveohtwo.AddrMode.IMPLIED
import choliver.sixfiveohtwo.State
import choliver.sixfiveohtwo._0
import choliver.sixfiveohtwo._1
import org.junit.jupiter.api.Test

class FlagsTest {
  @Test
  fun clc() {
    assertFlagModified(0x18, _0) { with(C = it) }
  }

  @Test
  fun cld() {
    assertFlagModified(0xD8, _0) { with(D = it) }
  }

  @Test
  fun cli() {
    assertFlagModified(0x58, _0) { with(I = it) }
  }

  @Test
  fun clv() {
    assertFlagModified(0xB8, _0) { with(V = it) }
  }

  @Test
  fun sec() {
    assertFlagModified(0x38, _1) { with(C = it) }
  }

  @Test
  fun sed() {
    assertFlagModified(0xF8, _1) { with(D = it) }
  }

  @Test
  fun sei() {
    assertFlagModified(0x78, _1) { with(I = it) }
  }

  private fun assertFlagModified(opcode: Int, expected: Boolean, state: State.(b: Boolean) -> State) {
    assertForAddressModes(mapOf(IMPLIED to opcode), originalState = { state(_1) }, expectedState = { state(expected) })
    assertForAddressModes(mapOf(IMPLIED to opcode), originalState = { state(_0) }, expectedState = { state(expected) })
  }
}
