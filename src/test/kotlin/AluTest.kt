import choliver.sixfiveohtwo.*
import choliver.sixfiveohtwo.Alu.Input
import choliver.sixfiveohtwo.Alu.Output
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource

class AluTest {
  private val alu = Alu()

  @Test
  fun asl() {
    assertEquals(Output(z = 0x00u, c = _0), alu.asl(Input(a = 0x00u)))
    assertEquals(Output(z = 0x02u, c = _0), alu.asl(Input(a = 0x01u)))
    assertEquals(Output(z = 0x80u, c = _0), alu.asl(Input(a = 0x40u)))
    assertEquals(Output(z = 0x00u, c = _1), alu.asl(Input(a = 0x80u)))
    assertEquals(Output(z = 0x02u, c = _0), alu.asl(Input(a = 0x01u, c = _1)))  // Ignores carry-in
  }

  @Test
  fun lsr() {
    assertEquals(Output(z = 0x00u, c = _0), alu.lsr(Input(a = 0x00u)))
    assertEquals(Output(z = 0x40u, c = _0), alu.lsr(Input(a = 0x80u)))
    assertEquals(Output(z = 0x01u, c = _0), alu.lsr(Input(a = 0x02u)))
    assertEquals(Output(z = 0x00u, c = _1), alu.lsr(Input(a = 0x01u)))
    assertEquals(Output(z = 0x40u, c = _0), alu.lsr(Input(a = 0x80u, c = _1)))  // Ignores carry-in
  }

  @Test
  fun rol() {
    assertEquals(Output(z = 0x00u, c = _0), alu.rol(Input(a = 0x00u)))
    assertEquals(Output(z = 0x02u, c = _0), alu.rol(Input(a = 0x01u)))
    assertEquals(Output(z = 0x80u, c = _0), alu.rol(Input(a = 0x40u)))
    assertEquals(Output(z = 0x00u, c = _1), alu.rol(Input(a = 0x80u)))
    assertEquals(Output(z = 0x03u, c = _0), alu.rol(Input(a = 0x01u, c = _1)))
  }

  @Test
  fun ror() {
    assertEquals(Output(z = 0x00u, c = _0), alu.ror(Input(a = 0x00u)))
    assertEquals(Output(z = 0x40u, c = _0), alu.ror(Input(a = 0x80u)))
    assertEquals(Output(z = 0x01u, c = _0), alu.ror(Input(a = 0x02u)))
    assertEquals(Output(z = 0x00u, c = _1), alu.ror(Input(a = 0x01u)))
    assertEquals(Output(z = 0xC0u, c = _0), alu.ror(Input(a = 0x80u, c = _1)))
  }
}
