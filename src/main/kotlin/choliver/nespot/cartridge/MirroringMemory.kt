package choliver.nespot.cartridge

import choliver.nespot.Address
import choliver.nespot.Data
import choliver.nespot.Memory
import choliver.nespot.cartridge.Rom.Mirroring
import choliver.nespot.cartridge.Rom.Mirroring.*

internal class MirroringMemory(
  private val mirroring: Mirroring,
  private val ram: Memory
) : Memory {
  override fun get(addr: Address) = ram[mapToVram(addr)]

  override fun set(addr: Address, data: Data) {
    ram[mapToVram(addr)] = data
  }

  private fun mapToVram(addr: Address) = when (mirroring) {
    HORIZONTAL -> mirrorHorizontal(addr)
    VERTICAL -> mirrorVertical(addr)
    IGNORED -> throw UnsupportedOperationException()
  }
}

internal fun mirrorHorizontal(addr: Address): Address = (addr and 1023) or ((addr and 2048) shr 1)
internal fun mirrorVertical(addr: Address): Address = addr and 2047
