package choliver.nespot.mappers

import choliver.nespot.cartridge.Rom
import choliver.nespot.cartridge.Rom.Mirroring.VERTICAL
import choliver.nespot.cartridge.StandardMapper
import choliver.nespot.mappers.NromMapperConfig.Companion.CHR_RAM_SIZE
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.verify
import com.nhaarman.mockitokotlin2.verifyNoMoreInteractions
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertSame
import org.junit.jupiter.api.Test
import org.mockito.Answers.RETURNS_DEEP_STUBS

class NromMapperConfigTest {
  private val mapper = mock<StandardMapper>(defaultAnswer = RETURNS_DEEP_STUBS)

  @Test
  fun `configures CHR ROM if data is non-empty`() {
    val chrData = ByteArray(4096)
    val config = NromMapperConfig(Rom(chrData = chrData))

    assertSame(chrData, config.chrData)
  }

  @Test
  fun `configures CHR RAM if data is empty`() {
    val config = NromMapperConfig(Rom())

    assertEquals(CHR_RAM_SIZE, config.chrData.size)
  }

  @Test
  fun `maps both PRG banks to 0 for size-16384`() {
    with(NromMapperConfig(Rom(prgData = ByteArray(16384)))) {
      mapper.onStartup()
    }

    verify(mapper.prg.bankMap)[1] = 0
    verifyNoMoreInteractions(mapper.prg.bankMap)
  }

  @Test
  fun `maps both PRG banks to 1 for size-32768`() {
    with(NromMapperConfig(Rom(prgData = ByteArray(32768)))) {
      mapper.onStartup()
    }

    verify(mapper.prg.bankMap)[1] = 1
    verifyNoMoreInteractions(mapper.prg.bankMap)
  }

  @Test
  fun `sets mirroring on startup`() {
    with(NromMapperConfig(Rom(mirroring = VERTICAL))) {
      mapper.onStartup()
    }

    verify(mapper.chr).mirroring = VERTICAL
  }
}
