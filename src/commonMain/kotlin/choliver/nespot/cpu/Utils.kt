package choliver.nespot.cpu

import choliver.nespot.common.Address

@Suppress("ObjectPropertyName")
const val _0 = false
@Suppress("ObjectPropertyName")
const val _1 = true

fun samePage(a: Address, b: Address) = ((a xor b) and 0xFF00) == 0
