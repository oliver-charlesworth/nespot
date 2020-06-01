package choliver.nespot.cartridge

import choliver.nespot.mappers.*

// See https://wiki.nesdev.com/w/index.php/Mapper#iNES_1.0_mapper_grid
fun createMapper(
  rom: Rom,
  getStepCount: () -> Int = { 0 }   // Some mappers need access to this to identify consecutive load/stores
): Mapper {
  validateMagicNumber(rom)
  return when (rom.mapper) {
    0 -> StandardMapper(NromMapperConfig(rom))
    1 -> Mmc1Mapper(rom, getStepCount)
    2 -> StandardMapper(UxRomMapperConfig(rom))
    3 -> StandardMapper(CnRomMapperConfig(rom))
    4 -> StandardMapper(Mmc3MapperConfig(rom))
    71 -> Mapper71(rom)
    else -> throw RuntimeException("Mapper #${rom.mapper}")
  }
}

private fun validateMagicNumber(rom: Rom) {
  if (rom.magic.toList() != listOf('N'.toByte(), 'E'.toByte(), 'S'.toByte(), 0x1A.toByte())) {
    throw IllegalArgumentException("Invalid magic number ${rom.magic.toList()}")
  }
}
