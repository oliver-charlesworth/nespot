package choliver.nespot.ppu

import choliver.nespot.Address
import choliver.nespot.Data
import choliver.nespot.Memory

class Palette : Memory {
  private val raw = IntArray(32) { 0x00 } // Not bytes, to avoid conversion overhead

  override fun get(addr: Address): Data = raw[addr]

  // Load is the common path, so duplicate stores for mirrors.
  // See http://wiki.nesdev.com/w/index.php/PPU_palettes#Memory_Map for mapping.
  override fun set(addr: Address, data: Data) {
    val d = data and 0x3F   // Bubble Bobble relies on this
    raw[addr] = d
    if ((addr and 0x03) == 0) {
      raw[addr xor 0x10] = d
    }
  }
}
