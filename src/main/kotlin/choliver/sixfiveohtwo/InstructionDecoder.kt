package choliver.sixfiveohtwo

import choliver.sixfiveohtwo.model.*
import choliver.sixfiveohtwo.model.AddressMode.*
import choliver.sixfiveohtwo.model.Operand.*

class InstructionDecoder {
  data class Decoded(
    val op: Opcode,
    val operand: Operand,
    val pc: ProgramCounter
  )

  fun decode(memory: Memory, pc: ProgramCounter): Decoded {
    var pcLocal = pc
    fun load() = memory.load((pcLocal++).u16())

    // TODO - error handling
    val found = ENCODINGS[load()]!!

    fun operand8() = load()
    fun operand16() = combine(lo = load(), hi = load())

    val mode = when (found.addressMode) {
      ACCUMULATOR -> Accumulator
      IMMEDIATE -> Immediate(operand8())
      IMPLIED -> Implied
      INDIRECT -> Indirect(operand16())
      RELATIVE -> Relative(operand8().s8())
      ABSOLUTE -> Absolute(operand16())
      ABSOLUTE_X -> AbsoluteIndexed(operand16(), IndexSource.X)
      ABSOLUTE_Y -> AbsoluteIndexed(operand16(), IndexSource.Y)
      ZERO_PAGE -> ZeroPage(operand8())
      ZERO_PAGE_X -> ZeroPageIndexed(operand8(), IndexSource.X)
      ZERO_PAGE_Y -> ZeroPageIndexed(operand8(), IndexSource.Y)
      INDEXED_INDIRECT -> IndexedIndirect(operand8())
      INDIRECT_INDEXED -> IndirectIndexed(operand8())
    }

    return Decoded(
      found.op,
      mode,
      pcLocal
    )
  }

  private data class OpAndMode(
    val op: Opcode,
    val addressMode: AddressMode = IMPLIED
  )

  companion object {
    private val ENCODINGS = OPCODES_TO_ENCODINGS
      .flatMap { (op, modes) -> modes.map { (mode, enc) -> enc to OpAndMode(op, mode) } }
      .toMap()
  }
}
