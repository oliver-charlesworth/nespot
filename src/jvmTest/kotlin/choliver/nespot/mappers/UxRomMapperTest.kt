package choliver.nespot.mappers

import choliver.nespot.cartridge.Cartridge
import choliver.nespot.cartridge.Rom
import choliver.nespot.cartridge.Rom.Mirroring.VERTICAL
import choliver.nespot.mappers.UxRomMapper.Companion.BASE_BANK_SELECT
import choliver.nespot.mappers.UxRomMapper.Companion.CHR_RAM_SIZE
import choliver.nespot.mappers.UxRomMapper.Companion.PRG_BANK_SIZE
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.mockito.Answers.RETURNS_DEEP_STUBS
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.verifyNoMoreInteractions

class UxRomMapperTest {
  private val cartridge = mock<Cartridge>(defaultAnswer = RETURNS_DEEP_STUBS)

  @Test
  fun `configures CHR RAM`() {
    val mapper = UxRomMapper(Rom())

    assertEquals(CHR_RAM_SIZE, mapper.chrData.size)
  }

  @Test
  fun `fixes upper bank on startup`() {
    with(UxRomMapper(Rom(prgData = ByteArray(8 * PRG_BANK_SIZE)))) {
      cartridge.onStartup()
    }

    verify(cartridge.prg.bankMap)[1] = 7
    verifyNoMoreInteractions(cartridge.prg.bankMap)
  }

  @Test
  fun `dynamically sets lower bank`() {
    with(UxRomMapper(Rom(prgData = ByteArray(8 * PRG_BANK_SIZE)))) {
      cartridge.onPrgSet(BASE_BANK_SELECT, 6)

      verify(cartridge.prg.bankMap)[0] = 6
    }
  }

  @Test
  fun `bank mapping wraps`() {
    with(UxRomMapper(Rom(prgData = ByteArray(8 * PRG_BANK_SIZE)))) {
      cartridge.onPrgSet(BASE_BANK_SELECT, 6 + 8)

      verify(cartridge.prg.bankMap)[0] = 6
    }
  }

  @Test
  fun `sets mirroring on startup`() {
    with(UxRomMapper(Rom(mirroring = VERTICAL))) {
      cartridge.onStartup()
    }

    verify(cartridge.chr).mirroring = VERTICAL
  }
}
