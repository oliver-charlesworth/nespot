package choliver.nes.ppu

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class PaletteTest {
  private val palette = Palette()

  @Test
  fun `treats non-mirrors as regular memory`() {
    val nonMirrors = (0..31) - listOf(16, 20, 24, 28)

    for (addr in nonMirrors) {
      palette.store(addr, addr * 4 + 3)
    }

    for (addr in nonMirrors) {
      assertEquals(addr * 4 + 3, palette.load(addr))
    }
  }

  @Test
  fun `mirrors sprite zero-addresses to background zero-addresses`() {
    val mirrors = listOf(16, 20, 24, 28)

    for (addr in mirrors) {
      palette.store(addr, addr * 4 + 3)
      assertEquals(addr * 4 + 3, palette.load(addr - 16))
    }

    for (addr in mirrors) {
      palette.store(addr - 16, addr * 4 + 2)
      assertEquals(addr * 4 + 2, palette.load(addr))
    }
  }
}
