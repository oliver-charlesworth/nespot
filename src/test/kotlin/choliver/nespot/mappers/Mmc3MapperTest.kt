package choliver.nespot.mappers

import choliver.nespot.Address
import choliver.nespot.Data
import choliver.nespot.Memory
import choliver.nespot.apu.repeat
import choliver.nespot.cartridge.Mapper
import choliver.nespot.cartridge.Rom
import choliver.nespot.cpu.utils._0
import choliver.nespot.cpu.utils._1
import choliver.nespot.data
import choliver.nespot.mappers.BankMappingChecker.Companion.takesBytes
import choliver.nespot.mappers.Mmc3Mapper.Companion.BASE_CHR_ROM
import choliver.nespot.mappers.Mmc3Mapper.Companion.BASE_PRG_RAM
import choliver.nespot.mappers.Mmc3Mapper.Companion.BASE_PRG_ROM
import choliver.nespot.mappers.Mmc3Mapper.Companion.CHR_BANK_SIZE
import choliver.nespot.mappers.Mmc3Mapper.Companion.PRG_BANK_SIZE
import choliver.nespot.mappers.Mmc3Mapper.Companion.PRG_RAM_SIZE
import com.nhaarman.mockitokotlin2.doReturn
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.verify
import com.nhaarman.mockitokotlin2.whenever
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test


class Mmc3MapperTest {
  @Nested
  inner class PrgRam {
    private val mapper = Mmc3Mapper(Rom())
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
    private val prgData = ByteArray(8 * PRG_BANK_SIZE)
    private val mapper = Mmc3Mapper(Rom(prgData = prgData))
    private val checker = BankMappingChecker(
      bankSize = PRG_BANK_SIZE,
      outBase = BASE_PRG_ROM,
      setSrc = takesBytes(prgData::set),
      getOut = mapper.prg::get
    )

    @Test
    fun `mode 0 - variable bank 0, fixed bank 2`() {
      mapper.setModeAndReg(prgMode = 0, reg = 6, data = 3)
      mapper.setModeAndReg(prgMode = 0, reg = 7, data = 5)

      checker.assertMapping(srcBank = 3, outBank = 0)
      checker.assertMapping(srcBank = 5, outBank = 1)
      checker.assertMapping(srcBank = 6, outBank = 2)   // Fixed to penultimate
      checker.assertMapping(srcBank = 7, outBank = 3)   // Fixed to last
    }

    @Test
    fun `mode 1 - fixed bank 0, variable bank 2`() {
      mapper.setModeAndReg(prgMode = 1, reg = 6, data = 3)
      mapper.setModeAndReg(prgMode = 1, reg = 7, data = 5)

      checker.assertMapping(srcBank = 6, outBank = 0)   // Fixed to penultimate
      checker.assertMapping(srcBank = 5, outBank = 1)
      checker.assertMapping(srcBank = 3, outBank = 2)
      checker.assertMapping(srcBank = 7, outBank = 3)   // Fixed to last
    }

    @Test
    fun `bank mapping wraps`() {
      mapper.setModeAndReg(prgMode = 0, reg = 6, data = 3 + 8 + 8)
      mapper.setModeAndReg(prgMode = 0, reg = 7, data = 5 + 8)

      checker.assertMapping(srcBank = 3, outBank = 0)
      checker.assertMapping(srcBank = 5, outBank = 1)
    }
  }

