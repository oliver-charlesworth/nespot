package choliver.nespot.mappers

import choliver.nespot.BASE_CHR_ROM
import choliver.nespot.BASE_PRG_ROM
import choliver.nespot.cartridge.Rom
import choliver.nespot.cartridge.Rom.Mirroring.HORIZONTAL
import choliver.nespot.cartridge.Rom.Mirroring.VERTICAL
import choliver.nespot.mappers.BankMappingChecker.Companion.takesBytes
import choliver.nespot.mappers.CnRomMapper.Companion.CHR_BANK_SIZE
import choliver.nespot.mappers.CnRomMapper.Companion.PRG_BANK_SIZE
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test


class CnRomMapperTest {
  @Nested
  inner class PrgRom {
    @Test
    fun `size-32768`() {
      val checker = checker(32768)

      checker.assertMappings(
        0 to 0,
        1 to 1
      )
    }

    @Test
    fun `size-16384`() {
      val checker = checker(16384)

      checker.assertMappings(
        0 to 0,
        0 to 1
      )
    }

    private fun checker(size: Int): BankMappingChecker {
      val prgData = ByteArray(size)
      val mapper = CnRomMapper(Rom(prgData = prgData))
      return BankMappingChecker(
        bankSize = PRG_BANK_SIZE,
        outBase = BASE_PRG_ROM,
        setSrc = takesBytes(prgData::set),
        getOut = mapper.prg::get
      )
    }
  }

  @Nested
  inner class ChrRom {
    private val chrData = ByteArray(8 * CHR_BANK_SIZE)
    private val mapper = CnRomMapper(Rom(chrData = chrData))
    private val checker = BankMappingChecker(
      bankSize = CHR_BANK_SIZE,
      outBase = BASE_CHR_ROM,
      setSrc = takesBytes(chrData::set),
      getOut = mapper.chr::get
    )

    @Test
    fun variable() {
      setBank(6)

      checker.assertMappings(6 to 0)
    }

    @Test
    fun `bank mapping wraps`() {
      setBank(6 + 8)

      checker.assertMappings(6 to 0)
    }

    private fun setBank(bank: Int) {
      mapper.prg[0x8000] = bank
    }
  }

  @Nested
  inner class Vram {
    @Test
    fun `vertical mirroring`() {
      assertVramMappings(CnRomMapper(Rom(mirroring = VERTICAL)), listOf(0, 2), listOf(1, 3))
    }

    @Test
    fun `horizontal mirroring`() {
      assertVramMappings(CnRomMapper(Rom(mirroring = HORIZONTAL)), listOf(0, 1), listOf(2, 3))
    }
  }
}
