package cpu

import FakeMemory
import choliver.sixfiveohtwo.*
import choliver.sixfiveohtwo.AddressMode.*
import choliver.sixfiveohtwo.AddressMode.IndexSource.*
import choliver.sixfiveohtwo.Opcode.*
import enc
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
        cpu.execute(enc(0xA5, 0x30), State())
      )
    }

    @Test
    fun zeroPageX() {
      memory.store(0x0050u, 0x69u)
      assertEquals(
        State(A = 0x69u, X = 0x20u),
        cpu.execute(enc(0xB5, 0x30), State(X = 0x20u))
      )
    }

    @Test
    fun absolute() {
      memory.store(0x1230u, 0x69u)
      assertEquals(
        State(A = 0x69u),
        cpu.execute(enc(0xAD, 0x30, 0x12), State())
      )
    }

    @Test
    fun absoluteX() {
      memory.store(0x1250u, 0x69u)
      assertEquals(
        State(A = 0x69u, X = 0x20u),
        cpu.execute(enc(0xBD, 0x30, 0x12), State(X = 0x20u))
      )
    }

    @Test
    fun absoluteY() {
      memory.store(0x1250u, 0x69u)
      assertEquals(
        State(A = 0x69u, Y = 0x20u),
        cpu.execute(enc(0xB9, 0x30, 0x12), State(Y = 0x20u))
      )
    }

    @Test
    fun indexedIndirect() {
      memory.store(0x1230u, 0x69u)
      memory.store(0x0040u, 0x30u)
      memory.store(0x0041u, 0x12u)

      assertEquals(
        State(A = 0x69u, X = 0x10u),
        cpu.execute(enc(0xA1, 0x30), State(X = 0x10u))
      )
    }

    @Test
    fun indirectIndexed() {
      memory.store(0x1230u, 0x69u)
      memory.store(0x0030u, 0x20u)
      memory.store(0x0031u, 0x12u)

      assertEquals(
        State(A = 0x69u, Y = 0x10u),
        cpu.execute(enc(0xB1, 0x30), State(Y = 0x10u))
      )
    }
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
        cpu.execute(enc(0xA6, 0x30), State())
      )
    }

    @Test
    fun zeroPageY() {
      memory.store(0x0050u, 0x69u)
      assertEquals(
        State(X = 0x69u, Y = 0x20u),
        cpu.execute(enc(0xB6, 0x30), State(Y = 0x20u))
      )
    }

    @Test
    fun absolute() {
      memory.store(0x1230u, 0x69u)
      assertEquals(
        State(X = 0x69u),
        cpu.execute(enc(0xAE, 0x30, 0x12), State())
      )
    }

    @Test
    fun absoluteY() {
      memory.store(0x1250u, 0x69u)
      assertEquals(
        State(X = 0x69u, Y = 0x20u),
        cpu.execute(enc(0xBE, 0x30, 0x12), State(Y = 0x20u))
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
        cpu.execute(enc(0xA4, 0x30), State())
      )
    }

    @Test
    fun zeroPageX() {
      memory.store(0x0050u, 0x69u)
      assertEquals(
        State(Y = 0x69u, X = 0x20u),
        cpu.execute(enc(0xB4, 0x30), State(X = 0x20u))
      )
    }

    @Test
    fun absolute() {
      memory.store(0x1230u, 0x69u)
      assertEquals(
        State(Y = 0x69u),
        cpu.execute(enc(0xAC, 0x30, 0x12), State())
      )
    }

    @Test
    fun absoluteX() {
      memory.store(0x1250u, 0x69u)
      assertEquals(
        State(Y = 0x69u, X = 0x20u),
        cpu.execute(enc(0xBC, 0x30, 0x12), State(X = 0x20u))
      )
    }
  }

  @Nested
  inner class Sta {
    // TODO - flag sweep

    @Test
    fun zeroPage() {
      assertStores(target = 0x0030u, encoding = enc(0x85, 0x30))
    }

    @Test
    fun zeroPageX() {
      assertStores(target = 0x0030u, encoding = enc(0x95, 0x10), state = State(X = 0x20u))
    }

    @Test
    fun absolute() {
      assertStores(target = 0x1230u, encoding = enc(0x8D, 0x30, 0x12))
    }

    @Test
    fun absoluteX() {
      assertStores(target = 0x1230u, encoding = enc(0x9D, 0x10, 0x12), state = State(X = 0x20u))
    }

    @Test
    fun absoluteY() {
      assertStores(target = 0x1230u, encoding = enc(0x99, 0x10, 0x12), state = State(Y = 0x20u))
    }

    @Test
    fun indexedIndirect() {
      memory.store(0x0040u, 0x30u)
      memory.store(0x0041u, 0x12u)

      assertStores(target = 0x1230u, encoding = enc(0x81, 0x30), state = State(X = 0x10u))
    }

    @Test
    fun indirectIndexed() {
      memory.store(0x0030u, 0x20u)
      memory.store(0x0031u, 0x12u)

      assertStores(target = 0x1230u, encoding = enc(0x91, 0x30), state = State(Y = 0x10u))
    }

    private fun assertStores(target: UInt16, encoding: Array<UInt8>, state: State = State()) {
      cpu.execute(encoding, state.with(A = 0x69u))

      assertEquals(0x69u.u8(), memory.load(target))
    }
  }

  @Nested
  inner class Stx {
    // TODO - flag sweep

    @Test
    fun zeroPage() {
      assertStores(target = 0x0030u, encoding = enc(0x86, 0x30))
    }

    @Test
    fun zeroPageY() {
      assertStores(target = 0x0030u, encoding = enc(0x96, 0x10), state = State(Y = 0x20u))
    }

    @Test
    fun absolute() {
      assertStores(target = 0x1230u, encoding = enc(0x8E, 0x30, 0x12))
    }

    private fun assertStores(target: UInt16, encoding: Array<UInt8>, state: State = State()) {
      cpu.execute(encoding, state.with(X = 0x69u))

      assertEquals(0x69u.u8(), memory.load(target))
    }
  }

  @Nested
  inner class Sty {
    // TODO - flag sweep

    @Test
    fun zeroPage() {
      assertStores(target = 0x0030u, encoding = enc(0x84, 0x30))
    }

    @Test
    fun zeroPageX() {
      assertStores(target = 0x0030u, encoding = enc(0x94, 0x10), state = State(X = 0x20u))
    }

    @Test
    fun absolute() {
      assertStores(target = 0x1230u, encoding = enc(0x8C, 0x30, 0x12))
    }

    private fun assertStores(target: UInt16, encoding: Array<UInt8>, state: State = State()) {
      cpu.execute(encoding, state.with(Y = 0x69u))

      assertEquals(0x69u.u8(), memory.load(target))
    }
  }
}
