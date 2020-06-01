package choliver.nespot.mappers

import choliver.nespot.*
import choliver.nespot.apu.repeat
import choliver.nespot.cartridge.Mapper
import choliver.nespot.cartridge.Rom
import choliver.nespot.cartridge.StandardMapper
import choliver.nespot.cpu.utils._0
import choliver.nespot.cpu.utils._1
import choliver.nespot.mappers.BankMappingChecker.Companion.takesBytes
import choliver.nespot.mappers.Mmc3MapperConfig.Companion.CHR_BANK_SIZE
import choliver.nespot.mappers.Mmc3MapperConfig.Companion.PRG_BANK_SIZE
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

class Mmc3MapperConfigTest {
  @Nested
  inner class PrgRam {
    private val mapper = StandardMapper(Mmc3MapperConfig(Rom()))
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
    private val mapper = StandardMapper(Mmc3MapperConfig(Rom(prgData = prgData)))
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

      checker.assertMappings(
        3 to 0,
        5 to 1,
        6 to 2,   // Fixed to penultimate
        7 to 3    // Fixed to last
      )
    }

    @Test
    fun `mode 1 - fixed bank 0, variable bank 2`() {
      mapper.setModeAndReg(prgMode = 1, reg = 6, data = 3)
      mapper.setModeAndReg(prgMode = 1, reg = 7, data = 5)

      checker.assertMappings(
        6 to 0,   // Fixed to penultimate
        5 to 1,
        3 to 2,
        7 to 3    // Fixed to last
      )
    }

    @Test
    fun `bank mapping wraps`() {
      mapper.setModeAndReg(prgMode = 0, reg = 6, data = 3 + 8 + 8)
      mapper.setModeAndReg(prgMode = 0, reg = 7, data = 5 + 8)

      checker.assertMappings(
        3 to 0,
        5 to 1
      )
    }
  }

  @Nested
  inner class ChrRom {
    private val chrData = ByteArray(8 * CHR_BANK_SIZE)
    private val mapper = StandardMapper(Mmc3MapperConfig(Rom(chrData = chrData)))
    private val checker = BankMappingChecker(
      bankSize = CHR_BANK_SIZE,
      outBase = BASE_CHR_ROM,
      setSrc = takesBytes(chrData::set),
      getOut = mapper.chr::get
    )

    @Test
    fun `mode 0 - low banks are 2k, high banks are 1k`() {
      mapper.setModeAndReg(chrMode = 0, reg = 0, data = 5) // Note we ignore LSB of selected bank
      mapper.setModeAndReg(chrMode = 0, reg = 1, data = 0) // Note we ignore LSB of selected bank
      mapper.setModeAndReg(chrMode = 0, reg = 2, data = 3)
      mapper.setModeAndReg(chrMode = 0, reg = 3, data = 6)
      mapper.setModeAndReg(chrMode = 0, reg = 4, data = 2)
      mapper.setModeAndReg(chrMode = 0, reg = 5, data = 1)

      checker.assertMappings(
        4 to 0,
        5 to 1,
        0 to 2,
        1 to 3,
        3 to 4,
        6 to 5,
        2 to 6,
        1 to 7
      )
    }

    @Test
    fun `mode 1 - low banks are 1k, high banks are 2k`() {
      mapper.setModeAndReg(chrMode = 1, reg = 0, data = 5) // Note we ignore LSB of selected bank
      mapper.setModeAndReg(chrMode = 1, reg = 1, data = 0) // Note we ignore LSB of selected bank
      mapper.setModeAndReg(chrMode = 1, reg = 2, data = 3)
      mapper.setModeAndReg(chrMode = 1, reg = 3, data = 6)
      mapper.setModeAndReg(chrMode = 1, reg = 4, data = 2)
      mapper.setModeAndReg(chrMode = 1, reg = 5, data = 1)

      checker.assertMappings(
        3 to 0,
        6 to 1,
        2 to 2,
        1 to 3,
        4 to 4,
        5 to 5,
        0 to 6,
        1 to 7
      )
    }

    @Test
    fun `bank mapping wraps`() {
      mapper.setModeAndReg(chrMode = 0, reg = 0, data = 5 + 8) // Note we ignore LSB of selected bank
      mapper.setModeAndReg(chrMode = 0, reg = 1, data = 0 + 8) // Note we ignore LSB of selected bank
      mapper.setModeAndReg(chrMode = 0, reg = 2, data = 3 + 8)
      mapper.setModeAndReg(chrMode = 0, reg = 3, data = 6 + 8)
      mapper.setModeAndReg(chrMode = 0, reg = 4, data = 2 + 8)
      mapper.setModeAndReg(chrMode = 0, reg = 5, data = 1 + 8)

      checker.assertMappings(
        4 to 0,
        5 to 1,
        0 to 2,
        1 to 3,
        3 to 4,
        6 to 5,
        2 to 6,
        1 to 7
      )
    }
  }

  @Nested
  inner class Vram {
    private val mapper = StandardMapper(Mmc3MapperConfig(Rom()))

    @Test
    fun `vertical mirroring`() {
      setMode(0)

      assertVramMappings(mapper, listOf(0, 2), listOf(1, 3))
    }

    @Test
    fun `horizontal mirroring`() {
      setMode(1)

      assertVramMappings(mapper, listOf(0, 1), listOf(2, 3))
    }

    private fun setMode(mode: Int) {
      mapper.prg[0xA000] = mode
    }
  }

  @Nested
  inner class Irq {
    private val mapper = StandardMapper(Mmc3MapperConfig(Rom(chrData = ByteArray(CHR_BANK_SIZE))))

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
      addrs.forEach { mapper.chr[it]; ret += mapper.irq }
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
