package choliver.nespot.cartridge

import choliver.nespot.Address
import choliver.nespot.Data
import choliver.nespot.Memory
import choliver.nespot.cartridge.Rom.Mirroring
import choliver.nespot.cartridge.Rom.Mirroring.*

class MirroringMemory(
  mirroring: Mirroring,
  private val ram: Memory
) : Memory {
  private val mapToVram: (Address) -> Address = when (mirroring) {
    HORIZONTAL -> { addr -> (addr and 1023) or ((addr and 2048) shr 1) }
    VERTICAL -> { addr -> addr and 2047 }
    IGNORED -> throw UnsupportedOperationException()
  }

  override fun load(addr: Address) = ram.load(mapToVram(addr))

  override fun store(addr: Address, data: Data) {
    ram.store(mapToVram(addr), data)
  }
}
