import choliver.sixfiveohtwo.Alu
import choliver.sixfiveohtwo.Alu.Input
import choliver.sixfiveohtwo.Alu.Output
import choliver.sixfiveohtwo.AluMode.*
import choliver.sixfiveohtwo._0
import choliver.sixfiveohtwo._1
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class AluTest {
  private val alu = Alu()

  @Test
  fun asl() {
    assertEquals(Output(z = 0x00u, c = _0), alu.execute(ASL, Input(a = 0x00u)))
    assertEquals(Output(z = 0x02u, c = _0), alu.execute(ASL, Input(a = 0x01u)))
    assertEquals(Output(z = 0x80u, c = _0), alu.execute(ASL, Input(a = 0x40u)))
    assertEquals(Output(z = 0x00u, c = _1), alu.execute(ASL, Input(a = 0x80u)))
    assertEquals(Output(z = 0x02u, c = _0), alu.execute(ASL, Input(a = 0x01u, c = _1)))  // Ignores carry-in
  }

  @Test
  fun lsr() {
    assertEquals(Output(z = 0x00u, c = _0), alu.execute(LSR, Input(a = 0x00u)))
    assertEquals(Output(z = 0x40u, c = _0), alu.execute(LSR, Input(a = 0x80u)))
    assertEquals(Output(z = 0x01u, c = _0), alu.execute(LSR, Input(a = 0x02u)))
    assertEquals(Output(z = 0x00u, c = _1), alu.execute(LSR, Input(a = 0x01u)))
    assertEquals(Output(z = 0x40u, c = _0), alu.execute(LSR, Input(a = 0x80u, c = _1)))  // Ignores carry-in
  }

  @Test
  fun rol() {
    assertEquals(Output(z = 0x00u, c = _0), alu.execute(ROL, Input(a = 0x00u)))
    assertEquals(Output(z = 0x02u, c = _0), alu.execute(ROL, Input(a = 0x01u)))
    assertEquals(Output(z = 0x80u, c = _0), alu.execute(ROL, Input(a = 0x40u)))
    assertEquals(Output(z = 0x00u, c = _1), alu.execute(ROL, Input(a = 0x80u)))
    assertEquals(Output(z = 0x03u, c = _0), alu.execute(ROL, Input(a = 0x01u, c = _1)))
  }

  @Test
  fun ror() {
    assertEquals(Output(z = 0x00u, c = _0), alu.execute(ROR, Input(a = 0x00u)))
    assertEquals(Output(z = 0x40u, c = _0), alu.execute(ROR, Input(a = 0x80u)))
    assertEquals(Output(z = 0x01u, c = _0), alu.execute(ROR, Input(a = 0x02u)))
    assertEquals(Output(z = 0x00u, c = _1), alu.execute(ROR, Input(a = 0x01u)))
    assertEquals(Output(z = 0xC0u, c = _0), alu.execute(ROR, Input(a = 0x80u, c = _1)))
  }
}
