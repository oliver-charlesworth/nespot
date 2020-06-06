package choliver.nespot.mappers

import choliver.nespot.BASE_PRG_ROM
import choliver.nespot.cartridge.Cartridge
import choliver.nespot.cartridge.Mapper
import choliver.nespot.cartridge.Rom
import choliver.nespot.common.Address
import choliver.nespot.common.Data

// See https://wiki.nesdev.com/w/index.php/CNROM
class CnRomMapper(private val rom: Rom) : Mapper {
  private val numChrBanks = (rom.chrData.size / CHR_BANK_SIZE)

  override val prgData = rom.prgData
  override val chrData = rom.chrData
  override val prgBankSize = PRG_BANK_SIZE
  override val chrBankSize = CHR_BANK_SIZE

  override fun Cartridge.onStartup() {
    chr.mirroring = rom.mirroring
    prg.bankMap[1] = if (rom.prgData.size > PRG_BANK_SIZE) 1 else 0
  }

  @Suppress("UNUSED_PARAMETER")
  override fun Cartridge.onPrgSet(addr: Address, data: Data) {
    chr.bankMap[0] = data % numChrBanks
  }

  companion object {
    const val PRG_BANK_SIZE = 16384
    const val CHR_BANK_SIZE = 8192
    const val BASE_BANK_SELECT = BASE_PRG_ROM
  }

}
