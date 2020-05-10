package choliver.nespot.cpu

import choliver.nespot.cpu.model.Opcode.NOP
import org.junit.jupiter.api.Test

class MiscTest {
  @Test
  fun nop() {
    assertForAddressModes(NOP)
  }
}
