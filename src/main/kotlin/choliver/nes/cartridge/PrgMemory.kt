package choliver.nes.cartridge

import choliver.sixfiveohtwo.model.UInt16
import choliver.sixfiveohtwo.model.UInt8

interface PrgMemory {
  fun load(addr: UInt16): UInt8?  // null if unmapped
  fun store(addr: UInt16, data: UInt8) {}
}
