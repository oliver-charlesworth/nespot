package choliver.nespot.mappers

import choliver.nespot.Data
import choliver.nespot.cartridge.Rom
import choliver.nespot.cartridge.Rom.Mirroring.*
import choliver.nespot.cartridge.StandardMapper
import choliver.nespot.cartridge.StandardMapper.Config
import choliver.nespot.mappers.Mmc1Mapper.Companion.BASE_SR
import choliver.nespot.mappers.Mmc1Mapper.Companion.CHR_BANK_SIZE
import choliver.nespot.mappers.Mmc1Mapper.Companion.CHR_RAM_SIZE
import choliver.nespot.mappers.Mmc1Mapper.Companion.PRG_BANK_SIZE
import com.nhaarman.mockitokotlin2.*
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertSame
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import org.mockito.Mockito.RETURNS_DEEP_STUBS

class Mmc1MapperTest {
  private val mapper = mock<StandardMapper>(defaultAnswer = RETURNS_DEEP_STUBS)
  private var step = 0

  @Test
  fun `configures CHR ROM if data is non-empty`() {
    val chrData = ByteArray(4096)
    val config = Mmc1Mapper(Rom(chrData = chrData), getStepCount = { step })

    assertSame(chrData, config.chrData)
  }

  @Test
  fun `configures CHR RAM if data is empty`() {
    val config = Mmc1Mapper(Rom(), getStepCount = { step })

    assertEquals(CHR_RAM_SIZE, config.chrData.size)
  }

  @Nested
  inner class Prg {
    private val map = mutableListOf(0, 0)
    private val config = Mmc1Mapper(Rom(prgData = ByteArray(8 * PRG_BANK_SIZE)), getStepCount = { step })

    init {
      whenever(mapper.prg.bankMap.set(any(), any())) doAnswer {
        map[it.getArgument(0)] = it.getArgument(1)
        Unit
      }
    }

    @Test
    fun `sets highest bank on startup`() {
      with(config) {
        mapper.onStartup()
      }

      assertEquals(7, map[1])
    }

    @ParameterizedTest
    @ValueSource(ints = [0, 1])
    fun `32k mode`(mode: Int) {
      setMode(mode)
      setBank(4)

      assertEquals(listOf(4, 5), map)
    }

    @Test
    fun `variable upper`() {
      setMode(2)
      setBank(6)

      assertEquals(listOf(0, 6), map)   // Lower bank fixed to first
    }

    @Test
    fun `variable lower`() {
      setMode(3)
      setBank(5)

      assertEquals(listOf(5, 7), map)   // Upper bank fixed to last
    }

    @Test
    fun `bank mapping wraps`() {
      setMode(0)
      setBank(4 + 8)

      assertEquals(listOf(4, 5), map)
    }

    private fun setMode(mode: Int) {
      config.writeReg(0, mode shl 2)
    }

    private fun setBank(bank: Int) {
      config.writeReg(3, bank)
    }
  }

  @Nested
  inner class Chr {
    private val map = mutableListOf(0, 0)
    private val config = Mmc1Mapper(Rom(chrData = ByteArray(8 * CHR_BANK_SIZE)), getStepCount = { step })

    init {
      whenever(mapper.chr.bankMap.set(any(), any())) doAnswer {
        map[it.getArgument(0)] = it.getArgument(1)
        Unit
      }
    }

    @Test
    fun `8k mode`() {
      // bank1 set to something weird to prove we ignore it
      setModeAndBanks(mode = 0, bank0 = 6, bank1 = 3)

      assertEquals(listOf(6, 7), map)
    }

    @Test
    fun `4k mode`() {
      setModeAndBanks(mode = 1, bank0 = 6, bank1 = 3)

      assertEquals(listOf(6, 3), map)
    }

    @Test
    fun `bank-selection wraps`() {
      setModeAndBanks(mode = 1, bank0 = 6 + 8, bank1 = 3 + 8)

      assertEquals(listOf(6, 3), map)
    }

    private fun setModeAndBanks(mode: Int, bank0: Int, bank1: Int) {
      config.writeReg(0, mode shl 4)
      config.writeReg(1, bank0)
      config.writeReg(2, bank1)
    }
  }

  @Nested
  inner class Mirroring {
    private val config = Mmc1Mapper(Rom(), getStepCount = { step })

    @Test
    fun `fixed-lower`() {
      setMirrorMode(0)

      verify(mapper.chr).mirroring = FIXED_LOWER
    }

    @Test
    fun `fixed-upper`() {
      setMirrorMode(1)

      verify(mapper.chr).mirroring = FIXED_UPPER
    }

    @Test
    fun vertical() {
      setMirrorMode(2)

      verify(mapper.chr).mirroring = VERTICAL
    }

    @Test
    fun horizontal() {
      setMirrorMode(3)

      verify(mapper.chr).mirroring = HORIZONTAL
    }

    private fun setMirrorMode(mode: Int) {
      config.writeReg(0, mode)
    }
  }

  private fun Config.writeReg(idx: Int, data: Data) {
    with(this) {
      val d = data and 0x1F
      val addr = BASE_SR or ((idx and 0x03) shl 13)
      mapper.onPrgSet(addr, 0x80)   // Reset
      step++
      mapper.onPrgSet(addr, d shr 0)
      step++
      mapper.onPrgSet(addr, d shr 1)
      step++
      mapper.onPrgSet(addr, d shr 2)
      step++
      mapper.onPrgSet(addr, d shr 3)
      step++
      mapper.onPrgSet(addr, d shr 4)
      step++
    }
  }
}
