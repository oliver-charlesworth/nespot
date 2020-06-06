package choliver.nespot.cpu

import choliver.nespot.cpu.Cpu.Companion.NUM_INTERRUPT_CYCLES
import choliver.nespot.cpu.Cpu.Companion.VECTOR_IRQ
import choliver.nespot.cpu.Cpu.Companion.VECTOR_NMI
import choliver.nespot.cpu.Cpu.Companion.VECTOR_RESET
import choliver.nespot.cpu.Opcode.CLI
import choliver.nespot.cpu.Opcode.NOP
import choliver.nespot.hi
import choliver.nespot.lo
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource

class InterruptsTest {
  @Nested
  inner class Reset {
    @ParameterizedTest(name = "I == {0}")
    @ValueSource(booleans = [_0, _1])
    fun `follows vector and sets I`(I: Boolean) {
      assertCpuEffects(
        instructions = INSTRUCTIONS,
        numStepsToExecute = 2,   // Enough for [delay, vector]
        initRegs = Regs(s = 0xFF, p = Flags(i = I)),
        initStores = MEMORY,
        expectedRegs = Regs(s = 0xFF, p = Flags(i = _1), pc = BASE_RESET),
        expectedCycles = NUM_DELAY_CYCLES + NUM_INTERRUPT_CYCLES,
        pollReset = { _1 }
      )
    }

    /**
     * Proves that leaving RESET asserted triggers a subsequent interrupt.
     *
     * We detect that interrupt has fired twice by asserting that the PC is still at the base of the ISR, even though
     * we've run multiple steps.
     */
    @Test
    fun `level-triggered`() {
      val isr = listOf(
        Instruction(NOP),
        Instruction(NOP)
      ).memoryMap(BASE_RESET)

      assertCpuEffects(
        instructions = INSTRUCTIONS,
        numStepsToExecute = 3,    // Enough for [delay, vector, vector]
        initRegs = Regs(s = 0xFF, p = Flags(i = _0)),
        initStores = MEMORY + isr,
        expectedRegs = Regs(s = 0xFF, p = Flags(i = _1), pc = BASE_RESET),  // Back at the ISR base
        pollReset = { _1 }
      )
    }
  }

  @Nested
  inner class Nmi {
    @ParameterizedTest(name = "I == {0}")
    @ValueSource(booleans = [_0, _1])
    fun `follows vector and leaves I unmodified`(I: Boolean) {
      val flags = Flags(i = I)
      assertCpuEffects(
        instructions = INSTRUCTIONS,
        numStepsToExecute = 2,   // Enough for [delay, vector]
        initRegs = Regs(s = 0xFF, p = flags),
        initStores = MEMORY,
        expectedRegs = Regs(s = 0xFC, p = flags, pc = BASE_NMI),
        expectedStores = listOf(
          0x1FF to PREV_FRAME.hi(),
          0x1FE to PREV_FRAME.lo(),
          0x1FD to flags.data()
        ),
        expectedCycles = NUM_DELAY_CYCLES + NUM_INTERRUPT_CYCLES,
        pollNmi = { _1 }
      )
    }

    /**
     * Proves that leaving NMI asserted doesn't trigger a subsequent interrupt.
     *
     * We detect that interrupt has fired only once by asserting that the PC has advanced into the ISR.
     */
    @Test
    fun `edge-triggered`() {
      val isr = listOf(
        Instruction(NOP)
      ).memoryMap(BASE_NMI)

      val flags = Flags(i = _0)
      assertCpuEffects(
        instructions = INSTRUCTIONS,
        numStepsToExecute = 3,    // Enough for [delay, vector, regular instruction]
        initRegs = Regs(s = 0xFF, p = flags),
        initStores = MEMORY + isr,
        expectedRegs = Regs(s = 0xFC, p = flags, pc = BASE_NMI + 1),      // PC has advanced
        expectedStores = listOf(
          0x1FF to PREV_FRAME.hi(),
          0x1FE to PREV_FRAME.lo(),
          0x1FD to flags.data()
        ),
        pollNmi = { _1 }    // Continually asserted
      )
    }

    /**
     * Proves that re-asserting NMI does trigger a subsequent interrupt.
     */
    @Test
    fun `second edge triggers another interrupt`() {
      val isr = listOf(
        Instruction(NOP)
      ).memoryMap(BASE_NMI)

      val flags = Flags(i = _0)
      assertCpuEffects(
        instructions = INSTRUCTIONS,
        numStepsToExecute = 4,    // Enough for [delay, vector, delay, vector]
        initRegs = Regs(s = 0xFF, p = flags),
        initStores = MEMORY + isr,
        expectedRegs = Regs(s = 0xF9, p = flags, pc = BASE_NMI),          // Stack is twice as big!
        expectedStores = listOf(
          0x1FF to PREV_FRAME.hi(),
          0x1FE to PREV_FRAME.lo(),
          0x1FD to flags.data(),
          0x1FC to (BASE_NMI + 1).hi(),   // We executed 1 instruction in the first ISR
          0x1FB to (BASE_NMI + 1).lo(),
          0x1FA to flags.data()
        ),
        pollNmi = { listOf(_1, _0, _1, _0)[it] }   // Two rising edges
      )
    }
  }

