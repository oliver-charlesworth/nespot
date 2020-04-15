package choliver.nes.ppu

import choliver.nes.Address
import choliver.nes.Data
import choliver.nes.Memory
import choliver.nes.data

class Palette : Memory {
  private val raw = ByteArray(32) { 0x00.toByte() }

  override fun load(addr: Address): Data = raw[map(addr and 31)].data()

  override fun store(addr: Address, data: Data) {
    raw[map(addr and 31)] = data.toByte()
  }

  // See http://wiki.nesdev.com/w/index.php/PPU_palettes#Memory_Map
  private fun map(addr: Address) = if (addr in listOf(0x10, 0x14, 0x18, 0x1C)) (addr - 0x10) else addr
}
