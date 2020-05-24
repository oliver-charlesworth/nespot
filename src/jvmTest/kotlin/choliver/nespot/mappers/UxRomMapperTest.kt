package choliver.nespot.mappers

import choliver.nespot.BASE_CHR_ROM
import choliver.nespot.BASE_PRG_ROM
import choliver.nespot.cartridge.Rom
import choliver.nespot.cartridge.Rom.Mirroring.HORIZONTAL
import choliver.nespot.cartridge.Rom.Mirroring.VERTICAL
import choliver.nespot.mappers.BankMappingChecker.Companion.takesBytes
import choliver.nespot.mappers.UxRomMapper.Companion.BASE_BANK_SELECT
import choliver.nespot.mappers.UxRomMapper.Companion.CHR_RAM_SIZE
import choliver.nespot.mappers.UxRomMapper.Companion.PRG_BANK_SIZE
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test


class UxRomMapperTest {
  @Nested
  inner class PrgRom {
    private val prgData = ByteArray(8 * 16384)
    private val mapper = UxRomMapper(Rom(prgData = prgData))
    private val checker = BankMappingChecker(
      bankSize = PRG_BANK_SIZE,
      outBase = BASE_PRG_ROM,
      setSrc = takesBytes(prgData::set),
      getOut = mapper.prg::get
    )

    @Test
    fun `fixed upper`() {
      setBank(6)

      checker.assertMappings(7 to 1)
    }

    @Test
    fun `variable lower`() {
      setBank(6)

      checker.assertMappings(6 to 0)
    }

    @Test
    fun `bank mapping wraps`() {
      setBank(6 + 8)

      checker.assertMappings(6 to 0)
    }

    @Test
    fun `starts up on min bank`() {
      checker.assertMappings(0 to 0)
    }

    private fun setBank(bank: Int) {
      mapper.prg[BASE_BANK_SELECT] = bank
    }
  }

  @Nested
  inner class ChrRam {
    private val mapper = UxRomMapper(Rom())
    private val checker = BankMappingChecker(
      bankSize = CHR_RAM_SIZE,
      srcBase = BASE_CHR_ROM,
      outBase = BASE_CHR_ROM,
      setSrc = mapper.chr::set,
      getOut = mapper.chr::get
    )

    @Test
    fun `load and store`() {
      checker.assertMappings(0 to 0)
    }
  }

  @Nested
  inner class Vram {
    @Test
    fun `vertical mirroring`() {
      assertVramMappings(UxRomMapper(Rom(mirroring = VERTICAL)), listOf(0, 2), listOf(1, 3))
    }

    @Test
    fun `horizontal mirroring`() {
      assertVramMappings(UxRomMapper(Rom(mirroring = HORIZONTAL)), listOf(0, 1), listOf(2, 3))
    }
  }
}
