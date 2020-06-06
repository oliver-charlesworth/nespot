package choliver.nespot.cpu

import choliver.nespot.common.Address
import choliver.nespot.common.Address8
import choliver.nespot.common.Data

sealed class Operand {
  enum class IndexSource { X, Y }

  object Accumulator : Operand()

  object Implied : Operand()

  data class Immediate(
    val literal: Data
  ) : Operand()

  data class Relative(
    val offset: Data    // Signed
  ) : Operand()

  data class Absolute(
    val addr: Address
  ) : Operand()

  data class ZeroPage(
    val addr: Address8
  ) : Operand()

  data class Indirect(
    val addr: Data
  ) : Operand()

  data class AbsoluteIndexed(
    val addr: Address,
    val source: IndexSource
  ) : Operand()

  data class ZeroPageIndexed(
    val addr: Address8,
    val source: IndexSource
  ) : Operand()

  // Always uses X as the source
  data class IndexedIndirect(
    val addr: Address8
  ) : Operand()

  // Always uses Y as the source
  data class IndirectIndexed(
    val addr: Address8
  ) : Operand()
}


