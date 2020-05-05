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
    HORIZONTAL -> ::mirrorHorizontal
    VERTICAL -> ::mirrorVertical
    IGNORED -> throw UnsupportedOperationException()
  }

  override fun get(addr: Address) = ram[mapToVram(addr)]

  override fun set(addr: Address, data: Data) {
    ram[mapToVram(addr)] = data
  }
}

internal fun mirrorHorizontal(addr: Address): Address = (addr and 1023) or ((addr and 2048) shr 1)
internal fun mirrorVertical(addr: Address): Address = addr and 2047
