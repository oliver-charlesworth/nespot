package choliver.nespot.mappers

import choliver.nespot.*
import choliver.nespot.cartridge.BoringChr
import choliver.nespot.cartridge.Mapper
import choliver.nespot.cartridge.Rom


// See https://wiki.nesdev.com/w/index.php/CNROM
class CnRomMapper(private val rom: Rom) : Mapper {
  private val numChrBanks = (rom.chrData.size / CHR_BANK_SIZE)

  override val irq = false
  override val persistentRam: Ram? = null

  override val prg = object : Memory {
    override fun get(addr: Address) = when {
      (addr >= BASE_PRG_ROM) -> rom.prgData[addr % rom.prgData.size]
      else -> 0x00
    }.data()

    override operator fun set(addr: Address, data: Data) {
      if (addr >= BASE_BANK_SELECT) {
        chr.bank = data % numChrBanks
      }
    }
  }

  override val chr = BoringChr(
    raw = rom.chrData,
    bankSize = CHR_BANK_SIZE,
    mirroring = rom.mirroring
  )

  @Suppress("unused")
  companion object {
    const val PRG_BANK_SIZE = 16384
    const val CHR_BANK_SIZE = 8192

    const val BASE_BANK_SELECT = BASE_PRG_ROM
  }
}
