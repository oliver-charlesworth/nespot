package choliver.nespot.cartridge.mappers

import choliver.nespot.Address
import choliver.nespot.Data
import choliver.nespot.Memory
import choliver.nespot.cartridge.Mapper
import choliver.nespot.cartridge.Rom
import choliver.nespot.cartridge.mappers.Mmc3Mapper.Companion.BASE_PRG_ROM
import choliver.nespot.cartridge.mappers.Mmc3Mapper.Companion.CHR_BANK_SIZE
import choliver.nespot.cartridge.mappers.Mmc3Mapper.Companion.PRG_BANK_SIZE
import choliver.nespot.data
import com.nhaarman.mockitokotlin2.doReturn
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.verify
import com.nhaarman.mockitokotlin2.whenever
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource


class Mmc3MapperTest {
  @Nested
  inner class PrgRam {
    private val mapper = Mmc3Mapper(Rom())

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
    private val mapper = Mmc3Mapper(Rom(prgData = prgData))

    @Test
    fun `mode 0`() {
      mapper.setModeAndReg(prgMode = 0, reg = 6, data = 3)
      mapper.setModeAndReg(prgMode = 0, reg = 7, data = 5)

      assertBankMapping(romBank = 3, prgBank = 0)
      assertBankMapping(romBank = 5, prgBank = 1)
      assertBankMapping(romBank = 6, prgBank = 2)   // Fixed to penultimate
      assertBankMapping(romBank = 7, prgBank = 3)   // Fixed to last
    }

    @Test
    fun `mode 1`() {
      mapper.setModeAndReg(prgMode = 1, reg = 6, data = 3)
      mapper.setModeAndReg(prgMode = 1, reg = 7, data = 5)

      assertBankMapping(romBank = 6, prgBank = 0)   // Fixed to penultimate
      assertBankMapping(romBank = 5, prgBank = 1)
      assertBankMapping(romBank = 3, prgBank = 2)
      assertBankMapping(romBank = 7, prgBank = 3)   // Fixed to last
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
  inner class ChrRom {
    private val chrData = ByteArray(8 * CHR_BANK_SIZE)
    private val mapper = Mmc3Mapper(Rom(chrData = chrData))

    @Test
    fun `low banks are 2k, high banks are 1k`() {
      mapper.setModeAndReg(chrMode = 0, reg = 0, data = 5) // Note we ignore LSB of selected bank
      mapper.setModeAndReg(chrMode = 0, reg = 1, data = 0) // Note we ignore LSB of selected bank
      mapper.setModeAndReg(chrMode = 0, reg = 2, data = 3)
      mapper.setModeAndReg(chrMode = 0, reg = 3, data = 6)
      mapper.setModeAndReg(chrMode = 0, reg = 4, data = 2)
      mapper.setModeAndReg(chrMode = 0, reg = 5, data = 1)

      assertBankMapping(romBank = 4, chrBank = 0)
      assertBankMapping(romBank = 5, chrBank = 1)
      assertBankMapping(romBank = 0, chrBank = 2)
      assertBankMapping(romBank = 1, chrBank = 3)
      assertBankMapping(romBank = 3, chrBank = 4)
      assertBankMapping(romBank = 6, chrBank = 5)
      assertBankMapping(romBank = 2, chrBank = 6)
      assertBankMapping(romBank = 1, chrBank = 7)
    }

    @Test
    fun `low banks are 1k, high banks are 2k`() {
      mapper.setModeAndReg(chrMode = 1, reg = 0, data = 5) // Note we ignore LSB of selected bank
      mapper.setModeAndReg(chrMode = 1, reg = 1, data = 0) // Note we ignore LSB of selected bank
      mapper.setModeAndReg(chrMode = 1, reg = 2, data = 3)
      mapper.setModeAndReg(chrMode = 1, reg = 3, data = 6)
      mapper.setModeAndReg(chrMode = 1, reg = 4, data = 2)
      mapper.setModeAndReg(chrMode = 1, reg = 5, data = 1)

      assertBankMapping(romBank = 3, chrBank = 0)
      assertBankMapping(romBank = 6, chrBank = 1)
      assertBankMapping(romBank = 2, chrBank = 2)
      assertBankMapping(romBank = 1, chrBank = 3)
      assertBankMapping(romBank = 4, chrBank = 4)
      assertBankMapping(romBank = 5, chrBank = 5)
      assertBankMapping(romBank = 0, chrBank = 6)
      assertBankMapping(romBank = 1, chrBank = 7)
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

      cases.forEach { (source, target) -> assertLoadAndStore(mode = 0, source = source, target = target) }
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

      cases.forEach { (source, target) -> assertLoadAndStore(mode = 1, source = source, target = target) }
    }

    private fun assertLoadAndStore(mode: Int, source: Address, target: Address) {
      val mapper = Mmc3Mapper(Rom())
      val vram = mock<Memory>()
      val chr = mapper.chr(vram)
      mapper.prg[0xA000] = mode

      val data = (target + 23).data() // Arbitrary payload
      whenever(vram[target]) doReturn data

      assertEquals(data, chr[source])

      chr[source] = data
      verify(vram)[target] = data
    }
  }

  // TODO - irq asserted when countdown complete

  // TODO - irq cleared (statefully) by disable/enable loop

  // TODO - doesn't need to be enabled during the whole countdown

  // TODO - irq not asserted if enable low when counter reaches zero

  // TODO - reloads new value when reaches zero

  // TODO - reloads new value immediately if reload flag set


  private fun Mapper.setModeAndReg(chrMode: Int = 0, prgMode: Int = 0, reg: Int, data: Data) {
    prg[0x8000] = (chrMode shl 7) or (prgMode shl 6) or reg
    prg[0x8001] = data
  }
}
