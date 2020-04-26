package choliver.nespot.sixfiveohtwo.instructions

import choliver.nespot.sixfiveohtwo.assertForAddressModes
import choliver.nespot.sixfiveohtwo.model.Opcode.NOP
import org.junit.jupiter.api.Test

class MiscTest {
  @Test
  fun nop() {
    assertForAddressModes(NOP)
  }
}
