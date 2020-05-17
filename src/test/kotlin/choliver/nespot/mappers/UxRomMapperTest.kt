package choliver.nespot.mappers

import choliver.nespot.Address
import choliver.nespot.Memory
import choliver.nespot.cartridge.Rom
import choliver.nespot.cartridge.Rom.Mirroring
import choliver.nespot.cartridge.Rom.Mirroring.HORIZONTAL
import choliver.nespot.cartridge.Rom.Mirroring.VERTICAL
import choliver.nespot.data
import choliver.nespot.mappers.BankMappingChecker.Companion.takesBytes
import choliver.nespot.mappers.UxRomMapper.Companion.BASE_BANK_SELECT
import choliver.nespot.mappers.UxRomMapper.Companion.BASE_CHR_ROM
import choliver.nespot.mappers.UxRomMapper.Companion.BASE_PRG_ROM
import choliver.nespot.mappers.UxRomMapper.Companion.CHR_RAM_SIZE
import choliver.nespot.mappers.UxRomMapper.Companion.PRG_BANK_SIZE
import com.nhaarman.mockitokotlin2.doReturn
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.verify
import com.nhaarman.mockitokotlin2.whenever
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test


class UxRomMapperTest {
  @Nested
  inner class PrgRom {
    private val prgData = ByteArray(8 * 16384)
    private val mapper = mapper(prgData = prgData)
    private val checker = BankMappingChecker(
      bankSize = PRG_BANK_SIZE,
      outBase = BASE_PRG_ROM,
      setSrc = takesBytes(prgData::set),
      getOut = mapper.prg::get
    )

    @Test
    fun `fixed upper`() {
      setBank(6)

      checker.assertMapping(srcBank = 7, outBank = 1)
    }

    @Test
    fun `variable lower`() {
      setBank(6)

      checker.assertMapping(srcBank = 6, outBank = 0)
    }

    @Test
    fun `bank mapping wraps`() {
      setBank(6 + 8)

      checker.assertMapping(srcBank = 6, outBank = 0)
    }

    @Test
    fun `starts up on min bank`() {
      checker.assertMapping(srcBank = 0, outBank = 0)
    }

    private fun setBank(bank: Int) {
      mapper.prg[BASE_BANK_SELECT] = bank
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
      checker.assertMapping(0, 0)
    }
  }

  @Nested
  inner class Vram {
    @Test
    fun `vertical mirroring`() {
      val cases = mapOf(
        // Nametable 0
        0x2000 to 0x0000,
        0x23FF to 0x03FF,
        // Nametable 1
        0x2400 to 0x0400,
        0x27FF to 0x07FF,
        // Nametable 2
        0x2800 to 0x0000,
        0x2BFF to 0x03FF,
        // Nametable 3
        0x2C00 to 0x0400,
        0x2FFF to 0x07FF
      )

      cases.forEach { (source, target) -> assertLoadAndStore(VERTICAL, source = source, target = target) }
    }

    @Test
    fun `horizontal mirroring`() {
      val cases = mapOf(
        // Nametable 0
        0x2000 to 0x0000,
        0x23FF to 0x03FF,
        // Nametable 1
        0x2400 to 0x0000,
        0x27FF to 0x03FF,
        // Nametable 2
        0x2800 to 0x0400,
        0x2BFF to 0x07FF,
        // Nametable 3
        0x2C00 to 0x0400,
        0x2FFF to 0x07FF
      )

      cases.forEach { (source, target) -> assertLoadAndStore(HORIZONTAL, source = source, target = target) }
    }

    private fun assertLoadAndStore(mirroring: Mirroring, source: Address, target: Address) {
      val mapper = mapper(mirroring = mirroring, chrData = ByteArray(8192))
      val vram = mock<Memory>()
      val chr = mapper.chr(vram)

      val data = (target + 23).data() // Arbitrary payload
      whenever(vram[target]) doReturn data

      assertEquals(data, chr[source])

      chr[source] = data
      verify(vram)[target] = data
    }
  }

  private fun mapper(
    prgData: ByteArray = ByteArray(32768),
    chrData: ByteArray = ByteArray(8192),
    mirroring: Mirroring = VERTICAL
  ) = UxRomMapper(Rom(
    mirroring = mirroring,
    prgData = prgData,
    chrData = chrData
  ))
}
