package choliver.nespot.cartridge

import choliver.nespot.Address
import choliver.nespot.Data
import choliver.nespot.Memory
import choliver.nespot.cartridge.ChrMemory.ChrLoadResult
import choliver.nespot.cartridge.ChrMemory.ChrStoreResult
import choliver.nespot.cartridge.MapperConfig.Mirroring.*
import choliver.nespot.data
import mu.KotlinLogging

// https://wiki.nesdev.com/w/index.php/NROM
class NromMapper(private val config: MapperConfig) : Mapper {
  private val logger = KotlinLogging.logger {}

  init {
    with(config) {
      validate(!hasPersistentMem, "Persistent memory")
      validate(mirroring != IGNORED, "Ignored mirroring control")
      validate(trainerData.isEmpty(), "Trainer data")
      validate(prgData.size in listOf(16384, 32768), "PRG ROM size ${prgData.size}")
      validate(chrData.size == 8192, "CHR ROM size ${chrData.size}")
    }
  }

  override val prg = object : Memory {
    override fun load(addr: Address): Data = when (addr) {
      in PRG_RAM_RANGE -> TODO()
      in PRG_ROM_RANGE -> config.prgData[addr and (config.prgData.size - 1)].data()
      else -> 0xCC
    }

    override fun store(addr: Address, data: Data) {
      when (addr) {
        in PRG_RAM_RANGE -> TODO()
        in PRG_ROM_RANGE -> logger.warn("Invalid PRG ROM store: 0x%02x -> 0x%04x".format(data, addr))
      }
    }
  }

  override val chr = object : ChrMemory {
    override fun load(addr: Address) = when (addr) {
      in CHR_ROM_RANGE -> ChrLoadResult.Data(config.chrData[addr].data())
      in VRAM_RANGE -> ChrLoadResult.VramAddr(mapToVram(addr))
      else -> throw IndexOutOfBoundsException(addr) // Should never happen?
    }

    override fun store(addr: Address, data: Data) = when (addr) {
      in CHR_ROM_RANGE -> {
        logger.warn("Invalid CHR ROM store: 0x%02x -> 0x%04x".format(data, addr))
        ChrStoreResult.None
      }
      in VRAM_RANGE -> ChrStoreResult.VramAddr(mapToVram(addr))
      else -> throw IndexOutOfBoundsException(addr) // Should never happen?
    }

    // TODO - optimise - hoist the conditional out?
    private fun mapToVram(addr: Address): Address = when (config.mirroring) {
      HORIZONTAL -> (addr and 1023) or ((addr and 2048) shr 1)
      VERTICAL -> (addr and 2047)
      IGNORED -> throw UnsupportedOperationException()
    }
  }

  private fun validate(predicate: Boolean, message: String) {
    if (!predicate) {
      throw Cartridge.UnsupportedRomException(message)
    }
  }

  companion object {
    val PRG_RAM_RANGE = 0x6000..0x7FFF
    val PRG_ROM_RANGE = 0x8000..0xFFFF
    val CHR_ROM_RANGE = 0x0000..0x1FFF
    val VRAM_RANGE    = 0x2000..0x3EFF
  }
}
