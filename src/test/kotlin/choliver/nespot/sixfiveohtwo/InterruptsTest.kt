package choliver.nespot.sixfiveohtwo

import choliver.nespot.hi
import choliver.nespot.lo
import choliver.nespot.sixfiveohtwo.Cpu.Companion.NUM_INTERRUPT_CYCLES
import choliver.nespot.sixfiveohtwo.Cpu.Companion.VECTOR_IRQ
import choliver.nespot.sixfiveohtwo.Cpu.Companion.VECTOR_NMI
import choliver.nespot.sixfiveohtwo.Cpu.Companion.VECTOR_RESET
import choliver.nespot.sixfiveohtwo.model.Flags
import choliver.nespot.sixfiveohtwo.model.Instruction
import choliver.nespot.sixfiveohtwo.model.Opcode.CLI
import choliver.nespot.sixfiveohtwo.model.Opcode.NOP
import choliver.nespot.sixfiveohtwo.model.State
import choliver.nespot.sixfiveohtwo.utils._0
import choliver.nespot.sixfiveohtwo.utils._1
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource


class InterruptsTest {
  private val memory =
    addrToMem(VECTOR_RESET, BASE_RESET) +
    addrToMem(VECTOR_NMI, BASE_NMI) +
    addrToMem(VECTOR_IRQ, BASE_IRQ)

  @ParameterizedTest(name = "I == {0}")
  @ValueSource(booleans = [_0, _1])
  fun `follows reset vector and sets I`(I: Boolean) {
    assertCpuEffects(
      instructions = listOf(Instruction(NOP)),
      initState = State(s = 0xFF, p = Flags(i = I)),
      initStores = memory,
      expectedState = State(s = 0xFF, p = Flags(i = _1), pc = BASE_RESET),
      expectedCycles = NUM_INTERRUPT_CYCLES,
      pollReset = { _1 }
    )
  }

  @ParameterizedTest(name = "I == {0}")
  @ValueSource(booleans = [_0, _1])
  fun `follows nmi vector and leaves I unmodified`(I: Boolean) {
    val flags = Flags(i = I)
    assertCpuEffects(
      instructions = listOf(Instruction(NOP)),
      initState = State(s = 0xFF, p = flags),
      initStores = memory,
      expectedState = State(s = 0xFC, p = flags, pc = BASE_NMI),
      expectedStores = listOf(
        0x1FF to BASE_USER.hi(),
        0x1FE to BASE_USER.lo(),
        0x1FD to flags.data()
      ),
      expectedCycles = NUM_INTERRUPT_CYCLES,
      pollNmi = { _1 }
    )
  }

  @Test
  fun `follows irq vector and sets I if I == _0`() {
    assertCpuEffects(
      instructions = listOf(Instruction(NOP)),
      initState = State(s = 0xFF, p = Flags(i = _0)),
      initStores = memory,
      expectedState = State(s = 0xFC, p = Flags(i = _1), pc = BASE_IRQ),
      expectedStores = listOf(
        0x1FF to BASE_USER.hi(),
        0x1FE to BASE_USER.lo(),
        0x1FD to Flags(i = _0).data()
      ),
      expectedCycles = NUM_INTERRUPT_CYCLES,
      pollIrq = { _1 }
    )
  }

  @Test
  fun `doesn't follow irq vector if I == _1`() {
    val flags = Flags(i = _1)
    assertCpuEffects(
      instructions = listOf(Instruction(NOP)),
      initState = State(s = 0xFF, p = flags),
      initStores = memory,
      expectedState = State(s = 0xFF, p = flags, pc = (BASE_USER + 1)),
      pollIrq = { _1 }
    )
  }

  /**
   * Proves that IRQ doesn't need to be de-asserted before another interrupt is triggered.
   *
   * We create an ISR that re-enables IRQ.  We detect that interrupt has fired twice by looking at how far the
   * stack has advanced.
   */
  @Test
  fun `irq is level-triggered`() {
    val isr = listOf(
      Instruction(CLI)
    ).memoryMap(BASE_IRQ)

    assertCpuEffects(
      instructions = listOf(Instruction(NOP)),
      numStepsToExecute = 3,  // Expect
      initState = State(s = 0xFF, p = Flags(i = _0)),
      initStores = memory + isr,
      expectedState = State(s = 0xF9, p = Flags(i = _1), pc = BASE_IRQ),  // Stack is twice as big!
      expectedStores = listOf(
        0x1FF to BASE_USER.hi(),
        0x1FE to BASE_USER.lo(),
        0x1FD to Flags(i = _0).data(),
        0x1FC to (BASE_IRQ + 1).hi(),   // We executed 1 instruction in the ISR
        0x1FB to (BASE_IRQ + 1).lo(),
        0x1FA to Flags(i = _0).data()
      ),
      pollIrq = { _1 }
    )
  }

  @Test
  fun `reset has highest priority`() {
    assertCpuEffects(
      instructions = listOf(Instruction(NOP)),
      initState = State(s = 0xFF, p = Flags(i = _0)),
      initStores = memory,
      expectedState = State(s = 0xFF, p = Flags(i = _1), pc = BASE_RESET),
      pollReset = { _1 },
      pollNmi = { _1 },
      pollIrq = { _1 }
    )
  }

  @Test
  fun `nmi has second highest priority`() {
    val flags = Flags(i = _0)
    assertCpuEffects(
      instructions = listOf(Instruction(NOP)),
      initState = State(s = 0xFF, p = flags),
      initStores = memory,
      expectedState = State(s = 0xFC, p = flags, pc = BASE_NMI),
      expectedStores = listOf(
        0x1FF to BASE_USER.hi(),
        0x1FE to BASE_USER.lo(),
        0x1FD to flags.data()
      ),
      pollNmi = { _1 },
      pollIrq = { _1 }
    )
  }

  companion object {
    private const val BASE_RESET = BASE_USER + 0x1230
    private const val BASE_NMI = BASE_USER + 0x2340
    private const val BASE_IRQ = BASE_USER + 0x3450
  }
}
