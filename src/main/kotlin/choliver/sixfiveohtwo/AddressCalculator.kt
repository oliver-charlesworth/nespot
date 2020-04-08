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
    is AbsoluteIndexed -> (operand.address + select(operand.source, state)).u16()
    is ZeroPage -> operand.address.u16()
    is ZeroPageIndexed -> (operand.address + select(operand.source, state)).lo().u16()
    is Indirect -> load16(operand.address)
    is IndexedIndirect -> load16FromZeroPage((operand.address + state.X).u8())
    is IndirectIndexed -> (load16FromZeroPage(operand.address) + state.Y).u16()
    else -> 0.u16()  // Don't care
  }

  private fun load16(addr: UInt16) = combine(
    lo = memory.load(addr),
    hi = memory.load((addr + 1u).u16())
  )

  private fun load16FromZeroPage(addr: UInt8) = combine(
    lo = memory.load(addr.u16()),
    hi = memory.load((addr + 1u).lo().u16())
  )

  private fun select(source: IndexSource, state: State) = when (source) {
    X -> state.X
    Y -> state.Y
  }
}
