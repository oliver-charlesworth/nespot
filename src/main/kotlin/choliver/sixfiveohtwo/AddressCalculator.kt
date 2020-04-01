package choliver.sixfiveohtwo

import choliver.sixfiveohtwo.AddressMode.*
import choliver.sixfiveohtwo.AddressMode.IndexSource.X
import choliver.sixfiveohtwo.AddressMode.IndexSource.Y

// TODO - we should model clock cycles accurately - implement as ucode sequencing in CPU itself
// TODO - semantics of these are weird for JMP and JSR
class AddressCalculator(
  private val memory: Memory
) {
  fun calculate(
    mode: AddressMode,
    state: State
  ): UInt16 = when (mode) {
    is Relative -> (state.PC + mode.offset.toUInt16()).toUInt16()
    is Absolute -> mode.address
    is ZeroPage -> mode.address.toUInt16()
    is Indirect -> mode.address
    is AbsoluteIndexed -> (mode.address + select(mode.source, state)).toUInt16()
    is ZeroPageIndexed -> (mode.address + select(mode.source, state)).toUInt8().toUInt16()
    is IndexedIndirect -> {
      val tmp = (mode.address + state.X)
      combine(
        low = memory.load(tmp.toUInt8().toUInt16()),
        high = memory.load((tmp + 1u).toUInt8().toUInt16())
      )
    }
    is IndirectIndexed -> (combine(
        low = memory.load(mode.address.toUInt16()),
        high = memory.load((mode.address + 1u).toUInt8().toUInt16())
      ) + state.Y).toUInt16()
    else -> 0.toUInt16()  // Don't care
  }

  private fun combine(low: UInt8, high: UInt8): UInt16 = (low.toUInt16() or (high * 256u).toUInt16())

  private fun select(source: IndexSource, state: State) = when (source) {
    X -> state.X
    Y -> state.Y
  }
}
