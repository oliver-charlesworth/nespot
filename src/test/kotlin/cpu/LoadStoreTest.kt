package cpu

import FakeMemory
import choliver.sixfiveohtwo.*
import choliver.sixfiveohtwo.AddressMode.*
import choliver.sixfiveohtwo.AddressMode.IndexSource.*
import choliver.sixfiveohtwo.Opcode.*
import forOpcode
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

class LoadStoreTest {
  private val memory = FakeMemory()
  private val cpu = Cpu(memory)

  @Nested
  inner class Lda {
    @Test
    fun immediateAndFlags() {
      forOpcode(LDA) {
        assertEquals(s.with(A = 0x69u, Z = _0, N = _0), s, Immediate(0x69u))
        assertEquals(s.with(A = 0x00u, Z = _1, N = _0), s, Immediate(0x00u))
        assertEquals(s.with(A = 0x96u, Z = _0, N = _1), s, Immediate(0x96u))
      }
    }

    @Test
    fun zeroPage() {
      memory.store(0x0030u, 0x69u)
      assertEquals(
        State(A = 0x69u),
        cpu.execute(Instruction(LDA, ZeroPage(0x30u)), State())
      )
    }

    @Test
    fun zeroPageX() {
      memory.store(0x0050u, 0x69u)
      assertEquals(
        State(A = 0x69u, X = 0x20u),
        cpu.execute(Instruction(LDA, ZeroPageIndexed(0x30u, X)), State(X = 0x20u))
      )
    }

    @Test
    fun absolute() {
      memory.store(0x1230u, 0x69u)
      assertEquals(
        State(A = 0x69u),
        cpu.execute(Instruction(LDA, Absolute(0x1230u)), State())
      )
    }

    @Test
    fun absoluteX() {
      memory.store(0x1250u, 0x69u)
      assertEquals(
        State(A = 0x69u, X = 0x20u),
        cpu.execute(Instruction(LDA, AbsoluteIndexed(0x1230u, X)), State(X = 0x20u))
      )
    }

    @Test
    fun absoluteY() {
      memory.store(0x1250u, 0x69u)
      assertEquals(
        State(A = 0x69u, Y = 0x20u),
        cpu.execute(Instruction(LDA, AbsoluteIndexed(0x1230u, Y)), State(Y = 0x20u))
      )
    }

//    @Test
//    fun indexedIndirect() {
//      memory.store(0x1250u, 0x69u)
//      assertEquals(
//        State(A = 0x69u, Y = 0x20u),
//        cpu.execute(Instruction(LDA, IndexedIndirect(0x1230u)), State(Y = 0x20u))
//      )
//    }
  }

  @Nested
  inner class Ldx {
    @Test
    fun immediateAndFlags() {
      forOpcode(LDX) {
        assertEquals(s.with(X = 0x69u, Z = _0, N = _0), s, Immediate(0x69u))
        assertEquals(s.with(X = 0x00u, Z = _1, N = _0), s, Immediate(0x00u))
        assertEquals(s.with(X = 0x96u, Z = _0, N = _1), s, Immediate(0x96u))
      }
    }

    @Test
    fun zeroPage() {
      memory.store(0x0030u, 0x69u)
      assertEquals(
        State(X = 0x69u),
        cpu.execute(Instruction(LDX, ZeroPage(0x30u)), State())
      )
    }

    @Test
    fun zeroPageY() {
      memory.store(0x0050u, 0x69u)
      assertEquals(
        State(X = 0x69u, Y = 0x20u),
        cpu.execute(Instruction(LDX, ZeroPageIndexed(0x30u, Y)), State(Y = 0x20u))
      )
    }

    @Test
    fun absolute() {
      memory.store(0x1230u, 0x69u)
      assertEquals(
        State(X = 0x69u),
        cpu.execute(Instruction(LDX, Absolute(0x1230u)), State())
      )
    }

    @Test
    fun absoluteY() {
      memory.store(0x1250u, 0x69u)
      assertEquals(
        State(X = 0x69u, Y = 0x20u),
        cpu.execute(Instruction(LDX, AbsoluteIndexed(0x1230u, Y)), State(Y = 0x20u))
      )
    }
  }

