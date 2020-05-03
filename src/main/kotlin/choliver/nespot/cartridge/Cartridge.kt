package choliver.nespot.cartridge

import choliver.nespot.Memory
import choliver.nespot.cartridge.mappers.Mmc1Mapper
import choliver.nespot.cartridge.mappers.NromMapper
import choliver.nespot.cartridge.mappers.UxRomMapper

// See https://wiki.nesdev.com/w/index.php/INES
class Cartridge(rom: Rom) {
  private val mapper = createMapper(rom)
  val irq get() = mapper.irq
  val prg = mapper.prg
  fun chr(vram: Memory) = mapper.chr(vram)

  init {
    if (rom.magic.toList() != listOf('N'.toByte(), 'E'.toByte(), 'S'.toByte(), 0x1A.toByte())) {
      throw IllegalArgumentException("Invalid magic number ${rom.magic.toList()}")
    }
  }

  // See https://wiki.nesdev.com/w/index.php/Mapper#iNES_1.0_mapper_grid
  private fun createMapper(rom: Rom) = when (rom.mapper) {
    0 -> NromMapper(rom)
    1 -> Mmc1Mapper(rom)
    2 -> UxRomMapper(rom)
    71 -> UxRomMapper(rom)  // See https://wiki.nesdev.com/w/index.php/INES_Mapper_071
    else -> throw RuntimeException("Mapper #${rom.mapper}")
  }
}
