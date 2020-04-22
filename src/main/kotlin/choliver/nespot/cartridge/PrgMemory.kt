package choliver.nespot.cartridge

import choliver.nespot.Address
import choliver.nespot.Data

interface PrgMemory {
  fun load(addr: Address): Data?  // null if unmapped
  fun store(addr: Address, data: Data) {}
}
