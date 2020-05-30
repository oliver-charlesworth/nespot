package choliver.nespot.cartridge

import choliver.nespot.Address
import choliver.nespot.cartridge.Rom.Mirroring
import choliver.nespot.cartridge.Rom.Mirroring.*

internal fun vramAddr(mirroring: Mirroring, addr: Address): Address = when (mirroring) {
  VERTICAL -> mirrorVertical(addr)
  HORIZONTAL -> mirrorHorizontal(addr)
  IGNORED -> throw UnsupportedOperationException()
}

internal fun mirrorHorizontal(addr: Address): Address = (addr and 1023) or ((addr and 2048) shr 1)
internal fun mirrorVertical(addr: Address): Address = addr and 2047
