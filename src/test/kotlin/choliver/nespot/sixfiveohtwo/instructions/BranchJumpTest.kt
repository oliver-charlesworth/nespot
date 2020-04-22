package choliver.nespot.sixfiveohtwo.instructions

import choliver.nespot.sixfiveohtwo.BASE_USER
import choliver.nespot.sixfiveohtwo.Cpu.Companion.VECTOR_IRQ
import choliver.nespot.sixfiveohtwo.SCARY_ADDR
import choliver.nespot.sixfiveohtwo.addrToMem
import choliver.nespot.sixfiveohtwo.assertForAddressModes
import choliver.nespot.sixfiveohtwo.model.Opcode
import choliver.nespot.sixfiveohtwo.model.Opcode.*
import choliver.nespot.sixfiveohtwo.model.State
import choliver.nespot.sixfiveohtwo.utils._0
import choliver.nespot.sixfiveohtwo.utils._1
import org.junit.jupiter.api.Test

class BranchJumpTest {

  @Test
  fun jmp() {
    assertForAddressModes(
      JMP,
      expectedState = { with(PC = SCARY_ADDR) }
    )
  }

  @Test
  fun jsr() {
    assertForAddressModes(
      JSR,
      initState = { with(S = 0x30) },
      expectedState = { with(PC = SCARY_ADDR, S = 0x2E) },
      expectedStores = { addrToMem(0x012F, BASE_USER + 2) } // JSR stores *last* byte of instruction
    )
  }

  @Test
  fun brk() {
    assertForAddressModes(
      BRK,
      initState = { with(S = 0x30, N = _1, V = _1, D = _1, I = _1, Z = _1, C = _1) },
      initStores = addrToMem(VECTOR_IRQ, SCARY_ADDR),
      expectedState = { with(PC = SCARY_ADDR, S = 0x2D, N = _1, V = _1, D = _1, I = _1, Z = _1, C = _1) },
      expectedStores = {
        addrToMem(0x012F, BASE_USER + 2) + // BRK stores PC+2
        mapOf(0x12E to 0xDF)  // Note B is also set on stack
      }
    )
  }

  @Test
  fun rts() {
    assertForAddressModes(
      RTS,
      initState = { with(S = 0x2E) },
      initStores = addrToMem(0x012F, SCARY_ADDR - 1), // JSR stores *last* byte of instruction
      expectedState = { with(PC = SCARY_ADDR, S = 0x30) }
    )
  }

  @Test
  fun rti() {
    assertForAddressModes(
      RTI,
      initState = { with(S = 0x2D) },
      initStores = mapOf(0x12E to 0xCF) + addrToMem(0x012F, SCARY_ADDR),
      expectedState = { with(PC = SCARY_ADDR, S = 0x30, N = _1, V = _1, D = _1, I = _1, Z = _1, C = _1) }
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
      expectedState = { state(_1).with(PC = BASE_USER + 2 + 0x30) }  // Offset from *next* instruction
    )
    assertForAddressModes(
      op,
      target = 0xD0,
      initState = { state(_1) },
      expectedState = { state(_1).with(PC = BASE_USER + 2 - 0x30) }  // Offset from *next* instruction
    )
  }
}
