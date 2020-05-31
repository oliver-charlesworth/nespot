package choliver.nespot.cartridge

import choliver.nespot.*
import choliver.nespot.mappers.BankMappingChecker
import choliver.nespot.mappers.BankMappingChecker.Companion.takesBytes
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.verify
import com.nhaarman.mockitokotlin2.verifyNoMoreInteractions
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

  @Test
  fun `invokes callback only for ROM addresses`() {
    val onSet = mock<(Address, Data) -> Unit>()
    val mapper = PrgMemory(ByteArray(0), bankSize = 8192, onSet = onSet)

    mapper[BASE_PRG_ROM - 5] = 32
    mapper[BASE_PRG_ROM + 5] = 33

    verify(onSet)(BASE_PRG_ROM + 5, 33)
    verifyNoMoreInteractions(onSet)
  }
}
