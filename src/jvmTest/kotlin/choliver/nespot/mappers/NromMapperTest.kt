package choliver.nespot.mappers

import choliver.nespot.cartridge.Rom
import choliver.nespot.cartridge.Rom.Mirroring.VERTICAL
import choliver.nespot.cartridge.Cartridge
import choliver.nespot.mappers.NromMapper.Companion.CHR_RAM_SIZE
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.verify
import com.nhaarman.mockitokotlin2.verifyNoMoreInteractions
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertSame
import org.junit.jupiter.api.Test
import org.mockito.Answers.RETURNS_DEEP_STUBS

class NromMapperTest {
  private val cartridge = mock<Cartridge>(defaultAnswer = RETURNS_DEEP_STUBS)

  @Test
  fun `configures CHR ROM if data is non-empty`() {
    val chrData = ByteArray(4096)
    val mapper = NromMapper(Rom(chrData = chrData))

    assertSame(chrData, mapper.chrData)
  }

  @Test
  fun `configures CHR RAM if data is empty`() {
    val mapper = NromMapper(Rom())

    assertEquals(CHR_RAM_SIZE, mapper.chrData.size)
  }

  @Test
  fun `maps both PRG banks to 0 for size-16384`() {
    with(NromMapper(Rom(prgData = ByteArray(16384)))) {
      cartridge.onStartup()
    }

    verify(cartridge.prg.bankMap)[1] = 0
    verifyNoMoreInteractions(cartridge.prg.bankMap)
  }

  @Test
  fun `maps both PRG banks to 1 for size-32768`() {
    with(NromMapper(Rom(prgData = ByteArray(32768)))) {
      cartridge.onStartup()
    }

    verify(cartridge.prg.bankMap)[1] = 1
    verifyNoMoreInteractions(cartridge.prg.bankMap)
  }

  @Test
  fun `sets mirroring on startup`() {
    with(NromMapper(Rom(mirroring = VERTICAL))) {
      cartridge.onStartup()
    }

    verify(cartridge.chr).mirroring = VERTICAL
  }
}
