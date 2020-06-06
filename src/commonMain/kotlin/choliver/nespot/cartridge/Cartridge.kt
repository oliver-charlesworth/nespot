package choliver.nespot.cartridge

import choliver.nespot.Ram
import choliver.nespot.mappers.*

class Cartridge(private val mapper: Mapper) {
  // TODO - can we avoid the callback overhead if callbacks not set?
  val prg = with(mapper) {
    PrgMemory(
      raw = prgData,
      bankSize = prgBankSize,
      onSet = { addr, data -> onPrgSet(addr, data) }
    )
  }

  // TODO - can we avoid the callback overhead if callbacks not set?
  val chr = with(mapper) {
    ChrMemory(
      raw = chrData,
      bankSize = chrBankSize,
      onGet = { addr -> onChrGet(addr) },
      onSet = { addr, data -> onChrSet(addr, data) }
    )
  }

  val irq get() = mapper.irq

  val persistentRam = if (mapper.persistRam) {
    Ram.backedBy(prg.ram)
  } else null

  init {
    with(mapper) {
      onStartup()
    }
  }

  companion object {
    // See https://wiki.nesdev.com/w/index.php/Mapper#iNES_1.0_mapper_grid
    fun create(
      rom: Rom,
      getStepCount: () -> Int = { 0 }   // Some mappers need access to this to identify consecutive load/stores
    ): Cartridge {
      validateMagicNumber(rom)
      return Cartridge(
        when (rom.mapper) {
          0 -> NromMapper(rom)
          1 -> Mmc1Mapper(rom, getStepCount)
          2 -> UxRomMapper(rom)
          3 -> CnRomMapper(rom)
          4 -> Mmc3Mapper(rom)
          7 -> AxRomMapper(rom)
          71 -> Mapper71(rom)
          else -> throw RuntimeException("Mapper #${rom.mapper}")
        }
      )
    }

    private fun validateMagicNumber(rom: Rom) {
      if (rom.magic.toList() != listOf('N'.toByte(), 'E'.toByte(), 'S'.toByte(), 0x1A.toByte())) {
        throw IllegalArgumentException("Invalid magic number ${rom.magic.toList()}")
      }
    }
  }
}
