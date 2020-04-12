package choliver.nes.sixfiveohtwo

import choliver.nes.*
import choliver.nes.sixfiveohtwo.model.Operand
import choliver.nes.sixfiveohtwo.model.Operand.*
import choliver.nes.sixfiveohtwo.model.Operand.IndexSource.X
import choliver.nes.sixfiveohtwo.model.Operand.IndexSource.Y
import choliver.nes.sixfiveohtwo.model.State

class AddressCalculator(
  private val memory: Memory
) {
  fun calculate(
    operand: Operand,
    state: State
  ): Address = when (operand) {
    is Relative -> state.PC.addr() + operand.offset.sext()
    is Absolute -> operand.addr
    is AbsoluteIndexed -> operand.addr + select(operand.source, state)
    is ZeroPage -> operand.addr
    is ZeroPageIndexed -> (operand.addr + select(operand.source, state)).addr8()
    is Indirect -> load16(operand.addr)
    is IndexedIndirect -> load16FromZeroPage((operand.addr + state.X).addr8())
    is IndirectIndexed -> load16FromZeroPage(operand.addr) + state.Y
    else -> 0
  }.addr()

  private fun load16(addr: Address) = addr(
    lo = memory.load(addr.addr()),
    hi = memory.load((addr + 1).addr())
  )

  private fun load16FromZeroPage(addr: Address8) = addr(
    lo = memory.load(addr),
    hi = memory.load((addr + 1).addr8())
  )

  private fun select(source: IndexSource, state: State) = when (source) {
    X -> state.X
    Y -> state.Y
  }
}
