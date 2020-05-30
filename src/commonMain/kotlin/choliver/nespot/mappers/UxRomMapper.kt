package choliver.nespot.mappers

import choliver.nespot.*
import choliver.nespot.cartridge.Mapper
import choliver.nespot.cartridge.Rom
import choliver.nespot.cartridge.vramAddr


// See https://wiki.nesdev.com/w/index.php/UxROM
class UxRomMapper(private val rom: Rom) : Mapper {
  private val vram = ByteArray(VRAM_SIZE)
  private val chrRam = ByteArray(CHR_RAM_SIZE)
  private val numPrgBanks = (rom.prgData.size / PRG_BANK_SIZE)
  private var prg0Bank = 0

  override val irq = false
  override val persistentRam: Ram? = null

  override val prg = object : Memory {
    override operator fun get(addr: Address) = getFromBank(
      addr,
      if (addr < BASE_PRG1_ROM) { prg0Bank } else { numPrgBanks - 1 } // Upper bank is fixed
    )

    private fun getFromBank(addr: Address, iBank: Int) =
      rom.prgData[(addr % PRG_BANK_SIZE) + iBank * PRG_BANK_SIZE].data()

    override operator fun set(addr: Address, data: Data) {
      if (addr >= BASE_BANK_SELECT) {
        prg0Bank = data % numPrgBanks
      }
    }
  }

  override val chr = object : Memory {
    override fun get(addr: Address) = when {
      (addr >= BASE_VRAM) -> vram[vramAddr(rom.mirroring, addr)]    // This maps everything >= 0x4000 too
      else -> chrRam[addr]
    }.data()

    override fun set(addr: Address, data: Data) {
      when {
        (addr >= BASE_VRAM) -> vram[vramAddr(rom.mirroring, addr)] = data.toByte()   // This maps everything >= 0x4000 too
        else -> chrRam[addr] = data.toByte()
      }
    }
  }

  @Suppress("unused")
  companion object {
    const val CHR_RAM_SIZE = 8192
    const val PRG_BANK_SIZE = 16384

    const val BASE_PRG0_ROM = BASE_PRG_ROM
    const val BASE_PRG1_ROM = BASE_PRG_ROM + PRG_BANK_SIZE
    const val BASE_BANK_SELECT = BASE_PRG_ROM
  }
}
