package choliver.nespot.sixfiveohtwo

import choliver.nespot.sixfiveohtwo.Cpu.Companion.VECTOR_IRQ
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
      expectedState = { with(pc =   SCARY_ADDR) }
    )
  }

  @Test
  fun jsr() {
    assertForAddressModes(
      JSR,
      initState = { with(s =  0x30) },
      expectedState = { with(pc =   SCARY_ADDR, s =  0x2E) },
      expectedStores = { addrToMem(0x012F, BASE_USER + 2) } // JSR stores *last* byte of instruction
    )
  }

  @Test
  fun brk() {
    assertForAddressModes(
      BRK,
      initState = { with(s =  0x30, n =  _1, v =  _1, d =  _1, i =  _1, z =  _1, c =  _1) },
      initStores = addrToMem(VECTOR_IRQ, SCARY_ADDR),
      expectedState = { with(pc =   SCARY_ADDR, s =  0x2D, n =  _1, v =  _1, d =  _1, i =  _1, z =  _1, c =  _1) },
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
      initState = { with(s =  0x2E) },
      initStores = addrToMem(0x012F, SCARY_ADDR - 1), // JSR stores *last* byte of instruction
      expectedState = { with(pc =   SCARY_ADDR, s =  0x30) }
    )
  }

  @Test
  fun rti() {
    assertForAddressModes(
      RTI,
      initState = { with(s =  0x2D) },
      initStores = mapOf(0x12E to 0xCF) + addrToMem(0x012F, SCARY_ADDR),
      expectedState = { with(pc =   SCARY_ADDR, s =  0x30, n =  _1, v =  _1, d =  _1, i =  _1, z =  _1, c =  _1) }
    )
  }

  @Test
  fun bpl() {
    assertBranch(BPL) { with(n =  !it) }
  }

  @Test
  fun bmi() {
    assertBranch(BMI) { with(n =  it) }
  }

  @Test
  fun bvc() {
    assertBranch(BVC) { with(v =  !it) }
  }

  @Test
  fun bvs() {
    assertBranch(BVS) { with(v =  it) }
  }

  @Test
  fun bcc() {
    assertBranch(BCC) { with(c =  !it) }
  }

  @Test
  fun bcs() {
    assertBranch(BCS) { with(c =  it) }
  }

  @Test
  fun bne() {
    assertBranch(BNE) { with(z =  !it) }
  }

  @Test
  fun beq() {
    assertBranch(BEQ) { with(z =  it) }
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
      expectedState = { state(_1).with(pc =   BASE_USER + 2 + 0x30) }  // Offset from *next* instruction
    )
    assertForAddressModes(
      op,
      target = 0xD0,
      initState = { state(_1) },
      expectedState = { state(_1).with(pc =   BASE_USER + 2 - 0x30) }  // Offset from *next* instruction
    )
  }
}
