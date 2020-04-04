package choliver.sixfiveohtwo



sealed class AddressMode {
  enum class IndexSource { X, Y }

  object Accumulator : AddressMode()

  object Implied : AddressMode()

  object Stack : AddressMode()  // TODO - internal-only

  data class Immediate(
    val literal: UInt8
  ) : AddressMode()

  data class Relative(
    val offset: Int8    // Signed
  ) : AddressMode()

  data class Absolute(
    val address: UInt16
  ) : AddressMode()

  data class ZeroPage(
    val address: UInt8
  ) : AddressMode()

  data class Indirect(
    val address: UInt16
  ) : AddressMode()

  data class AbsoluteIndexed(
    val address: UInt16,
    val source: IndexSource
  ) : AddressMode()

  data class ZeroPageIndexed(
    val address: UInt8,
    val source: IndexSource
  ) : AddressMode()

  // Always uses X as the source
  data class IndexedIndirect(
    val address: UInt8
  ) : AddressMode()

  // Always uses Y as the source
  data class IndirectIndexed(
    val address: UInt8
  ) : AddressMode()
}


