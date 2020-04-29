package choliver.nespot.cartridge

import choliver.nespot.Address
import choliver.nespot.Data
import choliver.nespot.Memory
import choliver.nespot.data

// https://wiki.nesdev.com/w/index.php/NROM
class NromMapper(private val config: MapperConfig) : Mapper {
  override val prg = object : Memory {
    // Just map everything to PRG-ROM
    override fun load(addr: Address) = config.prgData[addr and (config.prgData.size - 1)].data()

    // TODO - PRG-RAM
    override fun store(addr: Address, data: Data) {}
  }

  override val chr = object : ChrMemory {
    // Separate implementations so we're not performing the conditional in the hot path
    override fun intercept(ram: Memory): Memory {
      val mirroredRam = MirroringMemory(config.mirroring, ram)

      return object : Memory {
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
    }
  }

  @Suppress("unused")
  companion object {
    const val BASE_PRG_RAM = 0x6000
    const val BASE_PRG_ROM = 0x8000
    const val BASE_CHR_ROM = 0x0000
  }
}
