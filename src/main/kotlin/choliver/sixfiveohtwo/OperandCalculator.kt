package choliver.sixfiveohtwo

// TODO - we should model clock cycles accurately - implement as ucode sequencing in CPU itself
// TODO - semantics of these are weird for JMP and JSR
class OperandCalculator(
  private val memory: Memory
) {
  fun calculate(
    addressMode: AddressMode,
    state: State
  ): UInt16 = when (addressMode) {
    is AddressMode.Accumulator -> state.A.toUInt16()
    is AddressMode.Implied -> 0.toUInt16()
    is AddressMode.Immediate -> addressMode.literal.toUInt16()
    is AddressMode.Relative -> (state.PC + addressMode.offset.toUInt16()).toUInt16()
    is AddressMode.Absolute -> memory.load(addressMode.address).toUInt16()
    is AddressMode.ZeroPage -> memory.load(addressMode.address.toUInt16()).toUInt16()
    is AddressMode.Indirect -> TODO()
    is AddressMode.AbsoluteIndexed -> TODO()
    is AddressMode.ZeroPageIndexed -> TODO()
    is AddressMode.IndexedIndirect -> TODO()
    is AddressMode.IndirectIndexed -> TODO()
  }
}
