package choliver.nespot.mappers

import choliver.nespot.cartridge.Cartridge
import choliver.nespot.cartridge.Mapper
import choliver.nespot.cartridge.Rom

// https://wiki.nesdev.com/w/index.php/NROM
class NromMapper(private val rom: Rom) : Mapper {
  override val prgData = rom.prgData
  override val chrData = if (rom.chrData.isEmpty()) ByteArray(CHR_RAM_SIZE) else rom.chrData
  override val prgBankSize = PRG_BANK_SIZE

  override fun Cartridge.onStartup() {
    chr.mirroring = rom.mirroring
    prg.bankMap[1] = if (rom.prgData.size > PRG_BANK_SIZE) 1 else 0
  }

  companion object {
    const val CHR_RAM_SIZE = 8192
    const val PRG_BANK_SIZE = 16384
  }
}
