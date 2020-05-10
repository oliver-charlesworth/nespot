package choliver.nespot.sixfiveohtwo

import choliver.nespot.Address
import choliver.nespot.Data
import choliver.nespot.Memory
import choliver.nespot.sixfiveohtwo.InstructionDecoder.Decoded
import choliver.nespot.sixfiveohtwo.model.Instruction
import choliver.nespot.sixfiveohtwo.model.Opcode.*
import choliver.nespot.sixfiveohtwo.model.Operand.*
import choliver.nespot.sixfiveohtwo.model.Operand.IndexSource.X
import choliver.nespot.sixfiveohtwo.model.Operand.IndexSource.Y
import com.nhaarman.mockitokotlin2.doReturn
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.whenever
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

class InstructionDecoderTest {
  private val memory = mock<Memory>()
  private val decoder = InstructionDecoder(memory)

  @Nested
  inner class Relative {
    @Test
    fun positiveOffset() {
      assertEquals(0x1322,
        decode(Instruction(BEQ, Relative(0x30)), baseAddr = 0x12F0).addr
      )
    }

    @Test
    fun negativeOffset() {
      assertEquals(0x12C2,
        decode(Instruction(BEQ, Relative(0xD0)), baseAddr = 0x12F0).addr
      )
    }
  }

  @Test
  fun absolute() {
    assertEquals(
      0x1230,
      decode(Instruction(LDX, Absolute(0x1230))).addr
    )
  }

  @Test
  fun zeroPage() {
    assertEquals(
      0x0030,
      decode(Instruction(LDX, ZeroPage(0x30))).addr
    )
  }

  @Test
  fun indirect() {
    whenever(memory[0x40FF]) doReturn 0x30
    whenever(memory[0x4100]) doReturn 0x12

    assertEquals(
      0x1230,
      decode(Instruction(JMP, Indirect(0x40FF))).addr
    )
  }

  @Nested
  inner class AbsoluteIndexed {
    @Test
    fun basicX() {
      assertEquals(
        0x1230,
        decode(Instruction(LDA, AbsoluteIndexed(0x1220, X)), x = 0x10).addr
      )
    }

    @Test
    fun basicY() {
      assertEquals(
        0x1230,
        decode(Instruction(LDA, AbsoluteIndexed(0x1220, Y)), y = 0x10).addr
      )
    }
  }

  @Nested
  inner class ZeroPageIndexed {
    @Test
    fun basicX() {
      assertEquals(
        0x0030,
        decode(Instruction(LDY, ZeroPageIndexed(0x20, X)), x = 0x10).addr
      )
    }

    @Test
    fun basicY() {
      assertEquals(
        0x0030,
        decode(Instruction(LDX, ZeroPageIndexed(0x20, Y)), y = 0x10).addr
      )
    }

    @Test
    fun zeroPageWraparound() {
      assertEquals(
        0x0030,
        decode(Instruction(LDY, ZeroPageIndexed(0xF0, X)), x = 0x40).addr
      )
    }
  }

  @Nested
  inner class IndexedIndirect {
    @Test
    fun basic() {
      whenever(memory[0x0030]) doReturn 0x30
      whenever(memory[0x0031]) doReturn 0x12

      assertEquals(
        0x1230,
        decode(Instruction(LDA, IndexedIndirect(0x20)), x = 0x10).addr
      )
    }

    @Test
    fun zeroPageWraparoundOffset() {
      whenever(memory[0x0030]) doReturn 0x30
      whenever(memory[0x0031]) doReturn 0x12

      assertEquals(
        0x1230,
        decode(Instruction(LDA, IndexedIndirect(0xF0)), x = 0x40).addr
      )
    }

    @Test
    fun zeroPageWraparoundHighByte() {
      whenever(memory[0x00FF]) doReturn 0x30
      whenever(memory[0x0000]) doReturn 0x12

      assertEquals(
        0x1230,
        decode(Instruction(LDA, IndexedIndirect(0xFF)), x = 0x00).addr
      )
    }
  }

  @Nested
  inner class IndirectIndexed {
    @Test
    fun basic() {
      whenever(memory[0x0030]) doReturn 0x30
      whenever(memory[0x0031]) doReturn 0x12

      assertEquals(
        0x1240,
        decode(Instruction(LDA, IndirectIndexed(0x30)), y = 0x10).addr
      )
    }

    @Test
    fun zeroPageWraparoundHighByte() {
      whenever(memory[0x00FF]) doReturn 0x30
      whenever(memory[0x0000]) doReturn 0x12

      assertEquals(
        0x1240,
        decode(Instruction(LDA, IndirectIndexed(0xFF)), y = 0x10).addr
      )
    }
  }

  private fun decode(
    instruction: Instruction,
    x: Data = 0x00,
    y: Data = 0x00,
    baseAddr: Address = BASE_USER
  ): Decoded {
    val mmap = listOf(instruction).memoryMap(baseAddr)
    mmap.forEach { (addr, data) -> whenever(memory[addr]) doReturn data }
    return decoder.decode(baseAddr, x, y)
  }
}

