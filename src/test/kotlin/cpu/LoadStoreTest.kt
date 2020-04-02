package cpu

import FakeMemory
import assertForAddressModes
import choliver.sixfiveohtwo.*
import choliver.sixfiveohtwo.AddrMode.*
import enc
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import sweepStates

class LoadStoreTest {
  private val memory = FakeMemory()
  private val cpu = Cpu(memory)

  @Nested
  inner class Lda {
    private val ops = mapOf(
      IMMEDIATE to 0xA9,
      ZERO_PAGE to 0XA5,
      ZERO_PAGE_X to 0xB5,
      ABSOLUTE to 0xAD,
      ABSOLUTE_X to 0xBD,
      ABSOLUTE_Y to 0xB9,
      INDEXED_INDIRECT to 0xA1,
      INDIRECT_INDEXED to 0xB1
    )

    @Test
    fun positive() {
      assertForAddressModes(ops, 0x69) { with(A = 0x69u, Z = _0, N = _0) }
    }

    @Test
    fun negative() {
      assertForAddressModes(ops, 0x96) { with(A = 0x96u, Z = _0, N = _1) }
    }

    @Test
    fun zero() {
      assertForAddressModes(ops, 0x00) { with(A = 0x00u, Z = _1, N = _0) }
    }
  }

  @Nested
  inner class Ldx {
    private val ops = mapOf(
      IMMEDIATE to 0xA2,
      ZERO_PAGE to 0xA6,
      ZERO_PAGE_Y to 0xB6,
      ABSOLUTE to 0xAE,
      ABSOLUTE_Y to 0xBE
    )

    @Test
    fun positive() {
      assertForAddressModes(ops, 0x69) { with(X = 0x69u, Z = _0, N = _0) }
    }

    @Test
    fun negative() {
      assertForAddressModes(ops, 0x96) { with(X = 0x96u, Z = _0, N = _1) }
    }

    @Test
    fun zero() {
      assertForAddressModes(ops, 0x00) { with(X = 0x00u, Z = _1, N = _0) }
    }
  }

  @Nested
  inner class Ldy {
    private val ops = mapOf(
      IMMEDIATE to 0xA0,
      ZERO_PAGE to 0xA4,
      ZERO_PAGE_X to 0xB4,
      ABSOLUTE to 0xAC,
      ABSOLUTE_X to 0xBC
    )

    @Test
    fun positive() {
      assertForAddressModes(ops, 0x69) { with(Y = 0x69u, Z = _0, N = _0) }
    }

    @Test
    fun negative() {
      assertForAddressModes(ops, 0x96) { with(Y = 0x96u, Z = _0, N = _1) }
    }

    @Test
    fun zero() {
      assertForAddressModes(ops, 0x00) { with(Y = 0x00u, Z = _1, N = _0) }
    }
  }

  @Nested
  inner class Sta {
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

    @Test
    fun stateInvariants() {
      sweepStates {
        assertEquals(s, s, enc(0x85, 0x30))
      }
    }

    private fun assertStores(target: UInt16, encoding: Array<UInt8>, state: State = State()) {
      cpu.execute(encoding, state.with(A = 0x69u))

      assertEquals(0x69u.u8(), memory.load(target))
    }
  }

  @Nested
  inner class Stx {
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

    @Test
    fun stateInvariants() {
      sweepStates {
        assertEquals(s, s, enc(0x86, 0x30))
      }
    }

    private fun assertStores(target: UInt16, encoding: Array<UInt8>, state: State = State()) {
      cpu.execute(encoding, state.with(X = 0x69u))

      assertEquals(0x69u.u8(), memory.load(target))
    }
  }

  @Nested
  inner class Sty {
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

    @Test
    fun stateInvariants() {
      sweepStates {
        assertEquals(s, s, enc(0x84, 0x30))
      }
    }

    private fun assertStores(target: UInt16, encoding: Array<UInt8>, state: State = State()) {
      cpu.execute(encoding, state.with(Y = 0x69u))

      assertEquals(0x69u.u8(), memory.load(target))
    }
  }
}
