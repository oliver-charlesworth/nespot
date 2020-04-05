package choliver.sixfiveohtwo.cpu

import choliver.sixfiveohtwo.*
import choliver.sixfiveohtwo.AddrMode.*
import choliver.sixfiveohtwo.utils._0
import choliver.sixfiveohtwo.utils._1
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

  @Test
  fun bpl() {
    assertBranch(0x10) { with(N = !it) }
  }

  @Test
  fun bmi() {
    assertBranch(0x30) { with(N = it) }
  }

  @Test
  fun bvc() {
    assertBranch(0x50) { with(V = !it) }
  }

  @Test
  fun bvs() {
    assertBranch(0x70) { with(V = it) }
  }

  @Test
  fun bcc() {
    assertBranch(0x90) { with(C = !it) }
  }

  @Test
  fun bcs() {
    assertBranch(0xB0) { with(C = it) }
  }

  @Test
  fun bne() {
    assertBranch(0xD0) { with(Z = !it) }
  }

  @Test
  fun beq() {
    assertBranch(0xF0) { with(Z = it) }
  }

  private fun assertBranch(opcode: Int, state: State.(b: Boolean) -> State) {
    assertForAddressModes(
      ops = mapOf(RELATIVE to opcode),
      operand = 0x30,
      initState = { state(_0) },
      expectedState = { state(_0) }
    )
    assertForAddressModes(
      ops = mapOf(RELATIVE to opcode),
      operand = 0x30,
      initState = { state(_1) },
      expectedState = { state(_1).with(PC = (INIT_PC + 0x30).u16()) }
    )
    assertForAddressModes(
      ops = mapOf(RELATIVE to opcode),
      operand = 0xD0,
      initState = { state(_1) },
      expectedState = { state(_1).with(PC = (INIT_PC - 0x30).u16()) }
    )
  }
}
