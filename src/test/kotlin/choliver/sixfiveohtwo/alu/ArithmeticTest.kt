package choliver.sixfiveohtwo.alu

import choliver.sixfiveohtwo.State
import org.junit.jupiter.api.Test


class ArithmeticTest {
  private val alu = Alu()

  @Test
  fun adc() {
    assertEqualsID(State(A = 0x60u, V = false, C = false, N = false), State(A = 0x50u)) { alu.adc(it, 0x10u) }
    assertEqualsID(State(A = 0xE0u, V = false, C = false, N = true), State(A = 0x50u)) { alu.adc(it, 0x90u) }
    assertEqualsID(State(A = 0x20u, V = false, C = true, N = false), State(A = 0x50u)) { alu.adc(it, 0xD0u) }
    assertEqualsID(State(A = 0xA0u, V = false, C = true, N = true), State(A = 0xD0u)) { alu.adc(it, 0xD0u) }
    // {V = true, C = false, N = false} not possible
    assertEqualsID(State(A = 0xA0u, V = true, C = false, N = true), State(A = 0x50u)) { alu.adc(it, 0x50u) }
    assertEqualsID(State(A = 0x60u, V = true, C = true, N = false), State(A = 0xD0u)) { alu.adc(it, 0x90u) }
    // {V = true, C = true, N = true} not possible
    assertEqualsID(State(A = 0x00u, V = false, C = true, N = false, Z = true), State(A = 0x01u)) { alu.adc(it, 0xFFu) }
  }

  @Test
  fun dex() {
    assertEqualsIDCV(State(X = 0x01u, Z = false, N = false), State(X = 0x02u)) { alu.dex(it) }
    assertEqualsIDCV(State(X = 0x00u, Z = true, N = false), State(X = 0x01u)) { alu.dex(it) }
    assertEqualsIDCV(State(X = 0xFEu, Z = false, N = true), State(X = 0xFFu)) { alu.dex(it) }
  }

  @Test
  fun dey() {
    assertEqualsIDCV(State(Y = 0x01u, Z = false, N = false), State(Y = 0x02u)) { alu.dey(it) }
    assertEqualsIDCV(State(Y = 0x00u, Z = true, N = false), State(Y = 0x01u)) { alu.dey(it) }
    assertEqualsIDCV(State(Y = 0xFEu, Z = false, N = true), State(Y = 0xFFu)) { alu.dey(it) }
  }

  @Test
  fun inx() {
    assertEqualsIDCV(State(X = 0x02u, Z = false, N = false), State(X = 0x01u)) { alu.inx(it) }
    assertEqualsIDCV(State(X = 0x00u, Z = true, N = false), State(X = 0xFFu)) { alu.inx(it) }
    assertEqualsIDCV(State(X = 0xFFu, Z = false, N = true), State(X = 0xFEu)) { alu.inx(it) }
  }

  @Test
  fun iny() {
    assertEqualsIDCV(State(Y = 0x02u, Z = false, N = false), State(Y = 0x01u)) { alu.iny(it) }
    assertEqualsIDCV(State(Y = 0x00u, Z = true, N = false), State(Y = 0xFFu)) { alu.iny(it) }
    assertEqualsIDCV(State(Y = 0xFFu, Z = false, N = true), State(Y = 0xFEu)) { alu.iny(it) }
  }
}
