package choliver.nespot.mappers

import choliver.nespot.BASE_PRG_ROM
import choliver.nespot.cartridge.Mapper
import choliver.nespot.cartridge.Rom

// See https://wiki.nesdev.com/w/index.php/UxROM
object UxRomMapper {
  const val CHR_RAM_SIZE = 8192
  const val PRG_BANK_SIZE = 16384
  const val BASE_BANK_SELECT = BASE_PRG_ROM

  fun create(rom: Rom) : Mapper {
    val numPrgBanks = (rom.prgData.size / PRG_BANK_SIZE)

    val mapper = ParameterisedMapper(
      prgData = rom.prgData,
      chrData = ByteArray(CHR_RAM_SIZE),
      prgBankSize = PRG_BANK_SIZE,
      onPrgSet = { _, data ->
        prg.bankMap[0] = data % numPrgBanks
      }
    )

    with(mapper) {
      chr.mirroring = rom.mirroring
      prg.bankMap[1] = numPrgBanks - 1    // Upper bank is fixed
    }

    return mapper
  }
}

