package choliver.sixfiveohtwo.cpu

import choliver.sixfiveohtwo.*
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test

class StackTest {
  // TODO - unify with Utils

  @Test
  fun pha() {
    val memory = FakeMemory()
    val cpu = Cpu(memory)

    assertEquals(
      State(S = 0x2Fu, A = 0x20u),
      cpu.execute(enc(0x48), State(S = 0x30u, A = 0x20u))
    )

    memory.assertStores(mapOf(0x0130 to 0x20))
  }

  @Test
  fun php() {
    val memory = FakeMemory()
    val cpu = Cpu(memory)

    assertEquals(
      State(S = 0x2Fu, P = 0xCF.u8().toFlags()),
      cpu.execute(enc(0x08), State(S = 0x30u, P = 0xCF.u8().toFlags()))
    )

    memory.assertStores(mapOf(0x0130 to 0xCF))
  }

  @Test
  fun pla() {
    val memory = FakeMemory(mapOf(0x123 to 0x30))
    val cpu = Cpu(memory)

    assertEquals(
      State(S = 0x23u, A = 0x30u),
      cpu.execute(enc(0x68), State(S = 0x22u))
    )

    memory.assertStores(emptyMap())
  }

  @Test
  fun plp() {
    val memory = FakeMemory(mapOf(0x123 to 0xCF))
    val cpu = Cpu(memory)

    assertEquals(
      State(S = 0x23u, P = 0xCF.u8().toFlags()),
      cpu.execute(enc(0x28), State(S = 0x22u))
    )

    memory.assertStores(emptyMap())
  }
}
