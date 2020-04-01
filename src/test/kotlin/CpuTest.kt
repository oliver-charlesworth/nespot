import choliver.sixfiveohtwo.*
import choliver.sixfiveohtwo.AddressMode.*
import choliver.sixfiveohtwo.AddressMode.IndexSource.*
import choliver.sixfiveohtwo.Opcode.*
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource

class CpuTest {
  private val memory = FakeMemory()
  private val cpu = Cpu(memory)

  @Test
  fun wat() {
    val s0 = State()
    val s1 = cpu.execute(Instruction(ADC, Immediate(0x69u)), s0)
    val s2 = cpu.execute(Instruction(SEC), s1)
    val s3 = cpu.execute(Instruction(SBC, Immediate(0x69u)), s2)

    println(s0)
    println(s1)
    println(s2)
    println(s3)
  }

  // TODO - exercise other address modes
  // TODO - assert PC

  // Each case implemented twice to demonstrate flag setting respects carry in:
  // (1) basic, (2) carry-in set with (operand + 1)
  @ParameterizedTest(name = "carry = {0}")
  @ValueSource(booleans = [_0, _1])
  fun adc(carry: Boolean) {
    fun State.adjust() = if (carry) with(A = (A - 1u).toUByte(), C = _1) else with(C = _0)

    forOpcode(ADC) {
      // +ve + +ve -> +ve
      assertEquals(
        s.with(A = 0x60u, V = _0, C = _0, N = _0, Z = _0),
        s.with(A = 0x50u).adjust(),
        Immediate(0x10u)
      )

      // -ve + -ve => -ve
      assertEquals(
        s.with(A = 0xE0u, V = _0, C = _0, N = _1, Z = _0),
        s.with(A = 0x50u).adjust(),
        Immediate(0x90u)
      )

      // Unsigned carry out
      assertEquals(
        s.with(A = 0x20u, V = _0, C = _1, N = _0, Z = _0),
        s.with(A = 0x50u).adjust(),
        Immediate(0xD0u)
      )

      // Unsigned carry out, result is -ve
      assertEquals(
        s.with(A = 0xA0u, V = _0, C = _1, N = _1, Z = _0),
        s.with(A = 0xD0u).adjust(),
        Immediate(0xD0u)
      )

      // +ve + +ve -> -ve (overflow)
      assertEquals(
        s.with(A = 0xA0u, V = _1, C = _0, N = _1, Z = _0),
        s.with(A = 0x50u).adjust(),
        Immediate(0x50u)
      )

      // -ve + -ve -> +ve (overflow)
      assertEquals(
        s.with(A = 0x60u, V = _1, C = _1, N = _0, Z = _0),
        s.with(A = 0xD0u).adjust(),
        Immediate(0x90u)
      )

      // Result is zero
      assertEquals(
        s.with(A = 0x00u, V = _0, C = _1, N = _0, Z = _1),
        s.with(A = 0x01u).adjust(),
        Immediate(0xFFu)
      )
    }
  }

  // Each case implemented twice to demonstrate flag setting respects borrow in:
  // (1) basic, (2) borrow-in set with (operand + 1)
  @ParameterizedTest(name = "borrow = {0}")
  @ValueSource(booleans = [_0, _1])
  fun sbc(borrow: Boolean) {
    fun State.adjust() = if (borrow) with(A = (A + 1u).toUByte(), C = _0) else with(C = _1)

    forOpcode(SBC) {
      // +ve - -ve -> +ve
      assertEquals(
        s.with(A = 0x60u, V = _0, C = _0, N = _0, Z = _0),
        s.with(A = 0x50u).adjust(),
        Immediate(0xF0u)
      )

      // -ve - +ve => -ve
      assertEquals(
        s.with(A = 0xE0u, V = _0, C = _0, N = _1, Z = _0),
        s.with(A = 0x50u).adjust(),
        Immediate(0x70u)
      )

      // Unsigned carry out
      assertEquals(
        s.with(A = 0x20u, V = _0, C = _1, N = _0, Z = _0),
        s.with(A = 0x50u).adjust(),
        Immediate(0x30u)
      )

      // Unsigned carry out, result is -ve
      assertEquals(
        s.with(A = 0xA0u, V = _0, C = _1, N = _1, Z = _0),
        s.with(A = 0xD0u).adjust(),
        Immediate(0x30u)
      )

      // +ve - -ve -> -ve (overflow)
      assertEquals(
        s.with(A = 0xA0u, V = _1, C = _0, N = _1, Z = _0),
        s.with(A = 0x50u).adjust(),
        Immediate(0xB0u)
      )

      // -ve - +ve -> +ve (overflow)
      assertEquals(
        s.with(A = 0x60u, V = _1, C = _1, N = _0, Z = _0),
        s.with(A = 0xD0u).adjust(),
        Immediate(0x70u)
      )

      // Result is zero
      assertEquals(
        s.with(A = 0x00u, V = _0, C = _1, N = _0, Z = _1),
        s.with(A = 0x01u).adjust(),
        Immediate(0x01u)
      )
    }
  }

