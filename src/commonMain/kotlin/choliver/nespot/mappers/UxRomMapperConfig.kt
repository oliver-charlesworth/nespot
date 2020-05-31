package choliver.nespot.mappers

import choliver.nespot.Address
import choliver.nespot.BASE_PRG_ROM
import choliver.nespot.Data
import choliver.nespot.cartridge.Rom
import choliver.nespot.cartridge.StandardMapper
import choliver.nespot.cartridge.StandardMapper.Config

class UxRomMapperConfig(private val rom: Rom) : Config {
  private val numPrgBanks = (rom.prgData.size / PRG_BANK_SIZE)

  override val prgData = rom.prgData
  override val chrData = ByteArray(CHR_RAM_SIZE)
  override val prgBankSize = PRG_BANK_SIZE

  override fun StandardMapper.onStartup() {
    chr.mirroring = rom.mirroring
    prg.bankMap[1] = numPrgBanks - 1    // Upper bank is fixed
  }

  override fun StandardMapper.onPrgSet(addr: Address, data: Data) {
    prg.bankMap[0] = data % numPrgBanks
  }

  companion object {
    const val CHR_RAM_SIZE = 8192
    const val PRG_BANK_SIZE = 16384
    const val BASE_BANK_SELECT = BASE_PRG_ROM
  }
}