  @Nested
  inner class Ldy {
    @Test
    fun immediateAndFlags() {
      forOpcode(LDY) {
        assertEquals(s.with(Y = 0x69u, Z = _0, N = _0), s, Immediate(0x69u))
        assertEquals(s.with(Y = 0x00u, Z = _1, N = _0), s, Immediate(0x00u))
        assertEquals(s.with(Y = 0x96u, Z = _0, N = _1), s, Immediate(0x96u))
      }
    }

    @Test
    fun zeroPage() {
      memory.store(0x0030u, 0x69u)
      assertEquals(
        State(Y = 0x69u),
        cpu.execute(Instruction(LDY, ZeroPage(0x30u)), State())
      )
    }

    @Test
    fun zeroPageX() {
      memory.store(0x0050u, 0x69u)
      assertEquals(
        State(Y = 0x69u, X = 0x20u),
        cpu.execute(Instruction(LDY, ZeroPageIndexed(0x30u, X)), State(X = 0x20u))
      )
    }

    @Test
    fun absolute() {
      memory.store(0x1230u, 0x69u)
      assertEquals(
        State(Y = 0x69u),
        cpu.execute(Instruction(LDY, Absolute(0x1230u)), State())
      )
    }

    @Test
    fun absoluteX() {
      memory.store(0x1250u, 0x69u)
      assertEquals(
        State(Y = 0x69u, X = 0x20u),
        cpu.execute(Instruction(LDY, AbsoluteIndexed(0x1230u, X)), State(X = 0x20u))
      )
    }
  }

  @Nested
  inner class Sta {
    // TODO - flag sweep

    @Test
    fun zeroPage() {
      assertStores(target = 0x0030u, addrMode = ZeroPage(0x30u))
    }

    @Test
    fun zeroPageX() {
      assertStores(target = 0x0030u, addrMode = ZeroPageIndexed(0x10u, X), state = State(X = 0x20u))
    }

    @Test
    fun absolute() {
      assertStores(target = 0x1230u, addrMode = Absolute(0x1230u))
    }

    @Test
    fun absoluteX() {
      assertStores(target = 0x1230u, addrMode = AbsoluteIndexed(0x1210u, X), state = State(X = 0x20u))
    }

    @Test
    fun absoluteY() {
      assertStores(target = 0x1230u, addrMode = AbsoluteIndexed(0x1210u, Y), state = State(Y = 0x20u))
    }

    @Test
    fun indexedIndirect() {
      memory.store(0x40u, 0x30u)
      memory.store(0x41u, 0x12u)

      assertStores(target = 0x1230u, addrMode = IndexedIndirect(0x30u), state = State(X = 0x10u))
    }

    @Test
    fun indirectIndexed() {
      memory.store(0x30u, 0x20u)
      memory.store(0x31u, 0x12u)

      assertStores(target = 0x1230u, addrMode = IndirectIndexed(0x30u), state = State(Y = 0x10u))
    }

    private fun assertStores(target: UInt16, addrMode: AddressMode, state: State = State()) {
      cpu.execute(Instruction(STA, addrMode), state.with(A = 0x69u))

      assertEquals(0x69u.toUInt8(), memory.load(target))
    }
  }

  @Nested
  inner class Stx {
    // TODO - flag sweep

    @Test
    fun zeroPage() {
      assertStores(target = 0x0030u, addrMode = ZeroPage(0x30u))
    }

    @Test
    fun zeroPageY() {
      assertStores(target = 0x0030u, addrMode = ZeroPageIndexed(0x10u, Y), state = State(Y = 0x20u))
    }

    @Test
    fun absolute() {
      assertStores(target = 0x1230u, addrMode = Absolute(0x1230u))
    }

    private fun assertStores(target: UInt16, addrMode: AddressMode, state: State = State()) {
      cpu.execute(Instruction(STX, addrMode), state.with(X = 0x69u))

      assertEquals(0x69u.toUInt8(), memory.load(target))
    }
  }

  @Nested
  inner class Sty {
    // TODO - flag sweep

    @Test
    fun zeroPage() {
      assertStores(target = 0x0030u, addrMode = ZeroPage(0x30u))
    }

    @Test
    fun zeroPageX() {
      assertStores(target = 0x0030u, addrMode = ZeroPageIndexed(0x10u, X), state = State(X = 0x20u))
    }

    @Test
    fun absolute() {
      assertStores(target = 0x1230u, addrMode = Absolute(0x1230u))
    }

    private fun assertStores(target: UInt16, addrMode: AddressMode, state: State = State()) {
      cpu.execute(Instruction(STY, addrMode), state.with(Y = 0x69u))

      assertEquals(0x69u.toUInt8(), memory.load(target))
    }
  }
}