  // TODO - dec
  // TODO - inc

  @Test
  fun dex() {
    forOpcode(DEX) {
      assertEquals(s.with(X = 0x01u, Z = _0, N = _0), s.with(X = 0x02u))
      assertEquals(s.with(X = 0x00u, Z = _1, N = _0), s.with(X = 0x01u))
      assertEquals(s.with(X = 0xFEu, Z = _0, N = _1), s.with(X = 0xFFu))
    }
  }

  @Test
  fun dey() {
    forOpcode(DEY) {
      assertEquals(s.with(Y = 0x01u, Z = _0, N = _0), s.with(Y = 0x02u))
      assertEquals(s.with(Y = 0x00u, Z = _1, N = _0), s.with(Y = 0x01u))
      assertEquals(s.with(Y = 0xFEu, Z = _0, N = _1), s.with(Y = 0xFFu))
    }
  }

  @Test
  fun inx() {
    forOpcode(INX) {
      assertEquals(s.with(X = 0x02u,  Z = _0, N = _0), s.with(X = 0x01u))
      assertEquals(s.with(X = 0x00u, Z = _1, N = _0), s.with(X = 0xFFu))
      assertEquals(s.with(X = 0xFFu, Z = _0, N = _1), s.with(X = 0xFEu))
    }
  }

  @Test
  fun iny() {
    forOpcode(INY) {
      assertEquals(s.with(Y = 0x02u, Z = _0, N = _0), s.with(Y = 0x01u))
      assertEquals(s.with(Y = 0x00u, Z = _1, N = _0), s.with(Y = 0xFFu))
      assertEquals(s.with(Y = 0xFFu, Z = _0, N = _1), s.with(Y = 0xFEu))
    }
  }

  @Test
  fun and() {
    forOpcode(AND) {
      assertEquals(s.with(A = 0x01u, Z = _0, N = _0), s.with(A = 0x11u), Immediate(0x23u))
      assertEquals(s.with(A = 0x00u, Z = _1, N = _0), s.with(A = 0x11u), Immediate(0x22u))
      assertEquals(s.with(A = 0x81u, Z = _0, N = _1), s.with(A = 0x81u), Immediate(0x83u))
    }
  }

  @Test
  fun ora() {
    forOpcode(ORA) {
      assertEquals(s.with(A = 0x33u, Z = _0, N = _0), s.with(A = 0x11u), Immediate(0x23u))
      assertEquals(s.with(A = 0x00u, Z = _1, N = _0), s.with(A = 0x00u), Immediate(0x00u))
      assertEquals(s.with(A = 0x83u, Z = _0, N = _1), s.with(A = 0x81u), Immediate(0x83u))
    }
  }

  @Test
  fun eor() {
    forOpcode(EOR) {
      assertEquals(s.with(A = 0x32u, Z = _0, N = _0), s.with(A = 0x11u), Immediate(0x23u))
      assertEquals(s.with(A = 0x00u, Z = _1, N = _0), s.with(A = 0x11u), Immediate(0x11u))
      assertEquals(s.with(A = 0x82u, Z = _0, N = _1), s.with(A = 0x81u), Immediate(0x03u))
    }
  }

  @Test
  fun clc() {
    forOpcode(CLC) {
      assertEquals(s.with(C = _0), s.with(C = _1))
      assertEquals(s.with(C = _0), s.with(C = _0))
    }
  }

  @Test
  fun cld() {
    forOpcode(CLD) {
      assertEquals(s.with(D = _0), s.with(D = _1))
      assertEquals(s.with(D = _0), s.with(D = _0))
    }
  }

  @Test
  fun cli() {
    forOpcode(CLI) {
      assertEquals(s.with(I = _0), s.with(I = _1))
      assertEquals(s.with(I = _0), s.with(I = _0))
    }
  }

  @Test
  fun clv() {
    forOpcode(CLV) {
      assertEquals(s.with(V = _0), s.with(V = _1))
      assertEquals(s.with(V = _0), s.with(V = _0))
    }
  }

  @Test
  fun sec() {
    forOpcode(SEC) {
      assertEquals(s.with(C = _1), s.with(C = _1))
      assertEquals(s.with(C = _1), s.with(C = _0))
    }
  }

  @Test
  fun sed() {
    forOpcode(SED) {
      assertEquals(s.with(D = _1), s.with(D = _1))
      assertEquals(s.with(D = _1), s.with(D = _0))
    }
  }

  @Test
  fun sei() {
    forOpcode(SEI) {
      assertEquals(s.with(I = _1), s.with(I = _1))
      assertEquals(s.with(I = _1), s.with(I = _0))
    }
  }

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
}
