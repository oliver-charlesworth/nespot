package choliver.nespot.mappers

import choliver.nespot.cartridge.Rom
import choliver.nespot.cartridge.Rom.Mirroring.VERTICAL
import choliver.nespot.cartridge.StandardMapper
import choliver.nespot.mappers.CnRomMapperConfig.Companion.BASE_BANK_SELECT
import choliver.nespot.mappers.CnRomMapperConfig.Companion.CHR_BANK_SIZE
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.verify
import com.nhaarman.mockitokotlin2.verifyNoMoreInteractions
import org.junit.jupiter.api.Test
import org.mockito.Answers.RETURNS_DEEP_STUBS

class CnRomMapperConfigTest {
  private val mapper = mock<StandardMapper>(defaultAnswer = RETURNS_DEEP_STUBS)

  @Test
  fun `dynamically sets lower bank`() {
    with(CnRomMapperConfig(Rom(chrData = ByteArray(8 * CHR_BANK_SIZE)))) {
      mapper.onPrgSet(BASE_BANK_SELECT, 6)

      verify(mapper.chr.bankMap)[0] = 6
    }
  }

  @Test
  fun `bank mapping wraps`() {
    with(CnRomMapperConfig(Rom(chrData = ByteArray(8 * CHR_BANK_SIZE)))) {
      mapper.onPrgSet(BASE_BANK_SELECT, 6 + 8)

      verify(mapper.chr.bankMap)[0] = 6
    }
  }

  @Test
  fun `maps both PRG banks to 0 for size-16384`() {
    with(CnRomMapperConfig(Rom(prgData = ByteArray(16384)))) {
      mapper.onStartup()
    }

    verify(mapper.prg.bankMap)[1] = 0
    verifyNoMoreInteractions(mapper.prg.bankMap)
  }

  @Test
  fun `maps both PRG banks to 1 for size-32768`() {
    with(CnRomMapperConfig(Rom(prgData = ByteArray(32768)))) {
      mapper.onStartup()
    }

    verify(mapper.prg.bankMap)[1] = 1
    verifyNoMoreInteractions(mapper.prg.bankMap)
  }

  @Test
  fun `sets mirroring on startup`() {
    with(CnRomMapperConfig(Rom(mirroring = VERTICAL))) {
      mapper.onStartup()
    }

    verify(mapper.chr).mirroring = VERTICAL
  }
}
