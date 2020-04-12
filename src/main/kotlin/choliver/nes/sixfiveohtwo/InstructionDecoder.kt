package choliver.nes.sixfiveohtwo

import choliver.nes.Memory
import choliver.nes.addr
import choliver.nes.sixfiveohtwo.model.AddressMode
import choliver.nes.sixfiveohtwo.model.AddressMode.*
import choliver.nes.sixfiveohtwo.model.Instruction
import choliver.nes.sixfiveohtwo.model.Opcode
import choliver.nes.sixfiveohtwo.model.Operand.*
import choliver.nes.sixfiveohtwo.model.Operand.IndexSource.X
import choliver.nes.sixfiveohtwo.model.Operand.IndexSource.Y
import choliver.nes.sixfiveohtwo.model.ProgramCounter

class InstructionDecoder {
  data class Decoded(
    val instruction: Instruction,
    val pc: ProgramCounter
  )

  fun decode(memory: Memory, pc: ProgramCounter): Decoded {
    var pcLocal = pc
    fun load() = memory.load((pcLocal++).addr())

    // TODO - error handling
    val opcode = load()
    val found = ENCODINGS[opcode] ?: error("Unexpected opcode 0x%02x at %s".format(opcode, pc))

    fun operand8() = load()
    fun operand16() = addr(lo = load(), hi = load())

    val mode = when (found.addressMode) {
      ACCUMULATOR -> Accumulator
      IMMEDIATE -> Immediate(operand8())
      IMPLIED -> Implied
      INDIRECT -> Indirect(operand16())
      RELATIVE -> Relative(operand8())
      ABSOLUTE -> Absolute(operand16())
      ABSOLUTE_X -> AbsoluteIndexed(operand16(), X)
      ABSOLUTE_Y -> AbsoluteIndexed(operand16(), Y)
      ZERO_PAGE -> ZeroPage(operand8())
      ZERO_PAGE_X -> ZeroPageIndexed(operand8(), X)
      ZERO_PAGE_Y -> ZeroPageIndexed(operand8(), Y)
      INDEXED_INDIRECT -> IndexedIndirect(operand8())
      INDIRECT_INDEXED -> IndirectIndexed(operand8())
    }

    return Decoded(Instruction(found.op, mode), pcLocal)
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
