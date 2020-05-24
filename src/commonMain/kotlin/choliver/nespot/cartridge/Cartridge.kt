package choliver.nespot.cartridge

import choliver.nespot.mappers.*

// See https://wiki.nesdev.com/w/index.php/Mapper#iNES_1.0_mapper_grid
fun createMapper(
  rom: Rom,
  getStepCount: () -> Int = { 0 }   // Some mappers need access to this to identify consecutive load/stores
): Mapper {
  validateMagicNumber(rom)
  return when (rom.mapper) {
    0 -> NromMapper(rom)
    1 -> Mmc1Mapper(rom, getStepCount)
    2 -> UxRomMapper(rom)
    3 -> CnRomMapper(rom)
    4 -> Mmc3Mapper(rom)
    71 -> UxRomMapper(rom)  // Can re-use MMC1 (see https://wiki.nesdev.com/w/index.php/INES_Mapper_071)
    else -> throw RuntimeException("Mapper #${rom.mapper}")
  }
}

private fun validateMagicNumber(rom: Rom) {
  if (rom.magic.toList() != listOf('N'.toByte(), 'E'.toByte(), 'S'.toByte(), 0x1A.toByte())) {
    throw IllegalArgumentException("Invalid magic number ${rom.magic.toList()}")
  }
}
