package choliver.nespot.mappers

import choliver.nespot.cartridge.Cartridge
import choliver.nespot.cartridge.Rom
import choliver.nespot.cartridge.Rom.Mirroring.FIXED_LOWER
import choliver.nespot.cartridge.Rom.Mirroring.FIXED_UPPER
import choliver.nespot.mappers.AxRomMapper.Companion.BASE_BANK_SELECT
import choliver.nespot.mappers.AxRomMapper.Companion.CHR_RAM_SIZE
import choliver.nespot.mappers.AxRomMapper.Companion.PRG_BANK_SIZE
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.verify
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.mockito.Answers.RETURNS_DEEP_STUBS

class AxRomMapperTest {
  private val cartridge = mock<Cartridge>(defaultAnswer = RETURNS_DEEP_STUBS)
  private val mapper = AxRomMapper(Rom(prgData = ByteArray(4 * PRG_BANK_SIZE)))

  @Test
  fun `configures CHR RAM`() {
    assertEquals(CHR_RAM_SIZE, mapper.chrData.size)
  }

  @Nested
  inner class Prg {
    @Test
    fun `sets bank`() {
      with(mapper) {
        cartridge.onPrgSet(BASE_BANK_SELECT, 2)

        verify(cartridge.prg.bankMap)[0] = 2
      }
    }

    @Test
    fun `bank mapping wraps`() {
      with(mapper) {
        cartridge.onPrgSet(BASE_BANK_SELECT, 2 + 4)

        verify(cartridge.prg.bankMap)[0] = 2
      }
    }
  }

  @Nested
  inner class Mirroring {
    @Test
    fun `dynamic - fixed-lower`() {
      with(mapper) {
        cartridge.onPrgSet(BASE_BANK_SELECT, 0x00)

        verify(cartridge.chr).mirroring = FIXED_LOWER
      }
    }

    @Test
    fun `dynamic - fixed-upper`() {
      with(mapper) {
        cartridge.onPrgSet(BASE_BANK_SELECT, 0x10)

        verify(cartridge.chr).mirroring = FIXED_UPPER
      }
    }
  }
}
