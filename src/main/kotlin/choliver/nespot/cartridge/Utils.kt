package choliver.nespot.cartridge

import choliver.nespot.Address


internal fun mirrorHorizontal(addr: Address): Address = (addr and 1023) or ((addr and 2048) shr 1)
internal fun mirrorVertical(addr: Address): Address = addr and 2047
