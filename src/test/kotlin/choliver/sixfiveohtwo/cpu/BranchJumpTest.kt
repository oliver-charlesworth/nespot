package choliver.sixfiveohtwo.cpu

import choliver.sixfiveohtwo.*
import choliver.sixfiveohtwo.Opcode.*
import choliver.sixfiveohtwo.utils._0
import choliver.sixfiveohtwo.utils._1
import org.junit.jupiter.api.Test

class BranchJumpTest {

  @Test
  fun jmp() {
    assertForAddressModes(
      JMP,
      expectedState = { with(PC = SCARY_ADDR.u16()) }
    )
  }

  @Test
  fun jsr() {
    assertForAddressModes(
      JSR,
      initState = { with(S = 0x30u) },
      expectedState = { with(PC = SCARY_ADDR.u16(), S = 0x2Eu) },
      expectedStores = { mem16(0x012F, INIT_PC + 2) } // JSR stores *last* byte of instruction
    )
  }

  @Test
  fun rts() {
    assertForAddressModes(
      RTS,
      initState = { with(S = 0x2Eu) },
      initStores = mem16(0x012F, SCARY_ADDR - 1), // JSR stores *last* byte of instruction
      expectedState = { with(PC = SCARY_ADDR.u16(), S = 0x30u) }
    )
  }

  @Test
  fun bpl() {
    assertBranch(BPL) { with(N = !it) }
  }

  @Test
  fun bmi() {
    assertBranch(BMI) { with(N = it) }
  }

  @Test
  fun bvc() {
    assertBranch(BVC) { with(V = !it) }
  }

  @Test
  fun bvs() {
    assertBranch(BVS) { with(V = it) }
  }

  @Test
  fun bcc() {
    assertBranch(BCC) { with(C = !it) }
  }

  @Test
  fun bcs() {
    assertBranch(BCS) { with(C = it) }
  }

  @Test
  fun bne() {
    assertBranch(BNE) { with(Z = !it) }
  }

  @Test
  fun beq() {
    assertBranch(BEQ) { with(Z = it) }
  }

  private fun assertBranch(op: Opcode, state: State.(b: Boolean) -> State) {
    assertForAddressModes(
      op,
      operand = 0x30,
      initState = { state(_0) },
      expectedState = { state(_0) }
    )
    assertForAddressModes(
      op,
      operand = 0x30,
      initState = { state(_1) },
      expectedState = { state(_1).with(PC = (INIT_PC + 0x30).u16()) }
    )
    assertForAddressModes(
      op,
      operand = 0xD0,
      initState = { state(_1) },
      expectedState = { state(_1).with(PC = (INIT_PC - 0x30).u16()) }
    )
  }
}
