package choliver.nes

import choliver.nes.Cartridge.Mirroring.*
import choliver.nes.ChrMemory.ChrLoadResult
import choliver.nes.ChrMemory.ChrStoreResult
import choliver.sixfiveohtwo.model.UInt16
import choliver.sixfiveohtwo.model.UInt8
import choliver.sixfiveohtwo.model.u16
import choliver.sixfiveohtwo.model.u8
import mu.KotlinLogging

// https://wiki.nesdev.com/w/index.php/NROM
class NromMapper(private val stuff: Cartridge.Stuff) : Cartridge.Mapper {
  private val logger = KotlinLogging.logger {}

  init {
    with(stuff) {
      validate(!hasPersistentMem, "Persistent memory")
      validate(mirroring != IGNORED, "Ignored mirroring control")
      validate(trainerData.isEmpty(), "Trainer data")
      validate(prgData.size in listOf(16384, 32768), "PRG ROM size ${prgData.size}")
      validate(chrData.size == 8192, "CHR ROM size ${chrData.size}")
    }
  }

  override val prg = object : PrgMemory {
    override fun load(addr: UInt16) = when (addr) {
      in PRG_RAM_RANGE -> TODO()
      in PRG_ROM_RANGE -> stuff.prgData[addr.toInt() and (stuff.prgData.size - 1)].u8()
      else -> null
    }

    override fun store(addr: UInt16, data: UInt8) {
      when (addr) {
        in PRG_RAM_RANGE -> TODO()
        in PRG_ROM_RANGE -> logger.warn("Invalid PRG ROM store: 0x%02x -> 0x%04x".format(data.toByte(), addr.toShort()))
      }
    }
  }

  override val chr = object : ChrMemory {
    override fun load(addr: UInt16) = when (addr) {
      in CHR_ROM_RANGE -> ChrLoadResult.Data(stuff.chrData[addr.toInt()].u8())
      in VRAM_RANGE -> ChrLoadResult.VramAddr(mapToVram(addr))
      else -> throw IndexOutOfBoundsException(addr.toInt()) // Should never happen?
    }

    override fun store(addr: UInt16, data: UInt8) = when (addr) {
      in CHR_ROM_RANGE -> {
        logger.warn("Invalid CHR ROM store: 0x%02x -> 0x%04x".format(data.toByte(), addr.toShort()))
        ChrStoreResult.None
      }
      in VRAM_RANGE -> ChrStoreResult.VramAddr(mapToVram(addr))
      else -> throw IndexOutOfBoundsException(addr.toInt()) // Should never happen?
    }

    // TODO - optimise - hoist the conditional out?
    private fun mapToVram(addr: UInt16): UInt16 = when (stuff.mirroring) {
      HORIZONTAL -> (addr and 1023u) or ((addr and 2048u).toInt() shr 1).u16()
      VERTICAL -> (addr and 2047u)
      IGNORED -> throw UnsupportedOperationException()
    }
  }

  private fun validate(predicate: Boolean, message: String) {
    if (!predicate) {
      throw Cartridge.UnsupportedRomException(message)
    }
  }

  companion object {
    val PRG_RAM_RANGE = 0x6000u..0x7FFFu
    val PRG_ROM_RANGE = 0x8000u..0xFFFFu
    val CHR_ROM_RANGE = 0x0000u..0x1FFFu
    val VRAM_RANGE    = 0x2000u..0x3EFFu
  }
}
