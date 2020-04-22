package choliver.nespot.ppu

import choliver.nespot.Address
import choliver.nespot.Data
import choliver.nespot.Memory

class Palette : Memory {
  private val raw = IntArray(32) { 0x00 } // Not bytes, to avoid conversion overhead

  override fun load(addr: Address): Data = raw[addr]

  // Load is the common path, so duplicate stores for mirrors.
  // See http://wiki.nesdev.com/w/index.php/PPU_palettes#Memory_Map for mapping.
  override fun store(addr: Address, data: Data) {
    raw[addr] = data
    if ((addr and 0x03) == 0) {
      raw[addr xor 0x10] = data
    }
  }
}
