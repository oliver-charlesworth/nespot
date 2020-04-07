package choliver.sixfiveohtwo.cpu

import choliver.sixfiveohtwo.*
import choliver.sixfiveohtwo.model.Opcode
import choliver.sixfiveohtwo.model.Opcode.*
import choliver.sixfiveohtwo.model.State
import choliver.sixfiveohtwo.model.toPC
import choliver.sixfiveohtwo.utils._0
import choliver.sixfiveohtwo.utils._1
import org.junit.jupiter.api.Test

class BranchJumpTest {

  @Test
  fun jmp() {
    assertForAddressModes(
      JMP,
      expectedState = { with(PC = SCARY_ADDR.toPC()) }
    )
  }

  @Test
  fun jsr() {
    assertForAddressModes(
      JSR,
      initState = { with(S = 0x30u) },
      expectedState = { with(PC = SCARY_ADDR.toPC(), S = 0x2Eu) },
      expectedStores = { mem16(0x012F, BASE_USER + 2) } // JSR stores *last* byte of instruction
    )
  }

  @Test
  fun rts() {
    assertForAddressModes(
      RTS,
      initState = { with(S = 0x2Eu) },
      initStores = mem16(0x012F, SCARY_ADDR - 1), // JSR stores *last* byte of instruction
      expectedState = { with(PC = SCARY_ADDR.toPC(), S = 0x30u) }
    )
  }

  @Test
  fun rti() {
    assertForAddressModes(
      RTI,
      initState = { with(S = 0x2Du) },
      initStores = mapOf(0x12E to 0xCF) +
        mem16(0x012F, SCARY_ADDR),
      expectedState = { with(PC = SCARY_ADDR.toPC(), S = 0x30u, N = _1, V = _1, D = _1, I = _1, Z = _1, C = _1) }
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
      target = 0x30,
      initState = { state(_0) },
      expectedState = { state(_0) }
    )
    assertForAddressModes(
      op,
      target = 0x30,
      initState = { state(_1) },
      expectedState = { state(_1).with(PC = BASE_USER.toPC() + 0x30) }
    )
    assertForAddressModes(
      op,
      target = 0xD0,
      initState = { state(_1) },
      expectedState = { state(_1).with(PC = BASE_USER.toPC() - 0x30) }
    )
  }
}
