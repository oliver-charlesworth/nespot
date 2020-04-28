package choliver.nespot.cartridge

import choliver.nespot.*
import choliver.nespot.cartridge.MapperConfig.Mirroring.*

// https://wiki.nesdev.com/w/index.php/INES_Mapper_071
class Mapper71(private val config: MapperConfig) : Mapper {
  private val chrRam = Ram(8192)
  private val numPrgBanks = (config.prgData.size / 16384)
  private var prg0Bank = 0

  override val prg = object : Memory {
    override fun load(addr: Address) = if (addr < BASE_PRG1_ROM) {
      load(addr, prg0Bank)
    } else {
      load(addr, numPrgBanks - 1) // Fixed
    }

    private fun load(addr: Address, iBank: Int) = config.prgData[(addr and 0x3FFF) + 0x4000 * iBank].data()

    override fun store(addr: Address, data: Data) {
      if (addr >= BASE_BANK_SELECT) {
        prg0Bank = data % numPrgBanks
      }
    }
  }

  override val chr = object : ChrMemory {
    // Separate implementations so we're not performing the conditional in the hot path
    override fun intercept(ram: Memory) = when (config.mirroring) {
      HORIZONTAL -> HorizontalChr(ram)
      VERTICAL -> VerticalChr(ram)
      IGNORED -> throw UnsupportedOperationException()
    }
  }

  private inner class HorizontalChr(private val ram: Memory) : Memory {
    override fun load(addr: Address) = if (addr >= BASE_VRAM) {
      ram.load(mapToVram(addr)) // This maps everything >= 0x4000 too
    } else {
      chrRam.load(addr)
    }

    override fun store(addr: Address, data: Data) {
      // This maps everything >= 0x4000 too
      if (addr >= BASE_VRAM) {
        ram.store(mapToVram(addr), data)
      } else {
        chrRam.store(addr, data)
      }
    }

    private fun mapToVram(addr: Address) = (addr and 1023) or ((addr and 2048) shr 1)
  }

  private inner class VerticalChr(private val ram: Memory) : Memory {
    override fun load(addr: Address) = if (addr >= BASE_VRAM) {
      ram.load(mapToVram(addr)) // This maps everything >= 0x4000 too
    } else {
      chrRam.load(addr)
    }

    override fun store(addr: Address, data: Data) {
      // This maps everything >= 0x4000 too
      if (addr >= BASE_VRAM) {
        ram.store(mapToVram(addr), data)
      } else {
        chrRam.store(addr, data)
      }
    }

    private fun mapToVram(addr: Address) = addr and 2047
  }

  @Suppress("unused")
  companion object {
    const val BASE_PRG0_ROM = 0x8000
    const val BASE_PRG1_ROM = 0xC000
    const val BASE_CHR_ROM = 0x0000
    const val BASE_VRAM = 0x2000
    const val BASE_BANK_SELECT = 0xC000
  }
}
