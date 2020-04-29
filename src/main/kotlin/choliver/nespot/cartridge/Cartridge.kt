package choliver.nespot.cartridge

import choliver.nespot.Memory
import choliver.nespot.cartridge.MapperConfig.Mirroring.*
import choliver.nespot.isBitSet

// https://wiki.nesdev.com/w/index.php/INES
class Cartridge(romData: ByteArray) {

  private val mapper = createMapper(romData)
  val prg = mapper.prg
  fun chr(vram: Memory) = mapper.chr(vram)

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

    val config = MapperConfig(
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
      0 -> NromMapper(config)
      1 -> Mmc1Mapper(config)
      71 -> Mapper71(config)
      else -> throw UnsupportedRomException("Mapper #${mapper}")
    }
  }

  class UnsupportedRomException(message: String) : RuntimeException(message)
}
