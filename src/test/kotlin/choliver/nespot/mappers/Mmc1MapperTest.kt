package choliver.nespot.mappers

import choliver.nespot.Address
import choliver.nespot.Data
import choliver.nespot.Memory
import choliver.nespot.cartridge.Rom
import choliver.nespot.data
import choliver.nespot.mappers.Mmc1Mapper.Companion.BASE_PRG_ROM
import choliver.nespot.mappers.Mmc1Mapper.Companion.BASE_SR
import choliver.nespot.mappers.Mmc1Mapper.Companion.CHR_BANK_SIZE
import choliver.nespot.mappers.Mmc1Mapper.Companion.PRG_BANK_SIZE
import com.nhaarman.mockitokotlin2.doReturn
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.verify
import com.nhaarman.mockitokotlin2.whenever
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource


class Mmc1MapperTest {
  private var step = 0

  @Nested
  inner class PrgRam {
    private val mapper = Mmc1Mapper(Rom(), getStepCount = { 0 })

    @Test
    fun `load and store`() {
      mapper.prg[0x6000] = 0x30 // Lowest mapped address
      mapper.prg[0x7FFF] = 0x40 // Highest mapped address

      assertEquals(0x30, mapper.prg[0x6000])
      assertEquals(0x40, mapper.prg[0x7FFF])
    }
  }

  @Nested
  inner class PrgRom {
    private val prgData = ByteArray(8 * PRG_BANK_SIZE)
    private val mapper = Mmc1Mapper(Rom(prgData = prgData), getStepCount = { step })

    @ParameterizedTest
    @ValueSource(ints = [0, 1])
    fun `32k mode`(mode: Int) {
      setMode(mode)
      setBank(6)

      assertBankMapping(romBank = 6, prgBank = 0)
      assertBankMapping(romBank = 7, prgBank = 1)
    }

    @Test
    fun `variable upper`() {
      setMode(2)
      setBank(6)

      assertBankMapping(romBank = 0, prgBank = 0)   // Fixed to first
      assertBankMapping(romBank = 6, prgBank = 1)
    }

    @Test
    fun `variable lower`() {
      setMode(3)
      setBank(5)

      assertBankMapping(romBank = 5, prgBank = 0)
      assertBankMapping(romBank = 7, prgBank = 1)   // Fixed to last
    }

    @Test
    fun `bank mapping wraps`() {
      setMode(0)
      setBank(6 + 8)

      assertBankMapping(romBank = 6, prgBank = 0)
      assertBankMapping(romBank = 7, prgBank = 1)
    }

    @Test
    fun `starts up on max bank`() {
      setMode(3)

      assertBankMapping(romBank = 7, prgBank = 0)
    }

    private fun setMode(mode: Int) {
      mapper.writeReg(0, mode shl 2)
    }

    private fun setBank(bank: Int) {
      mapper.writeReg(3, bank)
    }

    // Test top and bottom of bank
    private fun assertBankMapping(romBank: Int, prgBank: Int) {
      val romBase = romBank * PRG_BANK_SIZE
      val prgBase = BASE_PRG_ROM + (prgBank * PRG_BANK_SIZE)
      val offsetLast = PRG_BANK_SIZE - 1

      prgData[romBase] = 0x30
      prgData[romBase + offsetLast] = 0x40

      assertEquals(0x30, mapper.prg[prgBase])
      assertEquals(0x40, mapper.prg[prgBase + offsetLast])
    }
  }

  @Nested
  inner class ChrRam {
    private val mapper = Mmc1Mapper(Rom(), getStepCount = { 0 })

    @Test
    fun `load and store`() {
      val chr = mapper.chr(mock())

      chr[0x0000] = 0x30 // Lowest mapped address
      chr[0x1FFF] = 0x40 // Highest mapped address

      assertEquals(0x30, chr[0x0000])
      assertEquals(0x40, chr[0x1FFF])
    }
  }

  @Nested
  inner class ChrRom {
    private val chrData = ByteArray(8 * CHR_BANK_SIZE)
    private val mapper = Mmc1Mapper(Rom(chrData = chrData), getStepCount = { step })

    @Test
    fun `8k mode`() {
      // bank1 set to something weird to prove we ignore it
      setModeAndBanks(mode = 0, bank0 = 6, bank1 = 3)

      assertBankMapping(romBank = 6, chrBank = 0)
      assertBankMapping(romBank = 7, chrBank = 1)
    }

    @Test
    fun `4k mode`() {
      setModeAndBanks(mode = 1, bank0 = 6, bank1 = 3)

      assertBankMapping(romBank = 6, chrBank = 0)
      assertBankMapping(romBank = 3, chrBank = 1)
    }

    @Test
    fun `bank-selection wraps`() {
      setModeAndBanks(mode = 1, bank0 = 6 + 8, bank1 = 3 + 8)

      assertBankMapping(romBank = 6, chrBank = 0)
      assertBankMapping(romBank = 3, chrBank = 1)
    }

    private fun setModeAndBanks(mode: Int, bank0: Int, bank1: Int) {
      mapper.writeReg(0, mode shl 4)
      mapper.writeReg(1, bank0)
      mapper.writeReg(2, bank1)
    }

    // Test top and bottom of bank
    private fun assertBankMapping(romBank: Int, chrBank: Int) {
      val romBase = romBank * CHR_BANK_SIZE
      val chrBase = chrBank * CHR_BANK_SIZE
      val offsetLast = CHR_BANK_SIZE - 1

      chrData[romBase] = 0x30
      chrData[romBase + offsetLast] = 0x40

      val chr = mapper.chr(mock())
      assertEquals(0x30, chr[chrBase])
      assertEquals(0x40, chr[chrBase + offsetLast])
    }
  }

  @Nested
  inner class Vram {
    @Test
    fun `single-screen - nametable 0`() {
      val cases = mapOf(
        // Nametable 0
        0x2000 to 0x0000,
        0x23FF to 0x03FF,
        // Nametable 1
        0x2400 to 0x0000,
        0x27FF to 0x03FF,
        // Nametable 2
        0x2800 to 0x0000,
        0x2BFF to 0x03FF,
        // Nametable 3
        0x2C00 to 0x0000,
        0x2FFF to 0x03FF
      )

      cases.forEach { (source, target) -> assertLoadAndStore(mode = 0, source = source, target = target) }
    }

    @Test
    fun `single-screen - nametable 1`() {
      val cases = mapOf(
        // Nametable 0
        0x2000 to 0x0400,
        0x23FF to 0x07FF,
        // Nametable 1
        0x2400 to 0x0400,
        0x27FF to 0x07FF,
        // Nametable 2
        0x2800 to 0x0400,
        0x2BFF to 0x07FF,
        // Nametable 3
        0x2C00 to 0x0400,
        0x2FFF to 0x07FF
      )

      cases.forEach { (source, target) -> assertLoadAndStore(mode = 1, source = source, target = target) }
    }

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

      cases.forEach { (source, target) -> assertLoadAndStore(mode = 2, source = source, target = target) }
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

      cases.forEach { (source, target) -> assertLoadAndStore(mode = 3, source = source, target = target) }
    }

    private fun assertLoadAndStore(mode: Int, source: Address, target: Address) {
      val mapper = Mmc1Mapper(Rom(chrData = ByteArray(8192)), getStepCount = { step })
      val vram = mock<Memory>()
      val chr = mapper.chr(vram)
      mapper.writeReg(0, mode)

      val data = (target + 23).data() // Arbitrary payload
      whenever(vram[target]) doReturn data

      assertEquals(data, chr[source])

      chr[source] = data
      verify(vram)[target] = data
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
