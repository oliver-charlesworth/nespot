package choliver.nespot.mappers

import choliver.nespot.cartridge.Cartridge
import choliver.nespot.cartridge.Mapper
import choliver.nespot.cartridge.Rom
import choliver.nespot.cartridge.Rom.Mirroring.FIXED_LOWER
import choliver.nespot.cartridge.Rom.Mirroring.FIXED_UPPER
import choliver.nespot.common.Address
import choliver.nespot.common.Data

// See https://wiki.nesdev.com/w/index.php/AxROM
class AxRomMapper(rom: Rom) : Mapper {
  private val numPrgBanks = (rom.prgData.size / PRG_BANK_SIZE)

  override val prgData = rom.prgData
  override val chrData = ByteArray(CHR_RAM_SIZE)
  override val prgBankSize = PRG_BANK_SIZE

  override fun Cartridge.onPrgSet(addr: Address, data: Data) {
    chr.mirroring = when ((data and 0x10) shr 4) {
      0 -> FIXED_LOWER
      else -> FIXED_UPPER
    }
    prg.bankMap[0] = (data and 0x07) % numPrgBanks
  }

  companion object {
    const val CHR_RAM_SIZE = 8192
    const val PRG_BANK_SIZE = 32768
    const val BASE_BANK_SELECT = 0x8000
  }
}
