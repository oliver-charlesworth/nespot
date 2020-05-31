package choliver.nespot.mappers

import choliver.nespot.cartridge.Rom

// https://wiki.nesdev.com/w/index.php/NROM
object NromMapper {
  const val CHR_RAM_SIZE = 8192
  const val PRG_BANK_SIZE = 16384

  fun create(rom: Rom) = ParameterisedMapper(
    prgData = rom.prgData,
    chrData = if (rom.chrData.isEmpty()) ByteArray(CHR_RAM_SIZE) else rom.chrData,
    prgBankSize = PRG_BANK_SIZE
  ).apply {
    chr.mirroring = rom.mirroring
    prg.bankMap[1] = if (rom.prgData.size > PRG_BANK_SIZE) 1 else 0
  }
}
