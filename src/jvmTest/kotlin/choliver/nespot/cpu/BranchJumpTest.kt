package choliver.nespot.cpu

import choliver.nespot.Address
import choliver.nespot.cpu.Cpu.Companion.VECTOR_IRQ
import choliver.nespot.cpu.Opcode.*
import choliver.nespot.hi
import choliver.nespot.lo
import org.junit.jupiter.api.Test

class BranchJumpTest {

  @Test
  fun jmp() {
    assertForAddressModes(
      JMP,
      expectedRegs = { with(pc = SCARY_ADDR) }
    )
  }

  @Test
  fun jsr() {
    assertForAddressModes(
      JSR,
      initRegs = { with(s = 0x30) },
      expectedRegs = { with(pc = SCARY_ADDR, s = 0x2E) },
      expectedStores = { addrToMem(0x012F, BASE_USER + 2) } // JSR stores *last* byte of instruction
    )
  }

  @Test
  fun brk() {
    assertForAddressModes(
      BRK,
      initRegs = { with(s = 0x30, n = _1, v = _1, d = _1, i = _1, z = _1, c = _1) },
      initStores = addrToMem(VECTOR_IRQ, SCARY_ADDR),
      expectedRegs = { with(pc = SCARY_ADDR, s = 0x2D, n = _1, v = _1, d = _1, i = _1, z = _1, c = _1) },
      expectedStores = {
        addrToMem(0x012F, BASE_USER + 2) + // BRK stores PC+2
        listOf(0x12E to 0xDF)  // Note B is also set on stack
      }
    )
  }

  @Test
  fun rts() {
    assertForAddressModes(
      RTS,
      initRegs = { with(s = 0x2E) },
      initStores = addrToMem(0x012F, SCARY_ADDR - 1), // JSR stores *last* byte of instruction
      expectedRegs = { with(pc = SCARY_ADDR, s = 0x30) }
    )
  }

  @Test
  fun rti() {
    assertForAddressModes(
      RTI,
      initRegs = { with(s = 0x2D) },
      initStores = listOf(0x12E to 0xCF) + addrToMem(0x012F, SCARY_ADDR),
      expectedRegs = { with(pc = SCARY_ADDR, s = 0x30, n = _1, v = _1, d = _1, i = _1, z = _1, c = _1) }
    )
  }

  @Test
  fun bpl() {
    assertBranch(BPL) { with(n = !it) }
  }

  @Test
  fun bmi() {
    assertBranch(BMI) { with(n = it) }
  }

  @Test
  fun bvc() {
    assertBranch(BVC) { with(v = !it) }
  }

  @Test
  fun bvs() {
    assertBranch(BVS) { with(v = it) }
  }

  @Test
  fun bcc() {
    assertBranch(BCC) { with(c = !it) }
  }

  @Test
  fun bcs() {
    assertBranch(BCS) { with(c = it) }
  }

  @Test
  fun bne() {
    assertBranch(BNE) { with(z = !it) }
  }

  @Test
  fun beq() {
    assertBranch(BEQ) { with(z = it) }
  }

  private fun assertBranch(op: Opcode, regs: Regs.(b: Boolean) -> Regs) {
    assertForAddressModes(
      op,
      target = 0x30,
      initRegs = { regs(_0) },
      expectedRegs = { regs(_0) }
    )
    assertForAddressModes(
      op,
      target = 0x30,
      initRegs = { regs(_1) },
      expectedRegs = { regs(_1).with(pc = BASE_USER + 2 + 0x30) }  // Offset from *next* instruction
    )
    assertForAddressModes(
      op,
      target = 0xD0,
      initRegs = { regs(_1) },
      expectedRegs = { regs(_1).with(pc = BASE_USER + 2 - 0x30) }  // Offset from *next* instruction
    )
  }

  private fun addrToMem(addr: Address, data16: Int) = listOf((addr + 1) to data16.hi(), addr to data16.lo())
}
