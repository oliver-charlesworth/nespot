package choliver.nespot.mappers

import choliver.nespot.Ram
import choliver.nespot.cartridge.ChrMemory
import choliver.nespot.cartridge.Mapper
import choliver.nespot.cartridge.PrgMemory
import choliver.nespot.cartridge.Rom

// https://wiki.nesdev.com/w/index.php/NROM
class NromMapper(rom: Rom) : Mapper {
  override val irq = false
  override val persistentRam: Ram? = null   // Don't persist PRG-RAM

  override val prg = PrgMemory(
    raw = rom.prgData,
    bankSize = PRG_BANK_SIZE
  )

  override val chr = ChrMemory(
    raw = if (rom.chrData.isEmpty()) ByteArray(CHR_RAM_SIZE) else rom.chrData,
    mirroring = rom.mirroring
  )

  init {
    prg.bankMap[1] = if (rom.prgData.size > PRG_BANK_SIZE) 1 else 0
  }

  @Suppress("unused")
  companion object {
    const val CHR_RAM_SIZE = 8192
    const val PRG_BANK_SIZE = 16384
  }
}
