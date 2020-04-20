package choliver.nes.sixfiveohtwo

import choliver.nes.hi
import choliver.nes.lo
import choliver.nes.sixfiveohtwo.Cpu.Companion.NUM_INTERRUPT_CYCLES
import choliver.nes.sixfiveohtwo.Cpu.Companion.VECTOR_IRQ
import choliver.nes.sixfiveohtwo.Cpu.Companion.VECTOR_NMI
import choliver.nes.sixfiveohtwo.Cpu.Companion.VECTOR_RESET
import choliver.nes.sixfiveohtwo.model.Flags
import choliver.nes.sixfiveohtwo.model.Instruction
import choliver.nes.sixfiveohtwo.model.Opcode.ADC
import choliver.nes.sixfiveohtwo.model.Opcode.NOP
import choliver.nes.sixfiveohtwo.model.Operand.IndexSource.X
import choliver.nes.sixfiveohtwo.model.Operand.ZeroPageIndexed
import choliver.nes.sixfiveohtwo.model.State
import choliver.nes.sixfiveohtwo.utils._0
import choliver.nes.sixfiveohtwo.utils._1
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource

class CpuTest {
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

  @Nested
  inner class Interrupts {
    private val resetHandler = BASE_USER + 0x1230
    private val nmiHandler = BASE_USER + 0x2340
    private val irqHandler = BASE_USER + 0x3450

    private val memory =
      addrToMem(VECTOR_RESET, resetHandler) +
      addrToMem(VECTOR_NMI, nmiHandler) +
      addrToMem(VECTOR_IRQ, irqHandler)

    @ParameterizedTest(name = "I == {0}")
    @ValueSource(booleans = [_0, _1])
    fun `follows reset vector and sets I`(I: Boolean) {
      assertCpuEffects(
        instructions = listOf(Instruction(NOP)),
        initState = State(S = 0xFF, P = Flags(I = I)),
        initStores = memory,
        expectedState = State(S = 0xFF, P = Flags(I = _1), PC = resetHandler),
        expectedCycles = NUM_INTERRUPT_CYCLES,
        pollReset = { _1 }
      )
    }

    @ParameterizedTest(name = "I == {0}")
    @ValueSource(booleans = [_0, _1])
    fun `follows nmi vector and leaves I unmodified`(I: Boolean) {
      val flags = Flags(I = I)
      assertCpuEffects(
        instructions = listOf(Instruction(NOP)),
        initState = State(S = 0xFF, P = flags),
        initStores = memory,
        expectedState = State(S = 0xFC, P = flags, PC = nmiHandler),
        expectedStores = expectedStores(flags),
        expectedCycles = NUM_INTERRUPT_CYCLES,
        pollNmi = { _1 }
      )
    }

    @Test
    fun `follows irq vector and leaves I unmodified if I == _0`() {
      val flags = Flags(I = _0)
      assertCpuEffects(
        instructions = listOf(Instruction(NOP)),
        initState = State(S = 0xFF, P = flags),
        initStores = memory,
        expectedState = State(S = 0xFC, P = flags, PC = irqHandler),
        expectedStores = expectedStores(flags),
        expectedCycles = NUM_INTERRUPT_CYCLES,
        pollIrq = { _1 }
      )
    }

    @Test
    fun `doesn't follow irq vector if I == _1`() {
      val flags = Flags(I = _1)
      assertCpuEffects(
        instructions = listOf(Instruction(NOP)),
        initState = State(S = 0xFF, P = flags),
        initStores = memory,
        expectedState = State(S = 0xFF, P = flags, PC = (BASE_USER + 1)),
        pollIrq = { _1 }
      )
    }

    @Test
    fun `reset has highest priority`() {
      assertCpuEffects(
        instructions = listOf(Instruction(NOP)),
        initState = State(S = 0xFF, P = Flags(I = _0)),
        initStores = memory,
        expectedState = State(S = 0xFF, P = Flags(I = _1), PC = resetHandler),
        pollReset = { _1 },
        pollNmi = { _1 },
        pollIrq = { _1 }
      )
    }

    @Test
    fun `nmi has second highest priority`() {
      val flags = Flags(I = _0)
      assertCpuEffects(
        instructions = listOf(Instruction(NOP)),
        initState = State(S = 0xFF, P = flags),
        initStores = memory,
        expectedState = State(S = 0xFC, P = flags, PC = nmiHandler),
        expectedStores = expectedStores(flags),
        pollNmi = { _1 },
        pollIrq = { _1 }
      )
    }

    private fun expectedStores(P: Flags) = mapOf(
      0x01FF to BASE_USER.hi(),
      0x01FE to BASE_USER.lo(),
      0x01FD to P.data()
    )
  }
}
