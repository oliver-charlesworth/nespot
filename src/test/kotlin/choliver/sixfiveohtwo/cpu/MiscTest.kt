package choliver.sixfiveohtwo.cpu

import choliver.sixfiveohtwo.assertForAddressModes
import choliver.sixfiveohtwo.model.Opcode.NOP
import org.junit.jupiter.api.Test

class MiscTest {
  @Test
  fun nop() {
    assertForAddressModes(NOP)
  }
}
