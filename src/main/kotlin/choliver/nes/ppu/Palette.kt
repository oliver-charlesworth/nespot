package choliver.nes.ppu

import choliver.nes.Address
import choliver.nes.Data
import choliver.nes.Memory

class Palette : Memory {
  private val raw = IntArray(32) { 0x00 } // Not bytes, to avoid conversion overhead

  override fun load(addr: Address): Data = raw[mapMirrors(addr)]

  override fun store(addr: Address, data: Data) {
    raw[mapMirrors(addr )] = data
  }

  // See http://wiki.nesdev.com/w/index.php/PPU_palettes#Memory_Map
  private fun mapMirrors(addr: Address) = if ((addr and 0x03) == 0) (addr and 0xEF) else addr
}
