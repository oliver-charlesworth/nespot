package choliver.nespot.mappers

import choliver.nespot.*
import choliver.nespot.cartridge.*

// https://wiki.nesdev.com/w/index.php/NROM
class NromMapper(rom: Rom) : Mapper {
  private val prgRam = ByteArray(PRG_RAM_SIZE)
  private val prgData = rom.prgData
  private val chrData = if (rom.chrData.isEmpty()) ByteArray(CHR_RAM_SIZE) else rom.chrData
  private val mirroring = rom.mirroring
  override val irq = false
  override val persistentRam: Ram? = null   // Don't persist PRG-RAM

  override val prg = object : Memory {
    override fun get(addr: Address) = when {
      (addr >= BASE_PRG_ROM) -> prgData[addr % prgData.size]
      (addr >= BASE_PRG_RAM) -> prgRam[addr % PRG_RAM_SIZE]
      else -> 0x00
    }.data()

    override fun set(addr: Address, data: Data) {
      when {
        (addr >= BASE_PRG_RAM) -> prgRam[addr % PRG_RAM_SIZE] = data.toByte()
      }
    }
  }

  override fun chr(vram: Memory): Memory {
    val mirroredRam = MirroringMemory(mirroring, vram)

    return object : Memory {
      override fun get(addr: Address) = when {
        (addr >= BASE_VRAM) -> mirroredRam[addr]    // This maps everything >= 0x4000 too
        else -> chrData[addr].data()
      }

      override fun set(addr: Address, data: Data) {
        when {
          (addr >= BASE_VRAM) -> mirroredRam[addr] = data   // This maps everything >= 0x4000 too
          else -> chrData[addr] = data.toByte()
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
