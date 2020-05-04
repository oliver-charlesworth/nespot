package choliver.nespot.cartridge.mappers

import choliver.nespot.Address
import choliver.nespot.Data
import choliver.nespot.Memory
import choliver.nespot.cartridge.Rom
import choliver.nespot.cartridge.mappers.Mmc3Mapper.Companion.BASE_PRG_ROM
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
    fun `bank 0 variable`() {
      setModeAndReg(mode = 0, reg = 6, data = 3)

      assertBankMapping(romBank = 3, prgBank = 0)
    }

    @Test
    fun `bank 0 fixed to penultimate`() {
      setMode(mode = 1)

      assertBankMapping(romBank = 6, prgBank = 0)
    }

    @ParameterizedTest
    @ValueSource(ints = [0, 1])
    fun `bank 1 always variable`(mode: Int) {
      setModeAndReg(mode = mode, reg = 7, data = 3)

      assertBankMapping(romBank = 3, prgBank = 1)
    }

    @Test
    fun `bank 2 fixed to penultimate`() {
      setMode(mode = 0)

      assertBankMapping(romBank = 6, prgBank = 2)
    }

    @Test
    fun `bank 2 variable`() {
      setModeAndReg(mode = 1, reg = 6, data = 3)

      assertBankMapping(romBank = 3, prgBank = 2)
    }

    @ParameterizedTest
    @ValueSource(ints = [0, 1])
    fun `bank 3 always fixed to last`(mode: Int) {
      setMode(mode = mode)

      assertBankMapping(romBank = 7, prgBank = 3)
    }

    private fun setData(data: Map<Address, Data>) {
      data.forEach { (addr, data) -> prgData[addr] = data.toByte() }
    }

    private fun setMode(mode: Int) {
      mapper.prg[0x8000] = (mode shl 6)
    }

    private fun setModeAndReg(mode: Int, reg: Int, data: Data) {
      mapper.prg[0x8000] = (mode shl 6) or reg
      mapper.prg[0x8001] = data
    }

    // Test top and bottom of bank
    private fun assertBankMapping(romBank: Int, prgBank: Int) {
      val romBase = romBank * PRG_BANK_SIZE
      val prgBase = BASE_PRG_ROM + (prgBank * PRG_BANK_SIZE)
      val offsetLast = PRG_BANK_SIZE - 1

      setData(mapOf(
        romBase to 0x30,
        romBase + offsetLast to 0x40
      ))

      assertEquals(0x30, mapper.prg[prgBase])
      assertEquals(0x40, mapper.prg[prgBase + offsetLast])
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

  // TODO - chr modes (all banks * 2)

  // TODO - irq asserted when countdown complete

  // TODO - irq cleared (statefully) by disable/enable loop

  // TODO - doesn't need to be enabled during the whole countdown

  // TODO - irq not asserted if enable low when counter reaches zero

  // TODO - reloads new value when reaches zero

  // TODO - reloads new value immediately if reload flag set
}
