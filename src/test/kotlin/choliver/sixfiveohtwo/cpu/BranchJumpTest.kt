package choliver.sixfiveohtwo.cpu

import choliver.sixfiveohtwo.AddrMode.ABSOLUTE
import choliver.sixfiveohtwo.AddrMode.INDIRECT
import choliver.sixfiveohtwo.SCARY_ADDR
import choliver.sixfiveohtwo.assertForAddressModes
import choliver.sixfiveohtwo.u16
import org.junit.jupiter.api.Test

class BranchJumpTest {

  @Test
  fun jmp() {
    assertForAddressModes(
      ops = mapOf(
        ABSOLUTE to 0x4C,
        INDIRECT to 0x6C
      ),
      expectedState = { with(PC = SCARY_ADDR.u16()) }
    )
  }
}
