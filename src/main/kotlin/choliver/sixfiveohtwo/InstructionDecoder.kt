package choliver.sixfiveohtwo

import choliver.sixfiveohtwo.AddrMode.*
import choliver.sixfiveohtwo.AddressMode.*

class InstructionDecoder {
  data class Decoded(
    val op: Opcode,
    val addrMode: AddressMode,
    val length: UInt8
  )

  fun decode(encoding: Array<UInt8>): Decoded {
    var length = 1

    // TODO - error handling
    val found = ENCODINGS[encoding[0]]!!

    fun operand8(): UInt8 {
      length = 2
      return encoding[1]
    }
    fun operand16(): UInt16 {
      length = 3
      return combine(encoding[1], encoding[2])
    }

    val mode = when (found.addrMode) {
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
      length.u8()
    )
  }

  private data class OpAndMode(
    val op: Opcode,
    val addrMode: AddrMode = IMPLIED
  )

  companion object {
    private val ENCODINGS = Opcode.values()
      .flatMap { it.encodings.entries.map { (mode, enc) -> enc to OpAndMode(it, mode) } }
      .toMap()
  }
}
