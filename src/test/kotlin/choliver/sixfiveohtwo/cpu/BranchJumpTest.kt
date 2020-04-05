package choliver.sixfiveohtwo.cpu

import choliver.sixfiveohtwo.*
import choliver.sixfiveohtwo.AddrMode.*
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

  @Test
  fun jsr() {
    assertForAddressModes(
      ops = mapOf(ABSOLUTE to 0x20),
      initState = { with(S = 0x30u) },
      expectedState = { with(PC = SCARY_ADDR.u16(), S = 0x2Eu) },
      expectedStores = { mem16(0x012F, INIT_PC + 2) } // JSR stores *last* byte of instruction
    )
  }

  @Test
  fun rts() {
    assertForAddressModes(
      ops = mapOf(IMPLIED to 0x60),
      initState = { with(S = 0x2Eu) },
      initStores = mem16(0x012F, SCARY_ADDR - 1), // JSR stores *last* byte of instruction
      expectedState = { with(PC = SCARY_ADDR.u16(), S = 0x30u) }
    )
  }
}
