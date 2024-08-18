package choliver.nespot.cartridge

import choliver.nespot.BASE_PRG_RAM
import choliver.nespot.BASE_PRG_ROM
import choliver.nespot.PRG_RAM_SIZE
import choliver.nespot.cartridge.BankMappingChecker.Companion.takesBytes
import choliver.nespot.common.Address
import choliver.nespot.common.Data
import org.junit.jupiter.api.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.verifyNoMoreInteractions

class PrgMemoryTest {
  @Test
  fun `linear RAM loads and stores`() {
    val mem = PrgMemory(ByteArray(0), bankSize = 1024)
    val checker = BankMappingChecker(
      bankSize = PRG_RAM_SIZE,
      srcBase = BASE_PRG_RAM,
      outBase = BASE_PRG_RAM,
      setSrc = mem::set,
      getOut = mem::get
    )

    checker.assertMappings(0 to 0)
  }

  @Test
  fun `non-linear ROM loads`() {
    val raw = ByteArray(65536)
    val mem = PrgMemory(raw, bankSize = 8192)
    val checker = BankMappingChecker(
      bankSize = 8192,
      outBase = BASE_PRG_ROM,
      setSrc = takesBytes(raw::set),
      getOut = mem::get
    )

    mem.bankMap[0] = 7
    mem.bankMap[1] = 4
    mem.bankMap[2] = 5
    mem.bankMap[3] = 2

    checker.assertMappings(7 to 0, 4 to 1, 5 to 2, 2 to 3)
  }

  @Test
  fun `invokes callback only for ROM addresses`() {
    val onSet = mock<(Address, Data) -> Unit>()
    val mem = PrgMemory(ByteArray(0), bankSize = 8192, onSet = onSet)

    mem[BASE_PRG_ROM - 5] = 32
    mem[BASE_PRG_ROM + 5] = 33

    verify(onSet)(BASE_PRG_ROM + 5, 33)
    verifyNoMoreInteractions(onSet)
  }
}
