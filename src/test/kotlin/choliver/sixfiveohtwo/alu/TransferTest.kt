package choliver.sixfiveohtwo.alu

import choliver.sixfiveohtwo.State
import org.junit.jupiter.api.Test

class TransferTest {
  private val alu = Alu()

  @Test
  fun tax() {
    assertEqualsIDCV(State(A = 0x11u, X = 0x11u, Z = false, N = false), State(A = 0x11u, X = 0x22u), alu::tax)
    assertEqualsIDCV(State(A = 0x00u, X = 0x00u, Z = true, N = false), State(A = 0x00u, X = 0x22u), alu::tax)
    assertEqualsIDCV(State(A = 0xFFu, X = 0xFFu, Z = false, N = true), State(A = 0xFFu, X = 0x22u), alu::tax)
  }

  @Test
  fun tay() {
    assertEqualsIDCV(State(A = 0x11u, Y = 0x11u, Z = false, N = false), State(A = 0x11u, Y = 0x22u), alu::tay)
    assertEqualsIDCV(State(A = 0x00u, Y = 0x00u, Z = true, N = false), State(A = 0x00u, Y = 0x22u), alu::tay)
    assertEqualsIDCV(State(A = 0xFFu, Y = 0xFFu, Z = false, N = true), State(A = 0xFFu, Y = 0x22u), alu::tay)
  }

  @Test
  fun tsx() {
    assertEqualsIDCV(State(S = 0x11u, X = 0x11u, Z = false, N = false), State(S = 0x11u, X = 0x22u), alu::tsx)
    assertEqualsIDCV(State(S = 0x00u, X = 0x00u, Z = true, N = false), State(S = 0x00u, X = 0x22u), alu::tsx)
    assertEqualsIDCV(State(S = 0xFFu, X = 0xFFu, Z = false, N = true), State(S = 0xFFu, X = 0x22u), alu::tsx)
  }

  @Test
  fun txa() {
    assertEqualsIDCV(State(A = 0x11u, X = 0x11u, Z = false, N = false), State(A = 0x22u, X = 0x11u), alu::txa)
    assertEqualsIDCV(State(A = 0x00u, X = 0x00u, Z = true, N = false), State(A = 0x22u, X = 0x00u), alu::txa)
    assertEqualsIDCV(State(A = 0xFFu, X = 0xFFu, Z = false, N = true), State(A = 0x22u, X = 0xFFu), alu::txa)
  }

  @Test
  fun txs() {
    // Note - doesn't affect ZN
    assertEqualsIDCVZN(State(S = 0x11u, X = 0x11u), State(S = 0x22u, X = 0x11u), alu::txs)
    assertEqualsIDCVZN(State(S = 0x00u, X = 0x00u), State(S = 0x22u, X = 0x00u), alu::txs)
    assertEqualsIDCVZN(State(S = 0xFFu, X = 0xFFu), State(S = 0x22u, X = 0xFFu), alu::txs)
  }

  @Test
  fun tya() {
    assertEqualsIDCV(State(A = 0x11u, Y = 0x11u, Z = false, N = false), State(A = 0x22u, Y = 0x11u), alu::tya)
    assertEqualsIDCV(State(A = 0x00u, Y = 0x00u, Z = true, N = false), State(A = 0x22u, Y = 0x00u), alu::tya)
    assertEqualsIDCV(State(A = 0xFFu, Y = 0xFFu, Z = false, N = true), State(A = 0x22u, Y = 0xFFu), alu::tya)
  }
}
