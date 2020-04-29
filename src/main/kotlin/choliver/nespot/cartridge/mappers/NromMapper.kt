package choliver.nespot.cartridge.mappers

import choliver.nespot.Address
import choliver.nespot.Data
import choliver.nespot.Memory
import choliver.nespot.cartridge.BASE_VRAM
import choliver.nespot.cartridge.Mapper
import choliver.nespot.cartridge.MapperConfig
import choliver.nespot.cartridge.MirroringMemory
import choliver.nespot.data

// https://wiki.nesdev.com/w/index.php/NROM
class NromMapper(private val config: MapperConfig) : Mapper {
  override val prg = object : Memory {
    // Just map everything to PRG-ROM
    override fun load(addr: Address) = config.prgData[addr and (config.prgData.size - 1)].data()

    // TODO - PRG-RAM
    override fun store(addr: Address, data: Data) {}
  }

  override fun chr(vram: Memory) = object : Memory {
    val mirroredRam = MirroringMemory(config.mirroring, vram)

    override fun load(addr: Address) = if (addr >= BASE_VRAM) {
      mirroredRam.load(addr)  // This maps everything >= 0x4000 too
    } else {
      config.chrData[addr].data()
    }

    override fun store(addr: Address, data: Data) {
      if (addr >= BASE_VRAM) {
        mirroredRam.store(addr, data) // This maps everything >= 0x4000 too
      }
    }
  }

  @Suppress("unused")
  companion object {
    const val BASE_PRG_RAM = 0x6000
    const val BASE_PRG_ROM = 0x8000
    const val BASE_CHR_ROM = 0x0000
  }
}
