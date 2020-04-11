package choliver.sixfiveohtwo.model

sealed class Operand {
  enum class IndexSource { X, Y }

  object Accumulator : Operand()

  object Implied : Operand()

  data class Immediate(
    val literal: UInt8
  ) : Operand()

  data class Relative(
    val addr: Int8    // Signed
  ) : Operand()

  data class Absolute(
    val addr: UInt16
  ) : Operand()

  data class ZeroPage(
    val addr: UInt8
  ) : Operand()

  data class Indirect(
    val addr: UInt16
  ) : Operand()

  data class AbsoluteIndexed(
    val addr: UInt16,
    val source: IndexSource
  ) : Operand()

  data class ZeroPageIndexed(
    val addr: UInt8,
    val source: IndexSource
  ) : Operand()

  // Always uses X as the source
  data class IndexedIndirect(
    val addr: UInt8
  ) : Operand()

  // Always uses Y as the source
  data class IndirectIndexed(
    val addr: UInt8
  ) : Operand()
}


