import choliver.sixfiveohtwo.Alu
import choliver.sixfiveohtwo.Alu.Input
import choliver.sixfiveohtwo.Alu.Output
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource

class AluTest {
  private val alu = Alu()

  // Each case implemented twice to demonstrate flag setting respects carry in:
  // (1) basic, (2) carry-in set with (a - 1)
  @ParameterizedTest(name = "carry = {0}")
  @ValueSource(booleans = [_0, _1])
  fun adc(carry: Boolean) {
    fun input(a: UByte, b: UByte) = if (carry) {
      Input(a = (a - 1u).toUByte(), b = b, c = _1)
    } else {
      Input(a = a, b = b, c = _0)
    }

    // +ve + +ve -> +ve
    assertEquals(Output(x = 0x60u, v = _0, c = _0), alu.adc(input(0x50u, 0x10u)))

    // -ve + -ve => -ve
    assertEquals(Output(x = 0xE0u, v = _0, c = _0), alu.adc(input(0x50u, 0x90u)))

    // Unsigned carry out
    assertEquals(Output(x = 0x20u, v = _0, c = _1), alu.adc(input(0x50u, 0xD0u)))

    // Unsigned carry out, result is -ve
    assertEquals(Output(x = 0xA0u, v = _0, c = _1), alu.adc(input(0xD0u, 0xD0u)))

    // +ve + +ve -> -ve (overflow)
    assertEquals(Output(x = 0xA0u, v = _1, c = _0), alu.adc(input(0x50u, 0x50u)))

    // -ve + -ve -> +ve (overflow)
    assertEquals(Output(x = 0x60u, v = _1, c = _1), alu.adc(input(0xD0u, 0x90u)))
  }

  // Each case implemented twice to demonstrate flag setting respects borrow in:
  // (1) basic, (2) borrow-in set with (a + 1)
  @ParameterizedTest(name = "borrow = {0}")
  @ValueSource(booleans = [_0, _1])
  fun sbc(borrow: Boolean) {
    fun input(a: UByte, b: UByte) = if (borrow) {
      Input(a = (a + 1u).toUByte(), b = b, c = _0)
    } else {
      Input(a = a, b = b, c = _1)
    }

    // +ve - -ve -> +ve
    assertEquals(Output(x = 0x60u, v = _0, c = _0), alu.sbc(input(0x50u, 0xF0u)))

    // -ve - +ve => -ve
    assertEquals(Output(x = 0xE0u, v = _0, c = _0), alu.sbc(input(0x50u, 0x70u)))

    // Unsigned carry out
    assertEquals(Output(x = 0x20u, v = _0, c = _1), alu.sbc(input(0x50u, 0x30u)))

    // Unsigned carry out, result is -ve
    assertEquals(Output(x = 0xA0u, v = _0, c = _1), alu.sbc(input(0xD0u, 0x30u)))

    // +ve - -ve -> -ve (overflow)
    assertEquals(Output(x = 0xA0u, v = _1, c = _0), alu.sbc(input(0x50u, 0xB0u)))

    // -ve - +ve -> +ve (overflow)
    assertEquals(Output(x = 0x60u, v = _1, c = _1), alu.sbc(input(0xD0u, 0x70u)))
  }

  @Test
  fun dec() {
    assertEquals(Output(x = 0x01u), alu.dec(Input(a = 0x02u)))
    assertEquals(Output(x = 0x00u), alu.dec(Input(a = 0x01u)))
    assertEquals(Output(x = 0xFFu), alu.dec(Input(a = 0x00u)))
  }

  @Test
  fun inc() {
    assertEquals(Output(x = 0x01u), alu.inc(Input(a = 0x00u)))
    assertEquals(Output(x = 0x02u), alu.inc(Input(a = 0x01u)))
    assertEquals(Output(x = 0x00u), alu.inc(Input(a = 0xFFu)))
  }

  @Test
  fun and() {
    assertEquals(Output(x = 0x01u), alu.and(Input(a = 0x11u, b = 0x23u)))
  }

  @Test
  fun ora() {
    assertEquals(Output(x = 0x33u), alu.ora(Input(a = 0x11u, b = 0x23u)))
  }

  @Test
  fun eor() {
    assertEquals(Output(x = 0x32u), alu.eor(Input(a = 0x11u, b = 0x23u)))
  }

  @Test
  fun asl() {
    assertEquals(Output(x = 0x00u, c = _0), alu.asl(Input(a = 0x00u)))
    assertEquals(Output(x = 0x02u, c = _0), alu.asl(Input(a = 0x01u)))
    assertEquals(Output(x = 0x80u, c = _0), alu.asl(Input(a = 0x40u)))
    assertEquals(Output(x = 0x00u, c = _1), alu.asl(Input(a = 0x80u)))
    assertEquals(Output(x = 0x02u, c = _0), alu.asl(Input(a = 0x01u, c = _1)))  // Ignores carry-in
  }

  @Test
  fun lsr() {
    assertEquals(Output(x = 0x00u, c = _0), alu.lsr(Input(a = 0x00u)))
    assertEquals(Output(x = 0x40u, c = _0), alu.lsr(Input(a = 0x80u)))
    assertEquals(Output(x = 0x01u, c = _0), alu.lsr(Input(a = 0x02u)))
    assertEquals(Output(x = 0x00u, c = _1), alu.lsr(Input(a = 0x01u)))
    assertEquals(Output(x = 0x40u, c = _0), alu.lsr(Input(a = 0x80u, c = _1)))  // Ignores carry-in
  }

  @Test
  fun rol() {
    assertEquals(Output(x = 0x00u, c = _0), alu.rol(Input(a = 0x00u)))
    assertEquals(Output(x = 0x02u, c = _0), alu.rol(Input(a = 0x01u)))
    assertEquals(Output(x = 0x80u, c = _0), alu.rol(Input(a = 0x40u)))
    assertEquals(Output(x = 0x00u, c = _1), alu.rol(Input(a = 0x80u)))
    assertEquals(Output(x = 0x03u, c = _0), alu.rol(Input(a = 0x01u, c = _1)))
  }

  @Test
  fun ror() {
    assertEquals(Output(x = 0x00u, c = _0), alu.ror(Input(a = 0x00u)))
    assertEquals(Output(x = 0x40u, c = _0), alu.ror(Input(a = 0x80u)))
    assertEquals(Output(x = 0x01u, c = _0), alu.ror(Input(a = 0x02u)))
    assertEquals(Output(x = 0x00u, c = _1), alu.ror(Input(a = 0x01u)))
    assertEquals(Output(x = 0xC0u, c = _0), alu.ror(Input(a = 0x80u, c = _1)))
  }
}
