package choliver.nespot.mappers

import choliver.nespot.Address
import choliver.nespot.BASE_PRG_ROM
import choliver.nespot.Data
import choliver.nespot.Ram
import choliver.nespot.cartridge.ChrMemory
import choliver.nespot.cartridge.Mapper
import choliver.nespot.cartridge.PrgMemory
import choliver.nespot.cartridge.Rom

// See https://wiki.nesdev.com/w/index.php/UxROM
class UxRomMapper(rom: Rom) : Mapper {
  private val numPrgBanks = (rom.prgData.size / PRG_BANK_SIZE)
  override val irq = false
  override val persistentRam: Ram? = null

  override val prg = PrgMemory(
    raw = rom.prgData,
    bankSize = PRG_BANK_SIZE,
    onSet = ::updateReg
  )

  override val chr = ChrMemory(
    raw = ByteArray(CHR_RAM_SIZE),
    mirroring = rom.mirroring
  )

  init {
    prg.bankMap[1] = numPrgBanks - 1    // Upper bank is fixed
  }

  @Suppress("UNUSED_PARAMETER")
  private fun updateReg(addr: Address, data: Data) {
    prg.bankMap[0] = data % numPrgBanks
  }

  @Suppress("unused")
  companion object {
    const val CHR_RAM_SIZE = 8192
    const val PRG_BANK_SIZE = 16384
    const val BASE_BANK_SELECT = BASE_PRG_ROM
  }
}