  @Nested
  inner class Irq {
    @Test
    fun `follows vector and sets I if I == _0`() {
      assertCpuEffects(
        instructions = INSTRUCTIONS,
        numStepsToExecute = 2,   // Enough for [delay, vector]
        initRegs = Regs(s = 0xFF, p = Flags(i = _0)),
        initStores = MEMORY,
        expectedRegs = Regs(s = 0xFC, p = Flags(i = _1), pc = BASE_IRQ),
        expectedStores = listOf(
          0x1FF to PREV_FRAME.hi(),
          0x1FE to PREV_FRAME.lo(),
          0x1FD to Flags(i = _0).data()
        ),
        expectedCycles = NUM_DELAY_CYCLES + NUM_INTERRUPT_CYCLES,
        pollIrq = { _1 }
      )
    }

    @Test
    fun `doesn't follow vector if I == _1`() {
      val flags = Flags(i = _1)
      assertCpuEffects(
        instructions = INSTRUCTIONS,
        numStepsToExecute = 2,   // Enough for [delay, regular instruction]
        initRegs = Regs(s = 0xFF, p = flags),
        initStores = MEMORY,
        expectedRegs = Regs(s = 0xFF, p = flags, pc = (BASE_USER + 2)),
        pollIrq = { _1 }
      )
    }

    /**
     * Proves that leaving IRQ asserted triggers a subsequent interrupt.
     *
     * We detect that interrupt has fired twice by:
     *   - Asserting that PC is still at the base of the ISR, even though we've run multiple steps.
     *   - Asserting that the stack is twice as big.
     *
     * Note that unlike for RESET, we need to re-enable IRQs inside the ISR, via CLI instruction.
     */
    @Test
    fun `level-triggered`() {
      val isr = listOf(
        Instruction(CLI)
      ).memoryMap(BASE_IRQ)

      assertCpuEffects(
        instructions = INSTRUCTIONS,
        numStepsToExecute = 4,    // Enough for [delay, vector, CLI, vector]
        initRegs = Regs(s = 0xFF, p = Flags(i = _0)),
        initStores = MEMORY + isr,
        expectedRegs = Regs(s = 0xF9, p = Flags(i = _1), pc = BASE_IRQ),  // Stack is twice as big!
        expectedStores = listOf(
          0x1FF to PREV_FRAME.hi(),
          0x1FE to PREV_FRAME.lo(),
          0x1FD to Flags(i = _0).data(),
          0x1FC to (BASE_IRQ + 1).hi(),   // We executed 1 instruction in the first ISR
          0x1FB to (BASE_IRQ + 1).lo(),
          0x1FA to Flags(i = _0).data()
        ),
        pollIrq = { _1 }  // Continuously asserted
      )
    }
  }

  @Nested
  inner class Priority {
    @Test
    fun `reset has highest priority`() {
      assertCpuEffects(
        instructions = INSTRUCTIONS,
        numStepsToExecute = 2,
        initRegs = Regs(s = 0xFF, p = Flags(i = _0)),
        initStores = MEMORY,
        expectedRegs = Regs(s = 0xFF, p = Flags(i = _1), pc = BASE_RESET),
        pollReset = { _1 },
        pollNmi = { _1 },
        pollIrq = { _1 }
      )
    }

    @Test
    fun `nmi has second highest priority`() {
      val flags = Flags(i = _0)
      assertCpuEffects(
        instructions = INSTRUCTIONS,
        numStepsToExecute = 2,
        initRegs = Regs(s = 0xFF, p = flags),
        initStores = MEMORY,
        expectedRegs = Regs(s = 0xFC, p = flags, pc = BASE_NMI),
        expectedStores = listOf(
          0x1FF to PREV_FRAME.hi(),
          0x1FE to PREV_FRAME.lo(),
          0x1FD to flags.data()
        ),
        pollNmi = { _1 },
        pollIrq = { _1 }
      )
    }
  }

  companion object {
    private const val BASE_RESET = BASE_USER + 0x1230
    private const val BASE_NMI = BASE_USER + 0x2340
    private const val BASE_IRQ = BASE_USER + 0x3450

    private val MEMORY =
      addrToMem(VECTOR_RESET, BASE_RESET) +
      addrToMem(VECTOR_NMI, BASE_NMI) +
      addrToMem(VECTOR_IRQ, BASE_IRQ)

    private val INSTRUCTIONS = listOf(Instruction(NOP), Instruction(NOP))

    private const val PREV_FRAME = BASE_USER + 1  // Advanced bt 1 due to delayed interrupt polling

    private const val NUM_DELAY_CYCLES = 2  // Delay corresponds to first instruction (i.e. NOP)
  }
}
