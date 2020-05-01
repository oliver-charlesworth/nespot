package choliver.nespot.cartridge.mappers

import choliver.nespot.*
import choliver.nespot.cartridge.BASE_VRAM
import choliver.nespot.cartridge.Mapper
import choliver.nespot.cartridge.MirroringMemory
import choliver.nespot.cartridge.Rom

// See https://wiki.nesdev.com/w/index.php/UxROM
class UxRomMapper(private val rom: Rom) : Mapper {
  private val chrRam = Ram(8192)
  private val numPrgBanks = (rom.prgData.size / 16384)
  private var prg0Bank = 0

  override val prg = object : Memory {
    override fun load(addr: Address) = if (addr < BASE_PRG1_ROM) {
      load(addr, prg0Bank)
    } else {
      load(addr, numPrgBanks - 1) // Fixed
    }

    private fun load(addr: Address, iBank: Int) = rom.prgData[(addr and 0x3FFF) + 0x4000 * iBank].data()

    override fun store(addr: Address, data: Data) {
      if (addr >= BASE_BANK_SELECT) {
        prg0Bank = data % numPrgBanks
      }
    }
  }

  override fun chr(vram: Memory): Memory {
    val mirroredRam = MirroringMemory(rom.mirroring, vram)

    return object : Memory {
      override fun load(addr: Address) = if (addr >= BASE_VRAM) {
        mirroredRam.load(addr)  // This maps everything >= 0x4000 too
      } else {
        chrRam.load(addr)
      }

      override fun store(addr: Address, data: Data) {
        if (addr >= BASE_VRAM) {
          mirroredRam.store(addr, data) // This maps everything >= 0x4000 too
        } else {
          chrRam.store(addr, data)
        }
      }
    }
  }

  @Suppress("unused")
  companion object {
    const val BASE_PRG0_ROM = 0x8000
    const val BASE_PRG1_ROM = 0xC000
    const val BASE_CHR_ROM = 0x0000
    const val BASE_BANK_SELECT = 0xC000
  }
}
