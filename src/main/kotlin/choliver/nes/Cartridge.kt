package choliver.nes

import choliver.nes.Cartridge.Mirroring.*

// https://wiki.nesdev.com/w/index.php/INES
class Cartridge(romData: ByteArray) {
  enum class Mirroring {
    HORIZONTAL,
    VERTICAL,
    IGNORED   // TODO - not sure if this is mutually exclusive
  }

  interface Mapper {
    val prg: PrgMemory
    val chr: ChrMemory
  }

  @Suppress("ArrayInDataClass")
  data class Stuff(
    val hasPersistentMem: Boolean,
    val mirroring: Mirroring,
    val trainerData: ByteArray,
    val prgData: ByteArray,
    val chrData: ByteArray
  )

  private val mapper = createMapper(romData)
  val prg = mapper.prg  // Nothing special - just log bus conflicts and all-null
  val chr = mapper.chr  // Return data, VRAM addr or null

  init {
    val magicNumber = romData.copyOfRange(0, 4).toList()
    if (magicNumber != listOf('N'.toByte(), 'E'.toByte(), 'S'.toByte(), 0x1A.toByte())) {
      throw IllegalArgumentException("Invalid magic number ${magicNumber}")
    }
  }

  private fun createMapper(romData: ByteArray): Mapper {
    val sizePrgRom = romData[4] * 16384
    val sizeChrRom = romData[5] * 8192
    val sizeTrainer = if (romData[6].isBitSet(2)) 512 else 0

    val stuff = Stuff(
      hasPersistentMem = romData[6].isBitSet(1),
      mirroring = when {
        romData[6].isBitSet(3) -> IGNORED
        romData[6].isBitSet(0) -> VERTICAL
        else -> HORIZONTAL
      },
      trainerData = romData.copyOfRange(16, 16 + sizeTrainer),
      prgData = romData.copyOfRange(16 + sizeTrainer, 16 + sizeTrainer + sizePrgRom),
      chrData = romData.copyOfRange(16 + sizeTrainer + sizePrgRom, 16 + sizeTrainer + sizePrgRom + sizeChrRom)
    )

    // https://wiki.nesdev.com/w/index.php/Mapper#iNES_1.0_mapper_grid
    return when (val mapper = ((romData[6].toInt() and 0xF0) shr 4) or ((romData[7].toInt() and 0xF0))) {
      0 -> NromMapper(stuff)
      else -> throw UnsupportedRomException("Mapper #${mapper}")
    }
  }

  private fun Byte.isBitSet(i: Int) = (toInt() and (1 shl i)) != 0

  class UnsupportedRomException(message: String) : RuntimeException(message)
}
