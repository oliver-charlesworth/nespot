package choliver.nespot.cartridge

import choliver.nespot.*

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

  override fun chr(vram: Memory): Memory {
    val mirroredRam = MirroringMemory(config.mirroring, vram)

    return object : Memory {
      override fun load(addr: Address) = if (addr >= choliver.nespot.cartridge.BASE_VRAM) {
        mirroredRam.load(addr)  // This maps everything >= 0x4000 too
      } else {
        chrRam.load(addr)
      }

      override fun store(addr: Address, data: Data) {
        if (addr >= choliver.nespot.cartridge.BASE_VRAM) {
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
    const val BASE_VRAM = 0x2000
    const val BASE_BANK_SELECT = 0xC000
  }
}
