package choliver.nespot.ppu

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class PaletteTest {
  private val palette = Palette()

  @Test
  fun `limits data to 6 bits`() {
    palette[0] = 65

    assertEquals(1, palette[0])
  }

  @Test
  fun `treats non-mirrors as regular memory`() {
    val nonMirrors = (0..31) - listOf(16, 20, 24, 28)

    for (addr in nonMirrors) {
      palette[addr] = 63 - addr
    }

    for (addr in nonMirrors) {
      assertEquals(63 - addr, palette[addr])
    }
  }

  @Test
  fun `mirrors sprite zero-addresses to background zero-addresses`() {
    val mirrors = listOf(16, 20, 24, 28)

    for (addr in mirrors) {
      palette[addr] = 63 - addr
      assertEquals(63 - addr, palette[addr - 16])
    }

    for (addr in mirrors) {
      palette[addr - 16] = 63 - (addr - 16)
      assertEquals(63 - (addr - 16), palette[addr])
    }
  }
}
