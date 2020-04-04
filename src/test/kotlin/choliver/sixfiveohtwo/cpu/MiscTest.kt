package choliver.sixfiveohtwo.cpu

import choliver.sixfiveohtwo.assertForAddressModes
import choliver.sixfiveohtwo.AddrMode.IMPLIED
import org.junit.jupiter.api.Test

class MiscTest {
  @Test
  fun nop() {
    assertForAddressModes(mapOf(IMPLIED to 0xEA))
  }
}
