package choliver.nespot.sixfiveohtwo

import choliver.nespot.*
import choliver.nespot.sixfiveohtwo.model.Operand
import choliver.nespot.sixfiveohtwo.model.Operand.*
import choliver.nespot.sixfiveohtwo.model.Operand.IndexSource.X

class AddressCalculator(
  private val memory: Memory
) {
  fun calculate(
    operand: Operand,
    pc: Address = 0,
    x: Data = 0,
    y: Data = 0
  ): Address = when (operand) {
    is Relative -> pc + operand.offset.sext()
    is Absolute -> operand.addr
    is AbsoluteIndexed -> operand.addr + (if (operand.source == X) x else y)
    is ZeroPage -> operand.addr
    is ZeroPageIndexed -> (operand.addr + (if (operand.source == X) x else y)).addr8()
    is Indirect -> load16(operand.addr)
    is IndexedIndirect -> load16FromZeroPage((operand.addr + x).addr8())
    is IndirectIndexed -> load16FromZeroPage(operand.addr) + y
    else -> 0
  }.addr()

  private fun load16(addr: Address) = addr(
    lo = memory[addr.addr()],
    hi = memory[(addr + 1).addr()]
  )

  private fun load16FromZeroPage(addr: Address8) = addr(
    lo = memory[addr],
    hi = memory[(addr + 1).addr8()]
  )
}
