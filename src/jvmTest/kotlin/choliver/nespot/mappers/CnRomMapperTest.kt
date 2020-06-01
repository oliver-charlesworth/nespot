package choliver.nespot.mappers

import choliver.nespot.cartridge.Cartridge
import choliver.nespot.cartridge.Rom
import choliver.nespot.cartridge.Rom.Mirroring.VERTICAL
import choliver.nespot.mappers.CnRomMapper.Companion.BASE_BANK_SELECT
import choliver.nespot.mappers.CnRomMapper.Companion.CHR_BANK_SIZE
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.verify
import com.nhaarman.mockitokotlin2.verifyNoMoreInteractions
import org.junit.jupiter.api.Test
import org.mockito.Answers.RETURNS_DEEP_STUBS

class CnRomMapperTest {
  private val cartridge = mock<Cartridge>(defaultAnswer = RETURNS_DEEP_STUBS)

  @Test
  fun `dynamically sets lower bank`() {
    with(CnRomMapper(Rom(chrData = ByteArray(8 * CHR_BANK_SIZE)))) {
      cartridge.onPrgSet(BASE_BANK_SELECT, 6)

      verify(cartridge.chr.bankMap)[0] = 6
    }
  }

  @Test
  fun `bank mapping wraps`() {
    with(CnRomMapper(Rom(chrData = ByteArray(8 * CHR_BANK_SIZE)))) {
      cartridge.onPrgSet(BASE_BANK_SELECT, 6 + 8)

      verify(cartridge.chr.bankMap)[0] = 6
    }
  }

  @Test
  fun `maps both PRG banks to 0 for size-16384`() {
    with(CnRomMapper(Rom(prgData = ByteArray(16384)))) {
      cartridge.onStartup()
    }

    verify(cartridge.prg.bankMap)[1] = 0
    verifyNoMoreInteractions(cartridge.prg.bankMap)
  }

  @Test
  fun `maps both PRG banks to 1 for size-32768`() {
    with(CnRomMapper(Rom(prgData = ByteArray(32768)))) {
      cartridge.onStartup()
    }

    verify(cartridge.prg.bankMap)[1] = 1
    verifyNoMoreInteractions(cartridge.prg.bankMap)
  }

  @Test
  fun `sets mirroring on startup`() {
    with(CnRomMapper(Rom(mirroring = VERTICAL))) {
      cartridge.onStartup()
    }

    verify(cartridge.chr).mirroring = VERTICAL
  }
}
