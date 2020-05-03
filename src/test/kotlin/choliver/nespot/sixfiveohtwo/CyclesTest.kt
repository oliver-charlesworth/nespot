package choliver.nespot.sixfiveohtwo

import choliver.nespot.sixfiveohtwo.model.Flags
import choliver.nespot.sixfiveohtwo.model.Instruction
import choliver.nespot.sixfiveohtwo.model.Opcode.*
import choliver.nespot.sixfiveohtwo.model.Operand.IndexSource.X
import choliver.nespot.sixfiveohtwo.model.Operand.Relative
import choliver.nespot.sixfiveohtwo.model.Operand.ZeroPageIndexed
import choliver.nespot.sixfiveohtwo.model.State
import choliver.nespot.sixfiveohtwo.utils._1
import org.junit.jupiter.api.Test

class CyclesTest {
  @Test
  fun `calculates num cycles for executed instructions`() {
    assertCpuEffects(
      instructions = listOf(
        Instruction(NOP), // 2 cycles
        Instruction(ADC, ZeroPageIndexed(0x12, X)) // 4 cycles
      ),
      initState = State(),
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
      initState = State(),
      expectedCycles = 2
    )

    // Taken
    assertCpuEffects(
      instructions = listOf(
        Instruction(BEQ, Relative(0x01))
      ),
      initState = State(p = Flags(z = _1)),
      expectedCycles = 3
    )

    // Taken across page boundary
    assertCpuEffects(
      instructions = listOf(
        Instruction(BEQ, Relative(0x80))
      ),
      initState = State(p = Flags(z = _1)),
      expectedCycles = 4
    )
  }
}
