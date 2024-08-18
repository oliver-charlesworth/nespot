package choliver.nespot.mappers

import choliver.nespot.cartridge.Cartridge
import choliver.nespot.cartridge.Rom
import choliver.nespot.cartridge.Rom.Mirroring.*
import choliver.nespot.mappers.Mapper71.Companion.BASE_BANK_SELECT
import choliver.nespot.mappers.Mapper71.Companion.BASE_MIRRORING
import choliver.nespot.mappers.Mapper71.Companion.CHR_RAM_SIZE
import choliver.nespot.mappers.Mapper71.Companion.PRG_BANK_SIZE
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.mockito.Answers.RETURNS_DEEP_STUBS
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.verifyNoMoreInteractions

class Mapper71Test {
  private val cartridge = mock<Cartridge>(defaultAnswer = RETURNS_DEEP_STUBS)

  @Test
  fun `configures CHR RAM`() {
    val mapper = Mapper71(Rom())

    assertEquals(CHR_RAM_SIZE, mapper.chrData.size)
  }

  @Test
  fun `fixes upper bank on startup`() {
    with(Mapper71(Rom(prgData = ByteArray(8 * PRG_BANK_SIZE)))) {
      cartridge.onStartup()
    }

    verify(cartridge.prg.bankMap)[1] = 7
    verifyNoMoreInteractions(cartridge.prg.bankMap)
  }

  @Test
  fun `dynamically sets lower bank`() {
    with(Mapper71(Rom(prgData = ByteArray(8 * PRG_BANK_SIZE)))) {
      cartridge.onPrgSet(BASE_BANK_SELECT, 6)

      verify(cartridge.prg.bankMap)[0] = 6
    }
  }

  @Test
  fun `bank mapping wraps`() {
    with(Mapper71(Rom(prgData = ByteArray(8 * PRG_BANK_SIZE)))) {
      cartridge.onPrgSet(BASE_BANK_SELECT, 6 + 8)

      verify(cartridge.prg.bankMap)[0] = 6
    }
  }

  @Nested
  inner class Mirroring {
    @Test
    fun `on startup`() {
      with(Mapper71(Rom(mirroring = VERTICAL))) {
        cartridge.onStartup()
      }

      verify(cartridge.chr).mirroring = VERTICAL
    }

    @Test
    fun `dynamic - fixed-lower`() {
      with(Mapper71(Rom())) {
        cartridge.onPrgSet(BASE_MIRRORING, 0x00)

        verify(cartridge.chr).mirroring = FIXED_LOWER
      }
    }

    @Test
    fun `dynamic - fixed-upper`() {
      with(Mapper71(Rom())) {
        cartridge.onPrgSet(BASE_MIRRORING, 0x10)

        verify(cartridge.chr).mirroring = FIXED_UPPER
      }
    }
  }
}
