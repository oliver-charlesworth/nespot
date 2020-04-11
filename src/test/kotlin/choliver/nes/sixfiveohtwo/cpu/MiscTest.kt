package choliver.nes.sixfiveohtwo.cpu

import choliver.nes.sixfiveohtwo.assertForAddressModes
import choliver.nes.sixfiveohtwo.model.Opcode.NOP
import org.junit.jupiter.api.Test

class MiscTest {
  @Test
  fun nop() {
    assertForAddressModes(NOP)
  }
}
