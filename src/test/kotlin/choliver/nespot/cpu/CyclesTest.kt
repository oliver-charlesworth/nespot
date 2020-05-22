package choliver.nespot.cpu

import choliver.nespot.cpu.model.Flags
import choliver.nespot.cpu.model.Instruction
import choliver.nespot.cpu.model.Opcode
import choliver.nespot.cpu.model.Opcode.*
import choliver.nespot.cpu.model.Operand.*
import choliver.nespot.cpu.model.Operand.IndexSource.X
import choliver.nespot.cpu.model.Operand.IndexSource.Y
import choliver.nespot.cpu.model.Regs
import choliver.nespot.cpu.utils._1
import org.junit.jupiter.api.Nested
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

  @Nested
  inner class OperandPageCrossing {
    private val standardOps = listOf(ADC, AND, CMP, EOR, LDA, ORA, SBC)

    @Test
    fun `absolute indexed x`() {
      (standardOps + LDY).forEach { op ->
        assertAbsoluteIndexed(op, X, 4, 5)
      }
    }

    @Test
    fun `absolute indexed y`() {
      (standardOps + LDX).forEach { op ->
        assertAbsoluteIndexed(op, Y, 4, 5)
      }
    }

    @Test
    fun `indirect indexed`() {
      standardOps.forEach { op ->
        assertIndirectIndexed(op, 5, 6)
      }
    }

    @Test
    fun `absolute indexed x - no variations for exceptions`() {
      listOf(INC, DEC, ASL, LSR, ROL, ROR).forEach { op ->
        assertAbsoluteIndexed(op, X, 7, 7)
      }
    }

    @Test
    fun `STA - no variations`() {
      assertAbsoluteIndexed(STA, X, 5, 5)
      assertAbsoluteIndexed(STA, Y, 5, 5)
      assertIndirectIndexed(STA, 6, 6)
    }

    private fun assertAbsoluteIndexed(op: Opcode, source: IndexSource, normalCycles: Int, pageCrossedCycles: Int) {
      val initRegs = when (source) {
        X -> Regs(x = 0x30)
        Y -> Regs(y = 0x30)
      }

      // Regular
      assertCpuEffects(
        instructions = listOf(
          Instruction(op, AbsoluteIndexed(0x12CF, source))
        ),
        initRegs = initRegs,
        expectedStores = null,
        expectedCycles = normalCycles,
        name = op.name
      )

      // Crosses page boundary
      assertCpuEffects(
        instructions = listOf(
          Instruction(op, AbsoluteIndexed(0x12D0, source))
        ),
        initRegs = initRegs,
        expectedStores = null,
        expectedCycles = pageCrossedCycles,
        name = op.name
      )
    }

    private fun assertIndirectIndexed(op: Opcode, normalCycles: Int, pageCrossedCycles: Int) {
      // Regular
      assertCpuEffects(
        instructions = listOf(
          Instruction(op, IndirectIndexed(0x12))
        ),
        initRegs = Regs(y = 0x30),
        initStores = listOf(0x12 to 0x12CF),
        expectedStores = null,
        expectedCycles = normalCycles,
        name = op.name
      )

      // Crosses page boundary
      assertCpuEffects(
        instructions = listOf(
          Instruction(op, IndirectIndexed(0x12))
        ),
        initRegs = Regs(y = 0x30),
        initStores = listOf(0x12 to 0x12D0),
        expectedStores = null,
        expectedCycles = pageCrossedCycles,
        name = op.name
      )
    }
  }
}
