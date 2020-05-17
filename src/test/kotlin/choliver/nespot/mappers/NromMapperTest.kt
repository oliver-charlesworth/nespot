package choliver.nespot.mappers

import choliver.nespot.Address
import choliver.nespot.Memory
import choliver.nespot.cartridge.Rom
import choliver.nespot.cartridge.Rom.Mirroring
import choliver.nespot.cartridge.Rom.Mirroring.HORIZONTAL
import choliver.nespot.cartridge.Rom.Mirroring.VERTICAL
import choliver.nespot.data
import choliver.nespot.mappers.BankMappingChecker.Companion.takesBytes
import choliver.nespot.mappers.NromMapper.Companion.BASE_CHR_ROM
import choliver.nespot.mappers.NromMapper.Companion.BASE_PRG_RAM
import choliver.nespot.mappers.NromMapper.Companion.BASE_PRG_ROM
import choliver.nespot.mappers.NromMapper.Companion.CHR_RAM_SIZE
import choliver.nespot.mappers.NromMapper.Companion.PRG_BANK_SIZE
import choliver.nespot.mappers.NromMapper.Companion.PRG_RAM_SIZE
import com.nhaarman.mockitokotlin2.doReturn
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.verify
import com.nhaarman.mockitokotlin2.whenever
import org.junit.jupiter.api.Assertions.assertEquals
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
      checker.assertMapping(0, 0)
    }
  }

  @Nested
  inner class PrgRom {
    @Test
    fun `size-32768`() {
      val checker = checker(32768)

      checker.assertMapping(0, 0)
      checker.assertMapping(1, 1)
    }

    @Test
    fun `size-16384`() {
      val checker = checker(16384)

      checker.assertMapping(0, 0)
      checker.assertMapping(0, 1)
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
      checker.assertMapping(0, 0)
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
      checker.assertMapping(0, 0)
    }
  }

  @Nested
  inner class Vram {
    @Test
    fun `vertically maps 0x2000 to 0x3EFF to VRAM`() {
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
        0x2FFF to 0x07FF,
        // Nametable 0 (source mirror)
        0x3000 to 0x0000,
        0x33FF to 0x03FF,
        // Nametable 1 (source mirror)
        0x3400 to 0x0400,
        0x37FF to 0x07FF,
        // Nametable 2 (source mirror)
        0x3800 to 0x0000,
        0x3BFF to 0x03FF,
        // Nametable 3 (source mirror)
        0x3C00 to 0x0400,
        0x3EFF to 0x06FF
      )

      cases.forEach { (source, target) -> assertLoadAndStore(VERTICAL, source = source, target = target) }
    }

    @Test
    fun `horizontally maps 0x2000 to 0x3EFF to VRAM`() {
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
        0x2FFF to 0x07FF,
        // Nametable 0 (source mirror)
        0x3000 to 0x0000,
        0x33FF to 0x03FF,
        // Nametable 1 (source mirror)
        0x3400 to 0x0000,
        0x37FF to 0x03FF,
        // Nametable 2 (source mirror)
        0x3800 to 0x0400,
        0x3BFF to 0x07FF,
        // Nametable 3 (source mirror)
        0x3C00 to 0x0400,
        0x3EFF to 0x06FF
      )

      cases.forEach { (source, target) -> assertLoadAndStore(HORIZONTAL, source = source, target = target) }
    }

    private fun assertLoadAndStore(mirroring: Mirroring, source: Address, target: Address) {
      val mapper = mapper(mirroring = mirroring)
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
    prgData: ByteArray = ByteArray(0),
    chrData: ByteArray = ByteArray(0),
    mirroring: Mirroring = VERTICAL
  ) = NromMapper(Rom(
    mirroring = mirroring,
    prgData = prgData,
    chrData = chrData
  ))
}
