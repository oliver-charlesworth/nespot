package choliver.nespot.sixfiveohtwo

import choliver.nespot.*
import choliver.nespot.sixfiveohtwo.model.AddressMode
import choliver.nespot.sixfiveohtwo.model.AddressMode.*
import choliver.nespot.sixfiveohtwo.model.Opcode
import choliver.nespot.sixfiveohtwo.model.Opcode.BRK

class InstructionDecoder(private val memory: Memory) {
  data class Decoded(
    val opcode: Opcode,
    val addressMode: AddressMode,
    val addr: Address,
    val nextPc: Address,
    val numCycles: Int
  )

  fun decode(pc: Address, x: Data, y: Data): Decoded {
    val opcode = memory[pc]
    val found = ENCODINGS[opcode] ?: error("Unexpected opcode 0x%02x at 0x%04x".format(opcode, pc))

    val addr: Address
    val pcInc: Int

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
        addr = memory[pc + 1] // TODO - kind of a cheat
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
        addr = addr(lo = memory[pc + 1], hi = memory[pc + 2]) + x
        pcInc = 3
      }
      ABSOLUTE_Y -> {
        addr = addr(lo = memory[pc + 1], hi = memory[pc + 2]) + y
        pcInc = 3
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
        addr = load16FromZeroPage(memory[pc + 1]) + y
        pcInc = 2
      }
    }

    return Decoded(
      opcode = found.op,
      addressMode = found.addressMode,
      addr = addr.addr(),
      nextPc = pc + pcInc + (if (found.op == BRK) 1 else 0),  // Special case
      numCycles = found.numCycles
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
