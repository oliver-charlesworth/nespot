import choliver.sixfiveohtwo.*
import choliver.sixfiveohtwo.AddressMode.*
import choliver.sixfiveohtwo.Opcode.*
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
    fun State.adjust() = if (carry) copy(A = (A - 1u).toUByte()).withFlags(C = _1) else withFlags(C = _0)

    forOpcode(ADC) {
      // +ve + +ve -> +ve
      assertEquals(
        s.copy(A = 0x60u).withFlags(V = _0, C = _0, N = _0, Z = _0),
        s.copy(A = 0x50u).adjust(),
        Immediate(0x10u)
      )

      // -ve + -ve => -ve
      assertEquals(
        s.copy(A = 0xE0u).withFlags(V = _0, C = _0, N = _1, Z = _0),
        s.copy(A = 0x50u).adjust(),
        Immediate(0x90u)
      )

      // Unsigned carry out
      assertEquals(
        s.copy(A = 0x20u).withFlags(V = _0, C = _1, N = _0, Z = _0),
        s.copy(A = 0x50u).adjust(),
        Immediate(0xD0u)
      )

      // Unsigned carry out, result is -ve
      assertEquals(
        s.copy(A = 0xA0u).withFlags(V = _0, C = _1, N = _1, Z = _0),
        s.copy(A = 0xD0u).adjust(),
        Immediate(0xD0u)
      )

      // +ve + +ve -> -ve (overflow)
      assertEquals(
        s.copy(A = 0xA0u).withFlags(V = _1, C = _0, N = _1, Z = _0),
        s.copy(A = 0x50u).adjust(),
        Immediate(0x50u)
      )

      // -ve + -ve -> +ve (overflow)
      assertEquals(
        s.copy(A = 0x60u).withFlags(V = _1, C = _1, N = _0, Z = _0),
        s.copy(A = 0xD0u).adjust(),
        Immediate(0x90u)
      )

      // Result is zero
      assertEquals(
        s.copy(A = 0x00u).withFlags(V = _0, C = _1, N = _0, Z = _1),
        s.copy(A = 0x01u).adjust(),
        Immediate(0xFFu)
      )
    }
  }

  // Each case implemented twice to demonstrate flag setting respects borrow in:
  // (1) basic, (2) borrow-in set with (operand + 1)
  @ParameterizedTest(name = "borrow = {0}")
  @ValueSource(booleans = [_0, _1])
  fun sbc(borrow: Boolean) {
    fun State.adjust() = if (borrow) copy(A = (A + 1u).toUByte()).withFlags(C = _0) else withFlags(C = _1)

    forOpcode(SBC) {
      // +ve - -ve -> +ve
      assertEquals(
        s.copy(A = 0x60u).withFlags(V = _0, C = _0, N = _0, Z = _0),
        s.copy(A = 0x50u).adjust(),
        Immediate(0xF0u)
      )

      // -ve - +ve => -ve
      assertEquals(
        s.copy(A = 0xE0u).withFlags(V = _0, C = _0, N = _1, Z = _0),
        s.copy(A = 0x50u).adjust(),
        Immediate(0x70u)
      )

      // Unsigned carry out
      assertEquals(
        s.copy(A = 0x20u).withFlags(V = _0, C = _1, N = _0, Z = _0),
        s.copy(A = 0x50u).adjust(),
        Immediate(0x30u)
      )

      // Unsigned carry out, result is -ve
      assertEquals(
        s.copy(A = 0xA0u).withFlags(V = _0, C = _1, N = _1, Z = _0),
        s.copy(A = 0xD0u).adjust(),
        Immediate(0x30u)
      )

      // +ve - -ve -> -ve (overflow)
      assertEquals(
        s.copy(A = 0xA0u).withFlags(V = _1, C = _0, N = _1, Z = _0),
        s.copy(A = 0x50u).adjust(),
        Immediate(0xB0u)
      )

      // -ve - +ve -> +ve (overflow)
      assertEquals(
        s.copy(A = 0x60u).withFlags(V = _1, C = _1, N = _0, Z = _0),
        s.copy(A = 0xD0u).adjust(),
        Immediate(0x70u)
      )

      // Result is zero
      assertEquals(
        s.copy(A = 0x00u).withFlags(V = _0, C = _1, N = _0, Z = _1),
        s.copy(A = 0x01u).adjust(),
        Immediate(0x01u)
      )
    }
  }

  // TODO - dec
  // TODO - inc

  @Test
  fun dex() {
    forOpcode(DEX) {
      assertEquals(s.copy(X = 0x01u).withFlags(Z = _0, N = _0), s.copy(X = 0x02u))
      assertEquals(s.copy(X = 0x00u).withFlags(Z = _1, N = _0), s.copy(X = 0x01u))
      assertEquals(s.copy(X = 0xFEu).withFlags(Z = _0, N = _1), s.copy(X = 0xFFu))
    }
  }

  @Test
  fun dey() {
    forOpcode(DEY) {
      assertEquals(s.copy(Y = 0x01u).withFlags(Z = _0, N = _0), s.copy(Y = 0x02u))
      assertEquals(s.copy(Y = 0x00u).withFlags(Z = _1, N = _0), s.copy(Y = 0x01u))
      assertEquals(s.copy(Y = 0xFEu).withFlags(Z = _0, N = _1), s.copy(Y = 0xFFu))
    }
  }

  @Test
  fun inx() {
    forOpcode(INX) {
      assertEquals(s.copy(X = 0x02u).withFlags( Z = _0, N = _0), s.copy(X = 0x01u))
      assertEquals(s.copy(X = 0x00u).withFlags(Z = _1, N = _0), s.copy(X = 0xFFu))
      assertEquals(s.copy(X = 0xFFu).withFlags(Z = _0, N = _1), s.copy(X = 0xFEu))
    }
  }

  @Test
  fun iny() {
    forOpcode(INY) {
      assertEquals(s.copy(Y = 0x02u).withFlags(Z = _0, N = _0), s.copy(Y = 0x01u))
      assertEquals(s.copy(Y = 0x00u).withFlags(Z = _1, N = _0), s.copy(Y = 0xFFu))
      assertEquals(s.copy(Y = 0xFFu).withFlags(Z = _0, N = _1), s.copy(Y = 0xFEu))
    }
  }

  @Test
  fun and() {
    forOpcode(AND) {
      assertEquals(s.copy(A = 0x01u).withFlags(Z = _0, N = _0), s.copy(A = 0x11u), Immediate(0x23u))
      assertEquals(s.copy(A = 0x00u).withFlags(Z = _1, N = _0), s.copy(A = 0x11u), Immediate(0x22u))
      assertEquals(s.copy(A = 0x81u).withFlags(Z = _0, N = _1), s.copy(A = 0x81u), Immediate(0x83u))
    }
  }

  @Test
  fun ora() {
    forOpcode(ORA) {
      assertEquals(s.copy(A = 0x33u).withFlags(Z = _0, N = _0), s.copy(A = 0x11u), Immediate(0x23u))
      assertEquals(s.copy(A = 0x00u).withFlags(Z = _1, N = _0), s.copy(A = 0x00u), Immediate(0x00u))
      assertEquals(s.copy(A = 0x83u).withFlags(Z = _0, N = _1), s.copy(A = 0x81u), Immediate(0x83u))
    }
  }

  @Test
  fun eor() {
    forOpcode(EOR) {
      assertEquals(s.copy(A = 0x32u).withFlags(Z = _0, N = _0), s.copy(A = 0x11u), Immediate(0x23u))
      assertEquals(s.copy(A = 0x00u).withFlags(Z = _1, N = _0), s.copy(A = 0x11u), Immediate(0x11u))
      assertEquals(s.copy(A = 0x82u).withFlags(Z = _0, N = _1), s.copy(A = 0x81u), Immediate(0x03u))
    }
  }

  @Test
  fun clc() {
    forOpcode(CLC) {
      assertEquals(s.withFlags(C = _0), s.withFlags(C = _1))
      assertEquals(s.withFlags(C = _0), s.withFlags(C = _0))
    }
  }

  @Test
  fun cld() {
    forOpcode(CLD) {
      assertEquals(s.withFlags(D = _0), s.withFlags(D = _1))
      assertEquals(s.withFlags(D = _0), s.withFlags(D = _0))
    }
  }

  @Test
  fun cli() {
    forOpcode(CLI) {
      assertEquals(s.withFlags(I = _0), s.withFlags(I = _1))
      assertEquals(s.withFlags(I = _0), s.withFlags(I = _0))
    }
  }

  @Test
  fun clv() {
    forOpcode(CLV) {
      assertEquals(s.withFlags(V = _0), s.withFlags(V = _1))
      assertEquals(s.withFlags(V = _0), s.withFlags(V = _0))
    }
  }

  @Test
  fun sec() {
    forOpcode(SEC) {
      assertEquals(s.withFlags(C = _1), s.withFlags(C = _1))
      assertEquals(s.withFlags(C = _1), s.withFlags(C = _0))
    }
  }

  @Test
  fun sed() {
    forOpcode(SED) {
      assertEquals(s.withFlags(D = _1), s.withFlags(D = _1))
      assertEquals(s.withFlags(D = _1), s.withFlags(D = _0))
    }
  }

  @Test
  fun sei() {
    forOpcode(SEI) {
      assertEquals(s.withFlags(I = _1), s.withFlags(I = _1))
      assertEquals(s.withFlags(I = _1), s.withFlags(I = _0))
    }
  }

  // TODO - address modes
  @Test
  fun lda() {
    forOpcode(LDA) {
      assertEquals(s.copy(A = 0x69u).withFlags(Z = _0, N = _0), s, Immediate(0x69u))
      assertEquals(s.copy(A = 0x00u).withFlags(Z = _1, N = _0), s, Immediate(0x00u))
      assertEquals(s.copy(A = 0x96u).withFlags(Z = _0, N = _1), s, Immediate(0x96u))
    }
  }

  // TODO - address modes
  @Test
  fun ldx() {
    forOpcode(LDX) {
      assertEquals(s.copy(X = 0x69u).withFlags(Z = _0, N = _0), s, Immediate(0x69u))
      assertEquals(s.copy(X = 0x00u).withFlags(Z = _1, N = _0), s, Immediate(0x00u))
      assertEquals(s.copy(X = 0x96u).withFlags(Z = _0, N = _1), s, Immediate(0x96u))
    }
  }

  // TODO - address modes
  @Test
  fun ldy() {
    forOpcode(LDY) {
      assertEquals(s.copy(Y = 0x69u).withFlags(Z = _0, N = _0), s, Immediate(0x69u))
      assertEquals(s.copy(Y = 0x00u).withFlags(Z = _1, N = _0), s, Immediate(0x00u))
      assertEquals(s.copy(Y = 0x96u).withFlags(Z = _0, N = _1), s, Immediate(0x96u))
    }
  }
}
