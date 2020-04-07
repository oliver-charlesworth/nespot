package choliver.sixfiveohtwo.model

sealed class Operand {
  enum class IndexSource { X, Y }

  object Accumulator : Operand()

  object Implied : Operand()

  data class Immediate(
    val literal: UInt8
  ) : Operand()

  data class Relative(
    val offset: Int8    // Signed
  ) : Operand()

  data class Absolute(
    val address: UInt16
  ) : Operand()

  data class ZeroPage(
    val address: UInt8
  ) : Operand()

  data class Indirect(
    val address: UInt16
  ) : Operand()

  data class AbsoluteIndexed(
    val address: UInt16,
    val source: IndexSource
  ) : Operand()

  data class ZeroPageIndexed(
    val address: UInt8,
    val source: IndexSource
  ) : Operand()

  // Always uses X as the source
  data class IndexedIndirect(
    val address: UInt8
  ) : Operand()

  // Always uses Y as the source
  data class IndirectIndexed(
    val address: UInt8
  ) : Operand()
}


