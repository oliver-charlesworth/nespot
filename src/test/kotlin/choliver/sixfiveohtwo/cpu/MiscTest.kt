package choliver.sixfiveohtwo.cpu

import choliver.sixfiveohtwo.Opcode.NOP
import choliver.sixfiveohtwo.assertForAddressModes
import org.junit.jupiter.api.Test

class MiscTest {
  @Test
  fun nop() {
    assertForAddressModes(NOP)
  }
}
