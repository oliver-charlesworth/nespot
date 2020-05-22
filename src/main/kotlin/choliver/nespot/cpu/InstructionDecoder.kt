package choliver.nespot.cpu

import choliver.nespot.*
import choliver.nespot.cpu.model.AddressMode
import choliver.nespot.cpu.model.AddressMode.*
import choliver.nespot.cpu.model.Instruction
import choliver.nespot.cpu.model.Opcode
import choliver.nespot.cpu.model.Opcode.BRK
import choliver.nespot.cpu.model.Opcode.NOP
import choliver.nespot.cpu.model.Operand.*
import choliver.nespot.cpu.model.Operand.IndexSource.X
import choliver.nespot.cpu.model.Operand.IndexSource.Y
import choliver.nespot.cpu.utils.samePage

class InstructionDecoder(private val memory: Memory) {
  @MutableForPerfReasons
  data class Decoded(
    var opcode: Opcode = NOP,
    var addressMode: AddressMode = IMPLIED,
    var addr: Address = 0x0000,
    var nextPc: Address = 0x0000,
    var numCycles: Int = 0
  )

  data class DecodedInstruction(
    val instruction: Instruction,
    val nextPc: Address,
    val numCycles: Int
  )

  fun decode(decoded: Decoded, pc: Address, x: Data, y: Data) {
    val opcode = memory[pc]
    val found = ENCODINGS[opcode] ?: error("Unexpected opcode 0x%02x at 0x%04x".format(opcode, pc))

    val addr: Address
    val pcInc: Int
    var pageCrossing = false

    when (found.addressMode) {
      IMPLIED -> {
        addr = 0
        pcInc = 1
      }
      ACCUMULATOR -> {
        addr = 0
        pcInc = 1
      }
      IMMEDIATE -> {
        addr = memory[pc + 1]
        pcInc = 2
      }
      INDIRECT -> {
        addr = load16(addr(lo = memory[pc + 1], hi = memory[pc + 2]))
        pcInc = 3
      }
      RELATIVE -> {
        addr = (pc + 2) + memory[pc + 1].sext()
        pcInc = 2
      }
      ABSOLUTE -> {
        addr = addr(lo = memory[pc + 1], hi = memory[pc + 2])
        pcInc = 3
      }
      ABSOLUTE_X -> {
        val base = addr(lo = memory[pc + 1], hi = memory[pc + 2])
        addr = base + x
        pcInc = 3
        pageCrossing = !samePage(addr, base)
      }
      ABSOLUTE_Y -> {
        val base = addr(lo = memory[pc + 1], hi = memory[pc + 2])
        addr = base + y
        pcInc = 3
        pageCrossing = !samePage(addr, base)
      }
      ZERO_PAGE -> {
        addr = memory[pc + 1]
        pcInc = 2
      }
      ZERO_PAGE_X -> {
        addr = (memory[pc + 1] + x).addr8()
        pcInc = 2
      }
      ZERO_PAGE_Y -> {
        addr = (memory[pc + 1] + y).addr8()
        pcInc = 2
      }
      INDEXED_INDIRECT -> {
        addr = load16FromZeroPage((memory[pc + 1] + x).addr8())
        pcInc = 2
      }
      INDIRECT_INDEXED -> {
        val base = load16FromZeroPage(memory[pc + 1])
        addr = base + y
        pcInc = 2
        pageCrossing = !samePage(addr, base)
      }
    }

    decoded.opcode = found.op
    decoded.addressMode = found.addressMode
    decoded.addr = addr.addr()
    decoded.nextPc = pc + pcInc + (if (found.op == BRK) 1 else 0)  // Special case
    decoded.numCycles = found.enc.numCycles + (if (found.enc.extraCycleForPageCrossing && pageCrossing) 1 else 0)
  }

  /** Basically a debug mode. */
  fun decodeInstruction(pc: Address): DecodedInstruction {
    val decoded = Decoded()
    decode(decoded, pc, 0, 0)

    fun operand8() = memory[pc + 1]
    fun operand16() = addr(lo = memory[pc + 1], hi = memory[pc + 2])

    val operand = when (decoded.addressMode) {
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

    return DecodedInstruction(
      instruction = Instruction(decoded.opcode, operand),
      nextPc = decoded.nextPc,
      numCycles = decoded.numCycles
    )
  }

  private fun load16(addr: Address) = addr(
    lo = memory[addr.addr()],
    hi = memory[(addr + 1).addr()]
  )

  private fun load16FromZeroPage(addr: Address8) = addr(
    lo = memory[addr],
    hi = memory[(addr + 1).addr8()]
  )

  private data class OpAndMode(
    val op: Opcode,
    val addressMode: AddressMode,
    val enc: EncodingInfo
  )

  companion object {
    private val ENCODINGS = createEncodingTable()

    private fun createEncodingTable(): List<OpAndMode?> {
      val map = OPCODES_TO_ENCODINGS
        .flatMap { (op, modes) ->
          modes.map { (mode, enc) -> enc.encoding to OpAndMode(op, mode, enc) }
        }
        .toMap()

      return (0x00..0xFF).map { map[it] }
    }
  }
}
