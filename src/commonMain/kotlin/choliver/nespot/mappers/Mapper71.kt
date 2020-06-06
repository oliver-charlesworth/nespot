package choliver.nespot.mappers

import choliver.nespot.cartridge.Cartridge
import choliver.nespot.cartridge.Mapper
import choliver.nespot.cartridge.Rom
import choliver.nespot.cartridge.Rom.Mirroring.FIXED_LOWER
import choliver.nespot.cartridge.Rom.Mirroring.FIXED_UPPER
import choliver.nespot.common.Address
import choliver.nespot.common.Data

// See https://wiki.nesdev.com/w/index.php/INES_Mapper_071
class Mapper71(private val rom: Rom) : Mapper {
  private val numPrgBanks = (rom.prgData.size / PRG_BANK_SIZE)

  override val prgData = rom.prgData
  override val chrData = ByteArray(CHR_RAM_SIZE)
  override val prgBankSize = PRG_BANK_SIZE

  override fun Cartridge.onStartup() {
    chr.mirroring = rom.mirroring
    prg.bankMap[1] = numPrgBanks - 1    // Upper bank is fixed
  }

  override fun Cartridge.onPrgSet(addr: Address, data: Data) {
    when {
      (addr >= BASE_CIC_STUN) -> Unit
      (addr >= BASE_BANK_SELECT) -> prg.bankMap[0] = data % numPrgBanks
      (addr >= BASE_MIRRORING) -> chr.mirroring = when ((data and 0x10) shr 4) {
        0 -> FIXED_LOWER
        else -> FIXED_UPPER
      }
    }
  }

  companion object {
    const val CHR_RAM_SIZE = 8192
    const val PRG_BANK_SIZE = 16384
    // Based on the hack described here: https://wiki.nesdev.com/w/index.php/INES_Mapper_071#Mirroring_.28.248000-.249FFF.29
    const val BASE_MIRRORING = 0x9000
    const val BASE_BANK_SELECT = 0xC000
    const val BASE_CIC_STUN = 0xE000
  }
}
