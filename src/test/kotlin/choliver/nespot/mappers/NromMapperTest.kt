package choliver.nespot.mappers

import choliver.nespot.cartridge.Rom
import choliver.nespot.cartridge.Rom.Mirroring
import choliver.nespot.cartridge.Rom.Mirroring.HORIZONTAL
import choliver.nespot.cartridge.Rom.Mirroring.VERTICAL
import choliver.nespot.mappers.BankMappingChecker.Companion.takesBytes
import choliver.nespot.mappers.NromMapper.Companion.BASE_CHR_ROM
import choliver.nespot.mappers.NromMapper.Companion.BASE_PRG_RAM
import choliver.nespot.mappers.NromMapper.Companion.BASE_PRG_ROM
import choliver.nespot.mappers.NromMapper.Companion.CHR_RAM_SIZE
import choliver.nespot.mappers.NromMapper.Companion.PRG_BANK_SIZE
import choliver.nespot.mappers.NromMapper.Companion.PRG_RAM_SIZE
import com.nhaarman.mockitokotlin2.mock
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test


class NromMapperTest {
  @Nested
  inner class PrgRam {
    private val mapper = mapper()
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
      val mapper = mapper(prgData = prgData)
      return BankMappingChecker(
        bankSize = PRG_BANK_SIZE,
        outBase = BASE_PRG_ROM,
        setSrc = takesBytes(prgData::set),
        getOut = mapper.prg::get
      )
    }
  }

  @Nested
  inner class ChrRam {
    private val mapper = mapper()
    private val chr = mapper.chr(mock())
    private val checker = BankMappingChecker(
      bankSize = CHR_RAM_SIZE,
      srcBase = BASE_CHR_ROM,
      outBase = BASE_CHR_ROM,
      setSrc = chr::set,
      getOut = chr::get
    )

    @Test
    fun `load and store`() {
      checker.assertMappings(0 to 0)
    }
  }

  @Nested
  inner class ChrRom {
    private val chrData = ByteArray(8192)
    private val mapper = mapper(chrData = chrData)
    private val checker = BankMappingChecker(
      bankSize = CHR_RAM_SIZE,
      srcBase = BASE_CHR_ROM,
      outBase = BASE_CHR_ROM,
      setSrc = takesBytes(chrData::set),
      getOut = mapper.chr(mock())::get
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
      assertVramMappings(mapper(mirroring = VERTICAL), 0 to 0, 1 to 1, 0 to 2, 1 to 3)
    }

    @Test
    fun `horizontal mirroring`() {
      assertVramMappings(mapper(mirroring = HORIZONTAL), 0 to 0, 0 to 1, 1 to 2, 1 to 3)
    }
  }

  private fun mapper(
    prgData: ByteArray = ByteArray(0),
    chrData: ByteArray = ByteArray(0),
    mirroring: Mirroring = VERTICAL
  ) = NromMapper(Rom(
    mirroring = mirroring,
    prgData = prgData,
    chrData = chrData
  ))
}
