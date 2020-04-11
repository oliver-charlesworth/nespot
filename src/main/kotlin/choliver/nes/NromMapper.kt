package choliver.nes

import choliver.nes.Cartridge.Mirroring.IGNORED
import choliver.sixfiveohtwo.model.Memory
import choliver.sixfiveohtwo.model.UInt16
import choliver.sixfiveohtwo.model.u8

// https://wiki.nesdev.com/w/index.php/NROM
class NromMapper(private val stuff: Cartridge.Stuff) : Cartridge.Mapper {
  init {
    with(stuff) {
      validate(!hasPersistentMem, "Persistent memory")
      validate(mirroring != IGNORED, "Ignored mirroring control")
      validate(trainerData.isEmpty(), "Trainer data")
      validate(prgData.size in listOf(16384, 32768), "PRG ROM size ${prgData.size}")
      validate(chrData.size == 8192, "CHR ROM size ${chrData.size}")
    }
  }

  override val prg = object : Memory {
    override fun load(address: UInt16) = when (address) {
      in 0x6000u..0x7FFFu -> throw IndexOutOfBoundsException("PRG RAM unsupported") // TODO
      in 0x8000u..0xFFFFu -> stuff.prgData[address.toInt() and (stuff.prgData.size - 1)].u8()
      else -> throw IndexOutOfBoundsException(address.toInt())
    }
  }

  override val chr = object : Memory {
    override fun load(address: UInt16) = when (address) {
      in 0x0000u..0x1FFFu -> stuff.chrData[address.toInt()].u8()
      in 0x2000u..0x3EFFu -> TODO() // map to VRAM, taking mirroring into account
      in 0x3F00u..0x3FFFu -> TODO() // map to internal palette
      else -> throw IndexOutOfBoundsException(address.toInt())
    }
  }

  private fun validate(predicate: Boolean, message: String) {
    if (!predicate) {
      throw Cartridge.UnsupportedRomException(message)
    }
  }
}
