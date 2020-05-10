package choliver.nespot.cpu

import choliver.nespot.cpu.model.Flags
import choliver.nespot.cpu.model.Instruction
import choliver.nespot.cpu.model.Opcode.*
import choliver.nespot.cpu.model.Operand.IndexSource.X
import choliver.nespot.cpu.model.Operand.Relative
import choliver.nespot.cpu.model.Operand.ZeroPageIndexed
import choliver.nespot.cpu.model.Regs
import choliver.nespot.cpu.utils._1
import org.junit.jupiter.api.Test

class CyclesTest {
  @Test
  fun `calculates num cycles for executed instructions`() {
    assertCpuEffects(
      instructions = listOf(
        Instruction(NOP), // 2 cycles
        Instruction(ADC, ZeroPageIndexed(0x12, X)) // 4 cycles
      ),
      initRegs = Regs(),
      expectedCycles = 6
    )
  }
  @Test
  fun `models variable number of branch instructions`() {
    // Untaken
    assertCpuEffects(
      instructions = listOf(
        Instruction(BEQ, Relative(0x01)) // Normally 2 cycles
      ),
      initRegs = Regs(),
      expectedCycles = 2
    )

    // Taken
    assertCpuEffects(
      instructions = listOf(
        Instruction(BEQ, Relative(0x01))
      ),
      initRegs = Regs(p = Flags(z = _1)),
      expectedCycles = 3
    )

    // Taken across page boundary
    assertCpuEffects(
      instructions = listOf(
        Instruction(BEQ, Relative(0x80))
      ),
      initRegs = Regs(p = Flags(z = _1)),
      expectedCycles = 4
    )
  }
}