  @Nested
  inner class ChrRom {
    private val chrData = ByteArray(8 * CHR_BANK_SIZE)
    private val mapper = Mmc3Mapper(Rom(chrData = chrData))
    private val checker = BankMappingChecker(
      bankSize = CHR_BANK_SIZE,
      outBase = BASE_CHR_ROM,
      setSrc = takesBytes(chrData::set),
      getOut = mapper.chr(mock())::get
    )

    @Test
    fun `mode 0 - low banks are 2k, high banks are 1k`() {
      mapper.setModeAndReg(chrMode = 0, reg = 0, data = 5) // Note we ignore LSB of selected bank
      mapper.setModeAndReg(chrMode = 0, reg = 1, data = 0) // Note we ignore LSB of selected bank
      mapper.setModeAndReg(chrMode = 0, reg = 2, data = 3)
      mapper.setModeAndReg(chrMode = 0, reg = 3, data = 6)
      mapper.setModeAndReg(chrMode = 0, reg = 4, data = 2)
      mapper.setModeAndReg(chrMode = 0, reg = 5, data = 1)

      checker.assertMapping(srcBank = 4, outBank = 0)
      checker.assertMapping(srcBank = 5, outBank = 1)
      checker.assertMapping(srcBank = 0, outBank = 2)
      checker.assertMapping(srcBank = 1, outBank = 3)
      checker.assertMapping(srcBank = 3, outBank = 4)
      checker.assertMapping(srcBank = 6, outBank = 5)
      checker.assertMapping(srcBank = 2, outBank = 6)
      checker.assertMapping(srcBank = 1, outBank = 7)
    }

    @Test
    fun `mode 1 - low banks are 1k, high banks are 2k`() {
      mapper.setModeAndReg(chrMode = 1, reg = 0, data = 5) // Note we ignore LSB of selected bank
      mapper.setModeAndReg(chrMode = 1, reg = 1, data = 0) // Note we ignore LSB of selected bank
      mapper.setModeAndReg(chrMode = 1, reg = 2, data = 3)
      mapper.setModeAndReg(chrMode = 1, reg = 3, data = 6)
      mapper.setModeAndReg(chrMode = 1, reg = 4, data = 2)
      mapper.setModeAndReg(chrMode = 1, reg = 5, data = 1)

      checker.assertMapping(srcBank = 3, outBank = 0)
      checker.assertMapping(srcBank = 6, outBank = 1)
      checker.assertMapping(srcBank = 2, outBank = 2)
      checker.assertMapping(srcBank = 1, outBank = 3)
      checker.assertMapping(srcBank = 4, outBank = 4)
      checker.assertMapping(srcBank = 5, outBank = 5)
      checker.assertMapping(srcBank = 0, outBank = 6)
      checker.assertMapping(srcBank = 1, outBank = 7)
    }

    @Test
    fun `bank mapping wraps`() {
      mapper.setModeAndReg(chrMode = 0, reg = 0, data = 5 + 8) // Note we ignore LSB of selected bank
      mapper.setModeAndReg(chrMode = 0, reg = 1, data = 0 + 8) // Note we ignore LSB of selected bank
      mapper.setModeAndReg(chrMode = 0, reg = 2, data = 3 + 8)
      mapper.setModeAndReg(chrMode = 0, reg = 3, data = 6 + 8)
      mapper.setModeAndReg(chrMode = 0, reg = 4, data = 2 + 8)
      mapper.setModeAndReg(chrMode = 0, reg = 5, data = 1 + 8)

      checker.assertMapping(srcBank = 4, outBank = 0)
      checker.assertMapping(srcBank = 5, outBank = 1)
      checker.assertMapping(srcBank = 0, outBank = 2)
      checker.assertMapping(srcBank = 1, outBank = 3)
      checker.assertMapping(srcBank = 3, outBank = 4)
      checker.assertMapping(srcBank = 6, outBank = 5)
      checker.assertMapping(srcBank = 2, outBank = 6)
      checker.assertMapping(srcBank = 1, outBank = 7)
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

  @Nested
  inner class Irq {
    private val mapper = Mmc3Mapper(Rom(chrData = ByteArray(CHR_BANK_SIZE)))
    private val chr = mapper.chr(mock())

    init {
      setNewValue(3)
    }

    @Test
    fun `irq asserted on (N+1)-th rising edge`() {
      enable()

      assertEquals(
        listOf(_0, _0, _0, _0, _0, _0, _0, _1),
        pumpAndCollect(enoughToTrigger)
      )
    }

    @Test
    fun `irq stays asserted`() {
      enable()
      pumpAndCollect(enoughToTrigger)

      assertEquals(
        listOf(_1).repeat(4),
        pumpAndCollect(listOf(lo, hi, lo, hi))
      )
    }

    @Test
    fun `irq de-asserted when disabled`() {
      enable()
      pumpAndCollect(enoughToTrigger)
      disable()

      assertEquals(
        listOf(_0).repeat(4),
        pumpAndCollect(listOf(lo, hi, lo, hi))
      )
    }

    @Test
    fun `irq asserted so long as it's enabled for final rising edge`() {
      disable()
      pumpAndCollect(enoughToTrigger.dropLast(1)) // Just before (N+1)-th rising edge
      enable()

      assertEquals(
        listOf(_1),
        pumpAndCollect(listOf(hi))
      )
    }

    @Test
    fun `irq not asserted if not enabled for final rising edge`() {
      enable()
      pumpAndCollect(enoughToTrigger.dropLast(1)) // Just before (N+1)-th rising edge
      disable()

      assertEquals(
        listOf(_0),
        pumpAndCollect(listOf(hi))
      )
    }

    @Test
    fun `new value doesn't affect current countdown`() {
      enable()
      pumpAndCollect(enoughToTrigger.dropLast(2))
      setNewValue(5)

      assertEquals(
        listOf(_0, _1),   // We still trigger, meaning we ignore the new value
        pumpAndCollect(listOf(lo, hi))
      )
    }

    @Test
    fun `new value affects next countdown`() {
      enable()
      pumpAndCollect(enoughToTrigger.dropLast(2))
      setNewValue(5)
      pumpAndCollect(listOf(lo, hi))
      disable()   // Clear IRQ
      enable()

      assertEquals(
        listOf(_0, _0, _0, _0, _0, _0, _0, _0, _0, _0, _0, _1),
        pumpAndCollect(listOf(lo, hi, lo, hi, lo, hi, lo, hi, lo, hi, lo, hi))
      )
    }

    @Test
    fun `current countdown restarted if reload flag set`() {
      enable()
      pumpAndCollect(enoughToTrigger.dropLast(2))
      reload()

      assertEquals(
        listOf(_0, _0, _0, _0, _0, _0, _0, _1),
        pumpAndCollect(enoughToTrigger)
      )
    }

    private fun enable() {
      mapper.prg[0xE001] = 0xCC
    }

    private fun disable() {
      mapper.prg[0xE000] = 0xCC
    }

    private fun setNewValue(n: Int) {
      mapper.prg[0xC000] = n
    }

    private fun reload() {
      mapper.prg[0xC001] = 0xCC
    }

    private fun pumpAndCollect(addrs: List<Address>): List<Boolean> {
      val ret = mutableListOf<Boolean>()
      addrs.forEach { chr[it]; ret += mapper.irq }
      return ret
    }

    private val lo = 0x0000
    private val hi = 0x1000
    private val enoughToTrigger = listOf(lo, hi, lo, hi, lo, hi, lo, hi)
  }

  private fun Mapper.setModeAndReg(chrMode: Int = 0, prgMode: Int = 0, reg: Int, data: Data) {
    prg[0x8000] = (chrMode shl 7) or (prgMode shl 6) or reg
    prg[0x8001] = data
  }
}
