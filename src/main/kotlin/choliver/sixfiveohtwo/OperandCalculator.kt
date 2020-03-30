package choliver.sixfiveohtwo

import choliver.sixfiveohtwo.AddressMode.*

// TODO - we should model clock cycles accurately - implement as ucode sequencing in CPU itself
// TODO - semantics of these are weird for JMP and JSR
class OperandCalculator(
  private val memory: Memory
) {
  fun calculate(
    addressMode: AddressMode,
    state: State
  ): UInt16 = when (addressMode) {
    is Accumulator -> state.A.toUInt16()
    is Implied -> 0.toUInt16()
    is Immediate -> addressMode.literal.toUInt16()
    is Relative -> (state.PC + addressMode.offset.toUInt16()).toUInt16()
    is Absolute -> memory.load(addressMode.address).toUInt16()
    is ZeroPage -> memory.load(addressMode.address.toUInt16()).toUInt16()
    is Indirect -> TODO()
    is AbsoluteIndexed -> TODO()
    is ZeroPageIndexed -> TODO()
    is IndexedIndirect -> TODO()
    is IndirectIndexed -> TODO()
  }
}
