package choliver.nespot.cartridge

import choliver.nespot.*

// https://wiki.nesdev.com/w/index.php/MMC1
class Mmc1Mapper(private val config: MapperConfig) : Mapper {
  private val numPrgBanks = (config.prgData.size / 16384)
  private val numChrBanks = (config.chrData.size / 4096)
  private var srCount = 0
  private var sr = 0
  private var chr0Bank = 0
  private var chr1Bank = 0
  private var prgBank = numPrgBanks - 1
  private var mirrorMode = 0
  private var chrMode = 0
  private var prgMode = 0

  init {
    with(config) {
      validate(!hasPersistentMem, "Persistent memory")
      validate(trainerData.isEmpty(), "Trainer data")
      validate((prgData.size % 32768) == 0, "PRG ROM size ${prgData.size}")
      validate((chrData.size % 8192) == 0, "CHR ROM size ${chrData.size}")
    }
  }

  // TODO - PRG-RAM
  override val prg = object : Memory {
    override fun load(addr: Address) = if (addr < BASE_PRG1_ROM) {
      load(addr, when (prgMode) {
        0, 1 -> (prgBank and 0x0E) // 32k mode
        2 -> 0 // Fixed
        3 -> prgBank // Variable
        else -> throw IllegalArgumentException()  // Should never happen
      })
    } else {
      load(addr, when (prgMode) {
        0, 1 -> (prgBank or 0x01) // 32k mode
        2 -> prgBank  // Variable
        3 -> numPrgBanks - 1 // Fixed
        else -> throw IllegalArgumentException()  // Should never happen
      })
    }

    private fun load(addr: Address, iBank: Int) = config.prgData[(addr and 0x3FFF) + 0x4000 * iBank].data()

    override fun store(addr: Address, data: Data) {
      if (addr >= BASE_SR) {
        updateShiftRegister(addr, data)
      }
    }
  }

  override val chr = object : ChrMemory {
    override fun intercept(ram: Memory) = object : Memory {
      override fun load(addr: Address) = when {
        addr >= BASE_VRAM -> ram.load(mapToVram(addr))  // This maps everything >= 0x4000 too
        addr < BASE_CHR1_ROM -> load(addr, when (chrMode) {
          0 -> (chr0Bank and 0x1E)
          1 -> chr0Bank
          else -> throw IllegalArgumentException()  // Should never happen
        })
        else -> load(addr, when (chrMode) {
          0 -> (chr0Bank or 0x01)
          1 -> chr1Bank
          else -> throw IllegalArgumentException()  // Should never happen
        })
      }

      private fun load(addr: Address, iBank: Int) = config.chrData[(addr and 0x0FFF) + 0x1000 * iBank].data()

      override fun store(addr: Address, data: Data) {
        // This maps everything >= 0x4000 too
        if (addr >= BASE_VRAM) {
          ram.store(mapToVram(addr), data)
        }
      }

      private fun mapToVram(addr: Address): Address = when (mirrorMode) {
        0 -> (addr and 1023)
        1 -> (addr and 1023) + 1024
        2 -> (addr and 2047)
        3 -> (addr and 1023) or ((addr and 2048) shr 1)
        else -> throw UnsupportedOperationException()   // Should never happen
      }
    }
  }

  private fun updateShiftRegister(addr: Address, data: Data) {
    if (data.isBitSet(7)) {
      // Reset
      srCount = 5
      sr = 0x00
    } else {
      sr = (sr shr 1) or ((data and 1) shl 4)
      if (--srCount == 0) {
        when ((addr and 0x6000) shr 13) {
          0 -> {
            mirrorMode = (sr and 0x03)
            prgMode = (sr and 0x0C) shr 2
            chrMode = (sr and 0x10) shr 4
          }
          1 -> chr0Bank = sr % numChrBanks
          2 -> chr1Bank = sr % numChrBanks
          3 -> prgBank = sr % numPrgBanks
        }
        // Reset
        srCount = 5
        sr = 0x00
      }
    }
  }

  private fun validate(predicate: Boolean, message: String) {
    if (!predicate) {
      throw Cartridge.UnsupportedRomException(message)
    }
  }

  @Suppress("unused")
  companion object {
    const val BASE_PRG0_ROM = 0x8000
    const val BASE_PRG1_ROM = 0xC000
    const val BASE_CHR0_ROM = 0x0000
    const val BASE_CHR1_ROM = 0x1000
    const val BASE_VRAM = 0x2000
    const val BASE_SR = 0x8000
  }
}
