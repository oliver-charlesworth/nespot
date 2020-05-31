package choliver.nespot.mappers

import choliver.nespot.cartridge.Rom
import choliver.nespot.cartridge.StandardMapper
import choliver.nespot.cartridge.StandardMapper.Config

// https://wiki.nesdev.com/w/index.php/NROM
class NromMapperConfig(private val rom: Rom) : Config {
  override val prgData = rom.prgData
  override val chrData = if (rom.chrData.isEmpty()) ByteArray(CHR_RAM_SIZE) else rom.chrData
  override val prgBankSize = PRG_BANK_SIZE

  override fun StandardMapper.onStartup() {
    chr.mirroring = rom.mirroring
    prg.bankMap[1] = if (rom.prgData.size > PRG_BANK_SIZE) 1 else 0
  }

  companion object {
    const val CHR_RAM_SIZE = 8192
    const val PRG_BANK_SIZE = 16384
  }
}
