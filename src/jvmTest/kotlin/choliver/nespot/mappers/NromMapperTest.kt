package choliver.nespot.mappers

import choliver.nespot.BASE_CHR_ROM
import choliver.nespot.BASE_PRG_RAM
import choliver.nespot.BASE_PRG_ROM
import choliver.nespot.PRG_RAM_SIZE
import choliver.nespot.cartridge.Rom
import choliver.nespot.cartridge.Rom.Mirroring.HORIZONTAL
import choliver.nespot.cartridge.Rom.Mirroring.VERTICAL
import choliver.nespot.mappers.BankMappingChecker.Companion.takesBytes
import choliver.nespot.mappers.NromMapper.Companion.CHR_RAM_SIZE
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test


class NromMapperTest {
  @Nested
  inner class PrgRam {
    private val mapper = NromMapper(Rom())
    private val checker = BankMappingChecker(
      bankSize = PRG_RAM_SIZE,
      srcBase = BASE_PRG_RAM,
      outBase = BASE_PRG_RAM,
      setSrc = mapper.prg::set,
      getOut = mapper.prg::get
    )

    @Test
    fun `load and store`() {
      checker.assertMappings(0 to 0)
    }
  }

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
      val mapper = NromMapper(Rom(prgData = prgData))
      return BankMappingChecker(
        bankSize = 16384,
        outBase = BASE_PRG_ROM,
        setSrc = takesBytes(prgData::set),
        getOut = mapper.prg::get
      )
    }
  }

  @Nested
  inner class ChrRam {
    private val mapper = NromMapper(Rom())
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
  inner class ChrRom {
    private val chrData = ByteArray(8192)
    private val mapper = NromMapper(Rom(chrData = chrData))
    private val checker = BankMappingChecker(
      bankSize = CHR_RAM_SIZE,
      srcBase = BASE_CHR_ROM,
      outBase = BASE_CHR_ROM,
      setSrc = takesBytes(chrData::set),
      getOut = mapper.chr::get
    )

    @Test
    fun `maps 0x0000 to 0x1FFF`() {
      checker.assertMappings(0 to 0)
    }
  }

  @Nested
  inner class Vram {
    @Test
    fun `vertical mirroring`() {
      assertVramMappings(NromMapper(Rom(mirroring = VERTICAL)), listOf(0, 2), listOf(1, 3))
    }

    @Test
    fun `horizontal mirroring`() {
      assertVramMappings(NromMapper(Rom(mirroring = HORIZONTAL)), listOf(0, 1), listOf(2, 3))
    }
  }
}
