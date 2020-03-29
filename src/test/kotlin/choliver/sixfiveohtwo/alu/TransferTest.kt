package choliver.sixfiveohtwo.alu

import choliver.sixfiveohtwo.State
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class TransferTest {
  private val alu = Alu()

  @Test
  fun `tax transfers and updates Z,N`() {
    assertEquals(State(A = 0x11u, X = 0x11u, Z = false, N = false), alu.tax(State(A = 0x11u, X = 0x22u)))
    assertEquals(State(A = 0x00u, X = 0x00u, Z = true, N = false), alu.tax(State(A = 0x00u, X = 0x22u)))
    assertEquals(State(A = 0xFFu, X = 0xFFu, Z = false, N = true), alu.tax(State(A = 0xFFu, X = 0x22u)))
  }

  @Test
  fun `tay transfers and updates Z,N`() {
    assertEquals(State(A = 0x11u, Y = 0x11u, Z = false, N = false), alu.tay(State(A = 0x11u, Y = 0x22u)))
    assertEquals(State(A = 0x00u, Y = 0x00u, Z = true, N = false), alu.tay(State(A = 0x00u, Y = 0x22u)))
    assertEquals(State(A = 0xFFu, Y = 0xFFu, Z = false, N = true), alu.tay(State(A = 0xFFu, Y = 0x22u)))
  }

  @Test
  fun `tsx transfers and doesn't update Z,N`() {
    assertEquals(State(S = 0x11u, X = 0x11u), alu.tsx(State(S = 0x11u, X = 0x22u)))
    assertEquals(State(S = 0x00u, X = 0x00u), alu.tsx(State(S = 0x00u, X = 0x22u)))
    assertEquals(State(S = 0xFFu, X = 0xFFu), alu.tsx(State(S = 0xFFu, X = 0x22u)))
  }

  @Test
  fun `txa transfers and updates Z,N`() {
    assertEquals(State(A = 0x11u, X = 0x11u, Z = false, N = false), alu.txa(State(A = 0x22u, X = 0x11u)))
    assertEquals(State(A = 0x00u, X = 0x00u, Z = true, N = false), alu.txa(State(A = 0x22u, X = 0x00u)))
    assertEquals(State(A = 0xFFu, X = 0xFFu, Z = false, N = true), alu.txa(State(A = 0x22u, X = 0xFFu)))
  }

  @Test
  fun `txs transfers and doesn't update Z,N`() {
    assertEquals(State(S = 0x11u, X = 0x11u), alu.txs(State(S = 0x22u, X = 0x11u)))
    assertEquals(State(S = 0x00u, X = 0x00u), alu.txs(State(S = 0x22u, X = 0x00u)))
    assertEquals(State(S = 0xFFu, X = 0xFFu), alu.txs(State(S = 0x22u, X = 0xFFu)))
  }

  @Test
  fun `tya transfers and updates Z,N`() {
    assertEquals(State(A = 0x11u, Y = 0x11u, Z = false, N = false), alu.tya(State(A = 0x22u, Y = 0x11u)))
    assertEquals(State(A = 0x00u, Y = 0x00u, Z = true, N = false), alu.tya(State(A = 0x22u, Y = 0x00u)))
    assertEquals(State(A = 0xFFu, Y = 0xFFu, Z = false, N = true), alu.tya(State(A = 0x22u, Y = 0xFFu)))
  }
}
