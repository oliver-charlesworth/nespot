package choliver.sixfiveohtwo

import choliver.sixfiveohtwo.Alu.Output
import choliver.sixfiveohtwo.utils._0
import choliver.sixfiveohtwo.utils._1
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class AluTest {
  private val alu = Alu()

  @Test
  fun asl() {
    assertEquals(Output(q = 0x00u, c = _0), alu.asl(q = 0x00u))
    assertEquals(Output(q = 0x02u, c = _0), alu.asl(q = 0x01u))
    assertEquals(Output(q = 0x80u, c = _0), alu.asl(q = 0x40u))
    assertEquals(Output(q = 0x00u, c = _1), alu.asl(q = 0x80u))
  }

  @Test
  fun lsr() {
    assertEquals(Output(q = 0x00u, c = _0), alu.lsr(q = 0x00u))
    assertEquals(Output(q = 0x40u, c = _0), alu.lsr(q = 0x80u))
    assertEquals(Output(q = 0x01u, c = _0), alu.lsr(q = 0x02u))
    assertEquals(Output(q = 0x00u, c = _1), alu.lsr(q = 0x01u))
  }

  @Test
  fun rol() {
    assertEquals(Output(q = 0x00u, c = _0), alu.rol(q = 0x00u, c = _0))
    assertEquals(Output(q = 0x02u, c = _0), alu.rol(q = 0x01u, c = _0))
    assertEquals(Output(q = 0x80u, c = _0), alu.rol(q = 0x40u, c = _0))
    assertEquals(Output(q = 0x00u, c = _1), alu.rol(q = 0x80u, c = _0))
    assertEquals(Output(q = 0x03u, c = _0), alu.rol(q = 0x01u, c = _1))
  }

  @Test
  fun ror() {
    assertEquals(Output(q = 0x00u, c = _0), alu.ror(q = 0x00u, c = _0))
    assertEquals(Output(q = 0x40u, c = _0), alu.ror(q = 0x80u, c = _0))
    assertEquals(Output(q = 0x01u, c = _0), alu.ror(q = 0x02u, c = _0))
    assertEquals(Output(q = 0x00u, c = _1), alu.ror(q = 0x01u, c = _0))
    assertEquals(Output(q = 0xC0u, c = _0), alu.ror(q = 0x80u, c = _1))
  }
}
