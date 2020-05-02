package choliver.nespot.cartridge.mappers

import choliver.nespot.Address
import choliver.nespot.Data
import choliver.nespot.Memory
import choliver.nespot.cartridge.BASE_VRAM
import choliver.nespot.cartridge.Mapper
import choliver.nespot.cartridge.MirroringMemory
import choliver.nespot.cartridge.Rom
import choliver.nespot.data

// https://wiki.nesdev.com/w/index.php/NROM
class NromMapper(private val rom: Rom) : Mapper {
  override val prg = object : Memory {
    // Just map everything to PRG-ROM
    override fun get(addr: Address) = rom.prgData[addr and (rom.prgData.size - 1)].data()

    // TODO - PRG-RAM
    override fun set(addr: Address, data: Data) {}
  }

  override fun chr(vram: Memory) = object : Memory {
    val mirroredRam = MirroringMemory(rom.mirroring, vram)

    override fun get(addr: Address) = if (addr >= BASE_VRAM) {
      mirroredRam[addr]  // This maps everything >= 0x4000 too
    } else {
      rom.chrData[addr].data()
    }

    override fun set(addr: Address, data: Data) {
      if (addr >= BASE_VRAM) {
        mirroredRam[addr] = data // This maps everything >= 0x4000 too
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
