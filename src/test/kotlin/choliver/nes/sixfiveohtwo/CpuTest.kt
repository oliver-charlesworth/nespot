package choliver.nes.sixfiveohtwo

import choliver.nes.hi
import choliver.nes.lo
import choliver.nes.sixfiveohtwo.Cpu.Companion.NUM_INTERRUPT_CYCLES
import choliver.nes.sixfiveohtwo.Cpu.Companion.VECTOR_IRQ
import choliver.nes.sixfiveohtwo.Cpu.Companion.VECTOR_NMI
import choliver.nes.sixfiveohtwo.Cpu.Companion.VECTOR_RESET
import choliver.nes.sixfiveohtwo.model.Flags
import choliver.nes.sixfiveohtwo.model.Instruction
import choliver.nes.sixfiveohtwo.model.Opcode.NOP
import choliver.nes.sixfiveohtwo.model.State
import choliver.nes.sixfiveohtwo.model.toPC
import choliver.nes.sixfiveohtwo.utils._0
import choliver.nes.sixfiveohtwo.utils._1
import com.nhaarman.mockitokotlin2.verify
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource

class CpuTest {
  // TODO - runSteps return number of cycles

  @Nested
  inner class Interrupts {
    private val currentFrame = 0x4560
    private val resetHandler = 0x1230
    private val nmiHandler = 0x2340
    private val irqHandler = 0x3450

    private val memory = mockMemory(listOf(Instruction(NOP)).memoryMap(currentFrame) +
      addrToMem(VECTOR_RESET, resetHandler) +
      addrToMem(VECTOR_NMI, nmiHandler) +
      addrToMem(VECTOR_IRQ, irqHandler)
    )

    @ParameterizedTest(name = "I == {0}")
    @ValueSource(booleans = [_0, _1])
    fun `follows reset vector and sets I`(I: Boolean) {
      val cpu = Cpu(
        memory = memory,
        pollReset = { _1 },
        initialState = State(PC = currentFrame.toPC(), P = Flags(I = I))
      )

      val n = cpu.runSteps(1)

      assertEquals(NUM_INTERRUPT_CYCLES, n)
      assertEquals(resetHandler, cpu.state.PC.addr())
      assertEquals(_1, cpu.state.P.I)
      // Don't care about stack manipulation
    }

    @ParameterizedTest(name = "I == {0}")
    @ValueSource(booleans = [_0, _1])
    fun `follows nmi vector and leaves I unmodified`(I: Boolean) {
      val cpu = Cpu(
        memory = memory,
        pollNmi = { _1 },
        initialState = State(S = 0xFF, PC = currentFrame.toPC(), P = Flags(I = I))
      )

      val n = cpu.runSteps(1)

      assertEquals(NUM_INTERRUPT_CYCLES, n)
      assertEquals(nmiHandler, cpu.state.PC.addr())
      assertEquals(I, cpu.state.P.I)
      verify(memory).store(0x01FF, currentFrame.hi())
      verify(memory).store(0x01FE, currentFrame.lo())
      verify(memory).store(0x01FD, Flags(I = I).data())
    }

    @Test
    fun `follows irq vector and leaves I unmodified if I == _0`() {
      val cpu = Cpu(
        memory = memory,
        pollIrq = { _1 },
        initialState = State(PC = currentFrame.toPC(), P = Flags(I = _0))
      )

      val n = cpu.runSteps(1)

      assertEquals(NUM_INTERRUPT_CYCLES, n)
      assertEquals(irqHandler, cpu.state.PC.addr())
      assertEquals(_0, cpu.state.P.I)
      // TODO - assert stack
    }

    @Test
    fun `doesn't follow irq vector if I == _1`() {
      val cpu = Cpu(
        memory = memory,
        pollIrq = { _1 },
        initialState = State(PC = currentFrame.toPC(), P = Flags(I = _1))
      )

      cpu.runSteps(1)

      assertEquals(currentFrame + 1, cpu.state.PC.addr()) // Expect to have run the NOP instead
    }

    @Test
    fun `reset has highest priority`() {
      val cpu = Cpu(
        memory = memory,
        pollReset = { _1 },
        pollNmi = { _1 },
        pollIrq = { _1 },
        initialState = State(PC = currentFrame.toPC(), P = Flags(I = _0))
      )

      cpu.runSteps(1)

      assertEquals(resetHandler, cpu.state.PC.addr())
    }

    @Test
    fun `nmi has second highest priority`() {
      val cpu = Cpu(
        memory = memory,
        pollNmi = { _1 },
        pollIrq = { _1 },
        initialState = State(PC = currentFrame.toPC(), P = Flags(I = _0))
      )

      cpu.runSteps(1)

      assertEquals(nmiHandler, cpu.state.PC.addr())
    }
  }
}
