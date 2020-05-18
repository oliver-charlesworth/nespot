package choliver.nespot.mappers

import choliver.nespot.*
import choliver.nespot.cartridge.*

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
          else -> chrRam[addr]
        }

        override fun set(addr: Address, data: Data) {
          when {
            (addr >= BASE_VRAM) -> mirroredRam[addr] = data   // This maps everything >= 0x4000 too
            else -> chrRam[addr] = data
          }
        }
      }
    } else {
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
    }
  }

  @Suppress("unused")
  companion object {
    const val PRG_RAM_SIZE = 8192
    const val CHR_RAM_SIZE = 8192
    const val PRG_BANK_SIZE = 16384

    const val BASE_PRG_RAM = BASE_PRG_ROM - PRG_RAM_SIZE
  }
}
