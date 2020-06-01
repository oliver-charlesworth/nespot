package choliver.nespot.mappers

import choliver.nespot.Address
import choliver.nespot.Data
import choliver.nespot.apu.repeat
import choliver.nespot.cartridge.Rom
import choliver.nespot.cartridge.Rom.Mirroring.HORIZONTAL
import choliver.nespot.cartridge.Rom.Mirroring.VERTICAL
import choliver.nespot.cartridge.StandardMapper
import choliver.nespot.cartridge.StandardMapper.Config
import choliver.nespot.cpu.utils._0
import choliver.nespot.cpu.utils._1
import choliver.nespot.mappers.Mmc3MapperConfig.Companion.CHR_BANK_SIZE
import choliver.nespot.mappers.Mmc3MapperConfig.Companion.PRG_BANK_SIZE
import com.nhaarman.mockitokotlin2.any
import com.nhaarman.mockitokotlin2.doAnswer
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.whenever
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.mockito.Mockito.RETURNS_DEEP_STUBS
import org.mockito.Mockito.verify

class Mmc3MapperConfigTest {
  private val mapper = mock<StandardMapper>(defaultAnswer = RETURNS_DEEP_STUBS)

  @Nested
  inner class Prg {
    private val map = mutableListOf(0, 0, 0, 0)
    private val config = Mmc3MapperConfig(Rom(prgData = ByteArray(8 * PRG_BANK_SIZE)))

    init {
      whenever(mapper.prg.bankMap.set(any(), any())) doAnswer {
        map[it.getArgument(0)] = it.getArgument(1)
        Unit
      }
    }

    @Test
    fun `sets highest bank on startup`() {
      with(config) {
        mapper.onStartup()
      }

      assertEquals(7, map[3])
    }

    @Test
    fun `mode 0 - variable bank 0, fixed bank 2`() {
      config.setModeAndReg(prgMode = 0, reg = 6, data = 3)
      config.setModeAndReg(prgMode = 0, reg = 7, data = 5)

      assertEquals(listOf(
        3,
        5,
        6,      // Fixed to penultimate
        7       // Fixed to last
      ), map)
    }

    @Test
    fun `mode 1 - fixed bank 0, variable bank 2`() {
      config.setModeAndReg(prgMode = 1, reg = 6, data = 3)
      config.setModeAndReg(prgMode = 1, reg = 7, data = 5)

      assertEquals(listOf(
        6,      // Fixed to penultimate
        5,
        3,
        7       // Fixed to last
      ), map)
    }

    @Test
    fun `bank mapping wraps`() {
      config.setModeAndReg(prgMode = 0, reg = 6, data = 3 + 8 + 8)
      config.setModeAndReg(prgMode = 0, reg = 7, data = 5 + 8)

      assertEquals(listOf(
        3,
        5,
        6,
        7
      ), map)
    }
  }

  @Nested
  inner class ChrRom {
    private val map = mutableListOf(0, 0, 0, 0, 0, 0, 0, 0)
    private val config = Mmc3MapperConfig(Rom(chrData = ByteArray(8 * CHR_BANK_SIZE)))

    init {
      whenever(mapper.chr.bankMap.set(any(), any())) doAnswer {
        map[it.getArgument(0)] = it.getArgument(1)
        Unit
      }
    }

    @Test
    fun `mode 0 - low banks are 2k, high banks are 1k`() {
      config.setModeAndReg(chrMode = 0, reg = 0, data = 5) // Note we ignore LSB of selected bank
      config.setModeAndReg(chrMode = 0, reg = 1, data = 0) // Note we ignore LSB of selected bank
      config.setModeAndReg(chrMode = 0, reg = 2, data = 3)
      config.setModeAndReg(chrMode = 0, reg = 3, data = 6)
      config.setModeAndReg(chrMode = 0, reg = 4, data = 2)
      config.setModeAndReg(chrMode = 0, reg = 5, data = 1)

      assertEquals(listOf(4, 5, 0, 1, 3, 6, 2, 1), map)
    }

    @Test
    fun `mode 1 - low banks are 1k, high banks are 2k`() {
      config.setModeAndReg(chrMode = 1, reg = 0, data = 5) // Note we ignore LSB of selected bank
      config.setModeAndReg(chrMode = 1, reg = 1, data = 0) // Note we ignore LSB of selected bank
      config.setModeAndReg(chrMode = 1, reg = 2, data = 3)
      config.setModeAndReg(chrMode = 1, reg = 3, data = 6)
      config.setModeAndReg(chrMode = 1, reg = 4, data = 2)
      config.setModeAndReg(chrMode = 1, reg = 5, data = 1)

      assertEquals(listOf(3, 6, 2, 1, 4, 5, 0, 1), map)
    }

    @Test
    fun `bank mapping wraps`() {
      config.setModeAndReg(chrMode = 0, reg = 0, data = 5 + 8) // Note we ignore LSB of selected bank
      config.setModeAndReg(chrMode = 0, reg = 1, data = 0 + 8) // Note we ignore LSB of selected bank
      config.setModeAndReg(chrMode = 0, reg = 2, data = 3 + 8)
      config.setModeAndReg(chrMode = 0, reg = 3, data = 6 + 8)
      config.setModeAndReg(chrMode = 0, reg = 4, data = 2 + 8)
      config.setModeAndReg(chrMode = 0, reg = 5, data = 1 + 8)

      assertEquals(listOf(4, 5, 0, 1, 3, 6, 2, 1), map)
    }
  }

  @Nested
  inner class Mirroring {
    private val config = Mmc3MapperConfig(Rom())

    @Test
    fun vertical() {
      setMode(0)

      verify(mapper.chr).mirroring = VERTICAL
    }

    @Test
    fun horizontal() {
      setMode(1)

      verify(mapper.chr).mirroring = HORIZONTAL
    }

    private fun setMode(mode: Int) {
      config.setPrg(0xA000, mode)
    }
  }

  @Nested
  inner class Irq {
    private val config = Mmc3MapperConfig(Rom())

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
      config.setPrg(0xE001, 0xCC)
    }

    private fun disable() {
      config.setPrg(0xE000, 0xCC)
    }

    private fun setNewValue(n: Int) {
      config.setPrg(0xC000, n)
    }

    private fun reload() {
      config.setPrg(0xC001, 0xCC)
    }

    private fun pumpAndCollect(addrs: List<Address>): List<Boolean> {
      val ret = mutableListOf<Boolean>()
      with(config) {
        addrs.forEach { mapper.onChrGet(it); ret += irq }
      }
      return ret
    }

    private val lo = 0x0000
    private val hi = 0x1000
    private val enoughToTrigger = listOf(lo, hi, lo, hi, lo, hi, lo, hi)
  }

  private fun Config.setModeAndReg(chrMode: Int = 0, prgMode: Int = 0, reg: Int, data: Data) {
    setPrg(0x8000, (chrMode shl 7) or (prgMode shl 6) or reg)
    setPrg(0x8001, data)
  }

  private fun Config.setPrg(addr: Address, data: Data) {
    with(this) {
      mapper.onPrgSet(addr, data)
    }
  }
}
