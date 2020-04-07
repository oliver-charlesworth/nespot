package choliver.sixfiveohtwo

import choliver.sixfiveohtwo.model.*
import choliver.sixfiveohtwo.model.Operand.*
import choliver.sixfiveohtwo.model.Operand.IndexSource.X
import choliver.sixfiveohtwo.model.Operand.IndexSource.Y

// TODO - we should model clock cycles accurately - implement as ucode sequencing in CPU itself
// TODO - semantics of these are weird for JMP and JSR
class AddressCalculator(
  private val memory: Memory
) {

  fun calculate(
    operand: Operand,
    state: State
  ): UInt16 = when (operand) {
    is Relative -> (state.PC + operand.offset.u16()).u16()
    is Absolute -> operand.address
    is ZeroPage -> operand.address.u16()
    is Indirect -> combine(
      lo = memory.load(operand.address),
      hi = memory.load((operand.address + 1u).u16())
    )
    is AbsoluteIndexed -> (operand.address + select(operand.source, state)).u16()
    is ZeroPageIndexed -> (operand.address + select(operand.source, state)).lo().u16()
    is IndexedIndirect -> {
      val tmp = (operand.address + state.X)
      combine(
        lo = memory.load(tmp.lo().u16()),
        hi = memory.load((tmp + 1u).lo().u16())
      )
    }
    is IndirectIndexed -> (combine(
      lo = memory.load(operand.address.u16()),
      hi = memory.load((operand.address + 1u).lo().u16())
    ) + state.Y).u16()
    else -> 0.u16()  // Don't care
  }

  private fun select(source: IndexSource, state: State) = when (source) {
    X -> state.X
    Y -> state.Y
  }
}
