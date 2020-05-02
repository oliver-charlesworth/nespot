package choliver.nespot.sixfiveohtwo

import choliver.nespot.Address
import choliver.nespot.Data
import choliver.nespot.Memory
import choliver.nespot.addr
import choliver.nespot.sixfiveohtwo.model.AddressMode
import choliver.nespot.sixfiveohtwo.model.AddressMode.*
import choliver.nespot.sixfiveohtwo.model.Instruction
import choliver.nespot.sixfiveohtwo.model.Opcode
import choliver.nespot.sixfiveohtwo.model.Opcode.BRK
import choliver.nespot.sixfiveohtwo.model.Operand.*
import choliver.nespot.sixfiveohtwo.model.Operand.IndexSource.X
import choliver.nespot.sixfiveohtwo.model.Operand.IndexSource.Y

class InstructionDecoder(private val memory: Memory) {
  data class Decoded(
    val instruction: Instruction,
    val addr: Address,
    val nextPc: Address,
    val numCycles: Int
  )

  private val addrCalc = AddressCalculator(memory)

  fun decode(pc: Address, x: Data, y: Data): Decoded {
    var nextPc = pc
    fun load() = memory[nextPc++]

    val opcode = load()
    val found = ENCODINGS[opcode] ?: error("Unexpected opcode 0x%02x at 0x%04x".format(opcode, pc))

    fun operand8() = load()
    fun operand16() = addr(lo = load(), hi = load())

    val operand = when (found.addressMode) {
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

    if (found.op == BRK) {
      load()  // Special case
    }

    return Decoded(
      instruction = Instruction(found.op, operand),
      addr = addrCalc.calculate(operand, pc = nextPc, x = x, y = y),
      nextPc = nextPc,
      numCycles = found.numCycles
    )
  }

  private data class OpAndMode(
    val op: Opcode,
    val addressMode: AddressMode,
    val numCycles: Int
  )

  companion object {
    private val ENCODINGS = createEncodingTable()

    private fun createEncodingTable(): List<OpAndMode?> {
      val map = OPCODES_TO_ENCODINGS
        .flatMap { (op, modes) ->
          modes.map { (mode, enc) -> enc.encoding to OpAndMode(op, mode, enc.numCycles) }
        }
        .toMap()

      return (0x00..0xFF).map { map[it] }
    }
  }
}
