package choliver.nespot.mappers

import choliver.nespot.BASE_CHR_ROM
import choliver.nespot.BASE_PRG_ROM
import choliver.nespot.Data
import choliver.nespot.cartridge.Rom
import choliver.nespot.mappers.BankMappingChecker.Companion.takesBytes
import choliver.nespot.mappers.Mmc1Mapper.Companion.BASE_PRG_RAM
import choliver.nespot.mappers.Mmc1Mapper.Companion.BASE_SR
import choliver.nespot.mappers.Mmc1Mapper.Companion.CHR_BANK_SIZE
import choliver.nespot.mappers.Mmc1Mapper.Companion.PRG_BANK_SIZE
import choliver.nespot.mappers.Mmc1Mapper.Companion.PRG_RAM_SIZE
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource


class Mmc1MapperTest {
  private var step = 0

  @Nested
  inner class PrgRam {
    private val mapper = Mmc1Mapper(Rom(), getStepCount = { 0 })
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
    private val prgData = ByteArray(8 * PRG_BANK_SIZE)
    private val mapper = Mmc1Mapper(Rom(prgData = prgData), getStepCount = { step })
    private val checker = BankMappingChecker(
      bankSize = PRG_BANK_SIZE,
      outBase = BASE_PRG_ROM,
      setSrc = takesBytes(prgData::set),
      getOut = mapper.prg::get
    )

    @ParameterizedTest
    @ValueSource(ints = [0, 1])
    fun `32k mode`(mode: Int) {
      setMode(mode)
      setBank(6)

      checker.assertMappings(
        6 to 0,
        7 to 1
      )
    }

    @Test
    fun `variable upper`() {
      setMode(2)
      setBank(6)

      checker.assertMappings(
        0 to 0,   // Fixed to first
        6 to 1
      )
    }

    @Test
    fun `variable lower`() {
      setMode(3)
      setBank(5)

      checker.assertMappings(
        5 to 0,
        7 to 1    // Fixed to last
      )
    }

    @Test
    fun `bank mapping wraps`() {
      setMode(0)
      setBank(6 + 8)

      checker.assertMappings(
        6 to 0,
        7 to 1
      )
    }

    @Test
    fun `starts up on max bank`() {
      setMode(3)

      checker.assertMappings(7 to 0)
    }

    private fun setMode(mode: Int) {
      mapper.writeReg(0, mode shl 2)
    }

    private fun setBank(bank: Int) {
      mapper.writeReg(3, bank)
    }
  }

  @Nested
  inner class ChrRam {
    private val mapper = Mmc1Mapper(Rom(), getStepCount = { 0 })
    private val checker = BankMappingChecker(
      bankSize = CHR_BANK_SIZE,
      srcBase = BASE_CHR_ROM,
      outBase = BASE_CHR_ROM,
      setSrc = mapper.chr::set,
      getOut = mapper.chr::get
    )

    @Test
    fun `load and store`() {
      checker.assertMappings(
        0 to 0,
        1 to 1
      )
    }

    // TODO - other modes
  }

  @Nested
  inner class ChrRom {
    private val chrData = ByteArray(8 * CHR_BANK_SIZE)
    private val mapper = Mmc1Mapper(Rom(chrData = chrData), getStepCount = { step })
    private val checker = BankMappingChecker(
      bankSize = CHR_BANK_SIZE,
      outBase = BASE_CHR_ROM,
      setSrc = takesBytes(chrData::set),
      getOut = mapper.chr::get
    )

    @Test
    fun `8k mode`() {
      // bank1 set to something weird to prove we ignore it
      setModeAndBanks(mode = 0, bank0 = 6, bank1 = 3)

      checker.assertMappings(
        6 to 0,
        7 to 1
      )
    }

    @Test
    fun `4k mode`() {
      setModeAndBanks(mode = 1, bank0 = 6, bank1 = 3)

      checker.assertMappings(
        6 to 0,
        3 to 1
      )
    }

    @Test
    fun `bank-selection wraps`() {
      setModeAndBanks(mode = 1, bank0 = 6 + 8, bank1 = 3 + 8)

      checker.assertMappings(
        6 to 0,
        3 to 1
      )
    }

    private fun setModeAndBanks(mode: Int, bank0: Int, bank1: Int) {
      mapper.writeReg(0, mode shl 4)
      mapper.writeReg(1, bank0)
      mapper.writeReg(2, bank1)
    }
  }

  @Nested
  inner class Vram {
    private val mapper = Mmc1Mapper(Rom(), getStepCount = { step })

    @Test
    fun `single-screen - nametable 0`() {
      setMirrorMode(0)

      assertVramMappings(mapper, listOf(0, 1, 2, 3))
    }

    // TODO - distinguish from nametable 0 - only possible if we change mode partway through
    @Test
    fun `single-screen - nametable 1`() {
      setMirrorMode(1)

      assertVramMappings(mapper, listOf(0, 1, 2, 3))
    }

    @Test
    fun `vertical mirroring`() {
      setMirrorMode(2)

      assertVramMappings(mapper, listOf(0, 2), listOf(1, 3))
    }

    @Test
    fun `horizontal mirroring`() {
      setMirrorMode(3)

      assertVramMappings(mapper, listOf(0, 1), listOf(2, 3))
    }

    private fun setMirrorMode(mode: Int) {
      mapper.writeReg(0, mode)
    }
  }

  private fun Mmc1Mapper.writeReg(idx: Int, data: Data) {
    val d = data and 0x1F
    val addr = BASE_SR or ((idx and 0x03) shl 13)
    prg[addr] = 0x80   // Reset
    step++
    prg[addr] = d shr 0
    step++
    prg[addr] = d shr 1
    step++
    prg[addr] = d shr 2
    step++
    prg[addr] = d shr 3
    step++
    prg[addr] = d shr 4
    step++
  }
}
