package choliver.nes.cartridge

import org.junit.jupiter.api.Test

class CartridgeTest {

  @Test
  fun yeah() {
    val raw = javaClass.getResource("/smb.nes").readBytes()

    val rom = Cartridge(raw)
  }
}
