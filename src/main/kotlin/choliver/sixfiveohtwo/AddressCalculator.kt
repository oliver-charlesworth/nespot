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
    is Relative -> (state.PC + mode.offset.u16()).u16()
    is Absolute -> mode.address
    is ZeroPage -> mode.address.u16()
    is Indirect -> combine(
      lo = memory.load(mode.address),
      hi = memory.load((mode.address + 1u).u16())
    )
    is AbsoluteIndexed -> (mode.address + select(mode.source, state)).u16()
    is ZeroPageIndexed -> (mode.address + select(mode.source, state)).lo().u16()
    is IndexedIndirect -> {
      val tmp = (mode.address + state.X)
      combine(
        lo = memory.load(tmp.lo().u16()),
        hi = memory.load((tmp + 1u).lo().u16())
      )
    }
    is IndirectIndexed -> (combine(
        lo = memory.load(mode.address.u16()),
        hi = memory.load((mode.address + 1u).lo().u16())
      ) + state.Y).u16()
    else -> 0.u16()  // Don't care
  }

  private fun select(source: IndexSource, state: State) = when (source) {
    X -> state.X
    Y -> state.Y
  }
}
