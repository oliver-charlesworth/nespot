package choliver.nespot.cartridge

import choliver.nespot.BASE_PRG_RAM
import choliver.nespot.BASE_PRG_ROM
import choliver.nespot.PRG_RAM_SIZE
import choliver.nespot.mappers.BankMappingChecker
import choliver.nespot.mappers.BankMappingChecker.Companion.takesBytes
import org.junit.jupiter.api.Test

class PrgMemoryTest {
  @Test
  fun `linear RAM loads and stores`() {
    val mapper = PrgMemory(ByteArray(0), bankSize = 1024)
    val checker = BankMappingChecker(
      bankSize = PRG_RAM_SIZE,
      srcBase = BASE_PRG_RAM,
      outBase = BASE_PRG_RAM,
      setSrc = mapper::set,
      getOut = mapper::get
    )

    checker.assertMappings(0 to 0)
  }

  @Test
  fun `non-linear ROM loads`() {
    val raw = ByteArray(65536)
    val mapper = PrgMemory(raw, bankSize = 8192)
    val checker = BankMappingChecker(
      bankSize = 8192,
      outBase = BASE_PRG_ROM,
      setSrc = takesBytes(raw::set),
      getOut = mapper::get
    )

    mapper.bankMap[0] = 7
    mapper.bankMap[1] = 4
    mapper.bankMap[2] = 5
    mapper.bankMap[3] = 2

    checker.assertMappings(7 to 0, 4 to 1, 5 to 2, 2 to 3)
  }
}
