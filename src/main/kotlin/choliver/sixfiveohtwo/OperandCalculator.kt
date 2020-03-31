package choliver.sixfiveohtwo

import choliver.sixfiveohtwo.AddressMode.*
import choliver.sixfiveohtwo.AddressMode.IndexSource.*

// TODO - we should model clock cycles accurately - implement as ucode sequencing in CPU itself
// TODO - semantics of these are weird for JMP and JSR
class OperandCalculator(
  private val memory: Memory
) {
  fun calculate(
    mode: AddressMode,
    state: State
  ): UInt16 = when (mode) {
    is Accumulator -> state.A.toUInt16()
    is Implied -> 0.toUInt16()
    is Immediate -> mode.literal.toUInt16()
    is Relative -> (state.PC + mode.offset.toUInt16()).toUInt16()
    is Absolute -> memory.load(mode.address).toUInt16()
    is ZeroPage -> memory.load(mode.address.toUInt16()).toUInt16()
    is Indirect -> TODO()
    is AbsoluteIndexed -> memory.load(
      (mode.address + select(mode.source, state)).toUInt16()
    ).toUInt16()
    is ZeroPageIndexed -> memory.load(
      ((mode.address + select(mode.source, state)) and 0xFFu).toUInt16()
    ).toUInt16()
    is IndexedIndirect -> TODO()
    is IndirectIndexed -> TODO()
  }

  private fun select(source: IndexSource, state: State) = when (source) {
    X -> state.X
    Y -> state.Y
  }
}
