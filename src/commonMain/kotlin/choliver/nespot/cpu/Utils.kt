package choliver.nespot.cpu

import choliver.nespot.common.Address

fun samePage(a: Address, b: Address) = ((a xor b) and 0xFF00) == 0
