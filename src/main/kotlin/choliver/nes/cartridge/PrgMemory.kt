package choliver.nes.cartridge

import choliver.nes.Address
import choliver.nes.Data

interface PrgMemory {
  fun load(addr: Address): Data?  // null if unmapped
  fun store(addr: Address, data: Data) {}
}
