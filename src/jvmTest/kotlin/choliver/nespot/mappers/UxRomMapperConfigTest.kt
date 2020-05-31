package choliver.nespot.mappers

import choliver.nespot.cartridge.Rom
import choliver.nespot.cartridge.Rom.Mirroring.VERTICAL
import choliver.nespot.cartridge.StandardMapper
import choliver.nespot.mappers.UxRomMapperConfig.Companion.BASE_BANK_SELECT
import choliver.nespot.mappers.UxRomMapperConfig.Companion.CHR_RAM_SIZE
import choliver.nespot.mappers.UxRomMapperConfig.Companion.PRG_BANK_SIZE
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.verify
import com.nhaarman.mockitokotlin2.verifyNoMoreInteractions
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.mockito.Answers.RETURNS_DEEP_STUBS

class UxRomMapperConfigTest {
  private val mapper = mock<StandardMapper>(defaultAnswer = RETURNS_DEEP_STUBS)

  @Test
  fun `configures CHR RAM`() {
    val config = UxRomMapperConfig(Rom())

    assertEquals(CHR_RAM_SIZE, config.chrData.size)
  }

  @Test
  fun `fixes upper bank on startup`() {
    with(UxRomMapperConfig(Rom(prgData = ByteArray(8 * PRG_BANK_SIZE)))) {
      mapper.onStartup()
    }

    verify(mapper.prg.bankMap)[1] = 7
    verifyNoMoreInteractions(mapper.prg.bankMap)
  }

  @Test
  fun `dynamically sets lower bank`() {
    with(UxRomMapperConfig(Rom(prgData = ByteArray(8 * PRG_BANK_SIZE)))) {
      mapper.onPrgSet(BASE_BANK_SELECT, 6)

      verify(mapper.prg.bankMap)[0] = 6
    }
  }

  @Test
  fun `bank mapping wraps`() {
    with(UxRomMapperConfig(Rom(prgData = ByteArray(8 * PRG_BANK_SIZE)))) {
      mapper.onPrgSet(BASE_BANK_SELECT, 6 + 8)

      verify(mapper.prg.bankMap)[0] = 6
    }
  }

  @Test
  fun `sets mirroring on startup`() {
    with(UxRomMapperConfig(Rom(mirroring = VERTICAL))) {
      mapper.onStartup()
    }

    verify(mapper.chr).mirroring = VERTICAL
  }
}
