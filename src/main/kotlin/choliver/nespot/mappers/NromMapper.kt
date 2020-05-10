package choliver.nespot.mappers

import choliver.nespot.*
import choliver.nespot.cartridge.BASE_VRAM
import choliver.nespot.cartridge.Mapper
import choliver.nespot.cartridge.MirroringMemory
import choliver.nespot.cartridge.Rom

// https://wiki.nesdev.com/w/index.php/NROM
class NromMapper(private val rom: Rom) : Mapper {
  private val prgRam = Ram(PRG_RAM_SIZE)
  private val chrRam = Ram(CHR_RAM_SIZE)
  private val usingChrRam = rom.chrData.isEmpty()
  override val irq = false
  override val persistentRam: Ram? = null   // Don't persist PRG-RAM

  override val prg = object : Memory {
    override fun get(addr: Address) = when {
      (addr >= BASE_PRG_ROM) -> rom.prgData[addr and (rom.prgData.size - 1)].data()
      (addr >= BASE_PRG_RAM) -> prgRam[addr and (PRG_RAM_SIZE - 1)]
      else -> 0x00
    }

    override fun set(addr: Address, data: Data) {
      when {
        (addr >= BASE_PRG_RAM) -> prgRam[addr and (PRG_RAM_SIZE - 1)] = data
      }
    }
  }

  override fun chr(vram: Memory): Memory {
    val mirroredRam = MirroringMemory(rom.mirroring, vram)

    return if (usingChrRam) {
      object : Memory {
        override fun get(addr: Address) = when {
          (addr >= BASE_VRAM) -> mirroredRam[addr]    // This maps everything >= 0x4000 too
          else -> rom.chrData[addr].data()
        }

        override fun set(addr: Address, data: Data) {
          when {
            (addr >= BASE_VRAM) -> mirroredRam[addr] = data   // This maps everything >= 0x4000 too
          }
        }
      }
    } else {
      object : Memory {
        override fun get(addr: Address) = when {
          (addr >= BASE_VRAM) -> mirroredRam[addr]    // This maps everything >= 0x4000 too
          else -> chrRam[addr]
        }

        override fun set(addr: Address, data: Data) {
          when {
            (addr >= BASE_VRAM) -> mirroredRam[addr] = data   // This maps everything >= 0x4000 too
            else -> chrRam[addr] = data
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

    const val PRG_RAM_SIZE = 8192
    const val CHR_RAM_SIZE = 8192
  }
}
