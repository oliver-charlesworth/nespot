package choliver.nespot.mappers

import choliver.nespot.*
import choliver.nespot.cartridge.Mapper
import choliver.nespot.cartridge.Rom
import choliver.nespot.cartridge.vramAddr


// See https://wiki.nesdev.com/w/index.php/CNROM
class CnRomMapper(private val rom: Rom) : Mapper {
  private val vram = ByteArray(VRAM_SIZE)
  private val numChrBanks = (rom.chrData.size / CHR_BANK_SIZE)
  private var chrBank = 0

  override val irq = false
  override val persistentRam: Ram? = null

  override val prg = object : Memory {
    override fun get(addr: Address) = when {
      (addr >= BASE_PRG_ROM) -> rom.prgData[addr % rom.prgData.size]
      else -> 0x00
    }.data()

    override operator fun set(addr: Address, data: Data) {
      if (addr >= BASE_BANK_SELECT) {
        chrBank = data % numChrBanks
      }
    }
  }

  override val chr = object : Memory {
    override fun get(addr: Address) = when {
      (addr >= BASE_VRAM) -> vram[vramAddr(rom.mirroring, addr)]    // This maps everything >= 0x4000 too
      else -> rom.chrData[chrAddr(addr)]
    }.data()

    override fun set(addr: Address, data: Data) {
      when {
        (addr >= BASE_VRAM) -> vram[vramAddr(rom.mirroring, addr)] = data.toByte()   // This maps everything >= 0x4000 too
      }
    }
  }

  private fun chrAddr(addr: Address): Int {
    return (addr % CHR_BANK_SIZE) + chrBank * CHR_BANK_SIZE
  }

  @Suppress("unused")
  companion object {
    const val PRG_BANK_SIZE = 16384
    const val CHR_BANK_SIZE = 8192

    const val BASE_BANK_SELECT = BASE_PRG_ROM
  }
}
