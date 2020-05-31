package choliver.nespot.mappers

import choliver.nespot.Address
import choliver.nespot.BASE_PRG_ROM
import choliver.nespot.Data
import choliver.nespot.Ram
import choliver.nespot.cartridge.ChrMemory
import choliver.nespot.cartridge.Mapper
import choliver.nespot.cartridge.PrgMemory
import choliver.nespot.cartridge.Rom
import choliver.nespot.cartridge.Rom.Mirroring.FIXED_LOWER
import choliver.nespot.cartridge.Rom.Mirroring.FIXED_UPPER

// See https://wiki.nesdev.com/w/index.php/INES_Mapper_071
class Mapper71(rom: Rom) : Mapper {
  private val numPrgBanks = (rom.prgData.size / PRG_BANK_SIZE)
  override val irq = false
  override val persistentRam: Ram? = null

  override val prg = PrgMemory(
    raw = rom.prgData,
    bankSize = PRG_BANK_SIZE,
    onSet = ::updateReg
  )

  override val chr = ChrMemory(
    raw = ByteArray(CHR_RAM_SIZE)
  )

  init {
    chr.mirroring = rom.mirroring
    prg.bankMap[1] = numPrgBanks - 1    // Upper bank is fixed
  }

  private fun updateReg(addr: Address, data: Data) {
    when {
      (addr >= BASE_BANK_SELECT) -> prg.bankMap[0] = data % numPrgBanks
      (addr >= BASE_MIRRORING) -> chr.mirroring = when ((data and 0x10) shr 4) {
        0 -> FIXED_LOWER
        else -> FIXED_UPPER
      }
    }
  }

  @Suppress("unused")
  companion object {
    const val CHR_RAM_SIZE = 8192
    const val PRG_BANK_SIZE = 16384
    const val BASE_MIRRORING = 0x9000
    const val BASE_BANK_SELECT = 0xC000
  }
}
