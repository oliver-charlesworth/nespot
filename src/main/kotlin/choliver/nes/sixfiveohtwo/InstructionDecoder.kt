package choliver.nes.sixfiveohtwo

import choliver.nes.Address
import choliver.nes.Memory
import choliver.nes.addr
import choliver.nes.sixfiveohtwo.model.*
import choliver.nes.sixfiveohtwo.model.AddressMode.*
import choliver.nes.sixfiveohtwo.model.Opcode.BRK
import choliver.nes.sixfiveohtwo.model.Operand.*
import choliver.nes.sixfiveohtwo.model.Operand.IndexSource.X
import choliver.nes.sixfiveohtwo.model.Operand.IndexSource.Y

class InstructionDecoder(private val memory: Memory) {
  data class Decoded(
    val instruction: Instruction,
    val addr: Address,
    val nextPc: ProgramCounter,
    val numCycles: Int
  )

  private val addrCalc = AddressCalculator(memory)

  fun decode(state: State, pc: ProgramCounter): Decoded {
    var nextPc = pc
    fun load() = memory.load((nextPc++).addr())

    // TODO - error handling
    val opcode = load()
    val found = ENCODINGS[opcode] ?: error("Unexpected opcode 0x%02x at %s".format(opcode, nextPc))

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
      addr = addrCalc.calculate(operand, state.with(PC = nextPc)),
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
    private val ENCODINGS = OPCODES_TO_ENCODINGS
      .flatMap { (op, modes) ->
        modes.map { (mode, enc) -> enc.encoding to OpAndMode(op, mode, enc.numCycles) }
      }
      .toMap()
  }
}
