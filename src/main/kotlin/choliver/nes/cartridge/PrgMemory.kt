package choliver.nes.cartridge

import choliver.nes.UInt16
import choliver.nes.UInt8

interface PrgMemory {
  fun load(addr: UInt16): UInt8?  // null if unmapped
  fun store(addr: UInt16, data: UInt8) {}
}
