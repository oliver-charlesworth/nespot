package choliver.nes.ppu

import choliver.nes.*
import choliver.nes.ppu.Ppu.Companion.BASE_NAMETABLES
import choliver.nes.ppu.Ppu.Companion.BASE_PALETTE
import choliver.nes.ppu.Ppu.Companion.NAMETABLE_SIZE_BYTES
import choliver.nes.ppu.Ppu.Companion.NUM_SCANLINES
import choliver.nes.ppu.Ppu.Companion.PATTERN_TABLE_SIZE_BYTES
import choliver.nes.ppu.Ppu.Companion.REG_OAMADDR
import choliver.nes.ppu.Ppu.Companion.REG_OAMDATA
import choliver.nes.ppu.Ppu.Companion.REG_PPUADDR
import choliver.nes.ppu.Ppu.Companion.REG_PPUCTRL
import choliver.nes.ppu.Ppu.Companion.REG_PPUDATA
import choliver.nes.ppu.Ppu.Companion.REG_PPUSCROLL
import choliver.nes.ppu.Ppu.Companion.REG_PPUSTATUS
import choliver.nes.sixfiveohtwo.utils._0
import choliver.nes.sixfiveohtwo.utils._1
import com.nhaarman.mockitokotlin2.*
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource

class PpuTest {
  private val memory = mock<Memory>()
  private val renderer = mock<Renderer>()
  private val onVbl = mock<() -> Unit>()
  private val ppu = Ppu(
    memory = memory,
    screen = mock(),
    onVbl = onVbl,
    renderer = renderer
  )

  @Nested
  inner class ExternalMemory {
    @Test
    fun `writes to incrementing memory locations`() {
      setPpuAddress(0x1230)

      ppu.writeReg(REG_PPUDATA, 0x20)
      ppu.writeReg(REG_PPUDATA, 0x30)
      ppu.writeReg(REG_PPUDATA, 0x40)

      verify(memory).store(0x1230, 0x20)
      verify(memory).store(0x1231, 0x30)
      verify(memory).store(0x1232, 0x40)
    }

    @Test
    fun `reads from incrementing memory locations, first read is garbage`() {
      whenever(memory.load(0x1230)) doReturn 0x20
      whenever(memory.load(0x1231)) doReturn 0x30
      whenever(memory.load(0x1232)) doReturn 0x40

      setPpuAddress(0x1230)

      ppu.readReg(REG_PPUDATA)  // Ignore garbage read
      assertEquals(0x20, ppu.readReg(REG_PPUDATA))
      assertEquals(0x30, ppu.readReg(REG_PPUDATA))
      assertEquals(0x40, ppu.readReg(REG_PPUDATA))
    }

    @Test
    fun `reads from successive latched addresses, first read is garbage in each case`() {
      whenever(memory.load(0x1230)) doReturn 0x20
      whenever(memory.load(0x1590)) doReturn 0x30

      setPpuAddress(0x1230)

      ppu.readReg(REG_PPUDATA)  // Ignore garbage read
      assertEquals(0x20, ppu.readReg(REG_PPUDATA))

      setPpuAddress(0x1590)

      ppu.readReg(REG_PPUDATA)  // Ignore garbage read
      assertEquals(0x30, ppu.readReg(REG_PPUDATA))
    }

    @Test
    fun `increments by 32 if PPUCTRL set appropriately`() {
      ppu.writeReg(REG_PPUCTRL, 0x04)
      setPpuAddress(0x1230)

      ppu.writeReg(REG_PPUDATA, 0x20)
      ppu.writeReg(REG_PPUDATA, 0x30)
      ppu.writeReg(REG_PPUDATA, 0x40)

      verify(memory).store(0x1230, 0x20)
      verify(memory).store(0x1250, 0x30)
      verify(memory).store(0x1270, 0x40)
    }
  }

  @Nested
  inner class PaletteMemory {
    @Test
    fun `writes and reads from incrementing palette memory locations, without garbage read`() {
      setPpuAddress(BASE_PALETTE)

      ppu.writeReg(REG_PPUDATA, 0x20)
      ppu.writeReg(REG_PPUDATA, 0x30)
      ppu.writeReg(REG_PPUDATA, 0x40)

      setPpuAddress(BASE_PALETTE)

      assertEquals(0x20, ppu.readReg(REG_PPUDATA))
      assertEquals(0x30, ppu.readReg(REG_PPUDATA))
      assertEquals(0x40, ppu.readReg(REG_PPUDATA))
    }

    @Test
    fun `writes and reads from mirrors`() {
      setPpuAddress(BASE_PALETTE)
      ppu.writeReg(REG_PPUDATA, 0x20)
      setPpuAddress(BASE_PALETTE + 0x1F)
      ppu.writeReg(REG_PPUDATA, 0x30)

      // First mirror
      setPpuAddress(BASE_PALETTE + 0x20)
      assertEquals(0x20, ppu.readReg(REG_PPUDATA))

      // Last mirror
      setPpuAddress(BASE_PALETTE + 0xFF)
      assertEquals(0x30, ppu.readReg(REG_PPUDATA))
    }
  }

  @Nested
  inner class OamMemory {
    @Test
    fun `writes and reads from incrementing memory locations`() {
      ppu.writeReg(REG_OAMADDR, 0x50)

      ppu.writeReg(REG_OAMDATA, 0x20)
      ppu.writeReg(REG_OAMDATA, 0x30)
      ppu.writeReg(REG_OAMDATA, 0x40)

      ppu.writeReg(REG_OAMADDR, 0x50)

      assertEquals(0x20, ppu.readReg(REG_OAMDATA))
      assertEquals(0x30, ppu.readReg(REG_OAMDATA))
      assertEquals(0x40, ppu.readReg(REG_OAMDATA))
    }
  }

  @Nested
  inner class Vbl {
    @Test
    fun `interrupt not fired if disabled`() {
      repeat(NUM_SCANLINES) { ppu.executeScanline() }
      verifyZeroInteractions(onVbl)
    }

    @Test
    fun `interrupt fired only on scanline (SCREEN_HEIGHT + 1) if enabled`() {
      ppu.writeReg(REG_PPUCTRL, 0x80)

      repeat(SCREEN_HEIGHT + 1) { ppu.executeScanline() }
      verifyZeroInteractions(onVbl)

      ppu.executeScanline()
      verify(onVbl)()

      repeat(NUM_SCANLINES - SCREEN_HEIGHT - 2) { ppu.executeScanline() }
      verifyZeroInteractions(onVbl)
    }

    @Test
    fun `status flag not set on scanline 0`() {
      assertEquals(_0, getVblStatus())
    }

    @Test
    fun `status flag still not set after scanline SCREEN_HEIGHT`() {
      for (i in 0..SCREEN_HEIGHT) { ppu.executeScanline() }

      assertEquals(_0, getVblStatus())
    }

    @Test
    fun `status flag set after scanline (SCREEN_HEIGHT + 1)`() {
      for (i in 0..(SCREEN_HEIGHT + 1)) { ppu.executeScanline() }

      assertEquals(_1, getVblStatus())
    }

    @Test
    fun `status flag still set on penultimate scanline`() {
      for (i in 0..(NUM_SCANLINES - 2)) { ppu.executeScanline() }

      assertEquals(_1, getVblStatus())
    }

    @Test
    fun `status flag cleared on final scanline`() {
      for (i in 0..(NUM_SCANLINES - 1)) { ppu.executeScanline() }

      assertEquals(_0, getVblStatus())
    }

    @Test
    fun `status flag cleared by reading it, and not set again on next scanline`() {
      for (i in 0..(SCREEN_HEIGHT + 1)) { ppu.executeScanline() }

      assertEquals(_1, getVblStatus())
      assertEquals(_0, getVblStatus())

      ppu.executeScanline()

      assertEquals(_0, getVblStatus())
    }

    private fun getVblStatus() = ppu.readReg(REG_PPUSTATUS).isBitSet(7)
  }

  @Nested
  inner class RendererContext {
    @Test
    fun `passes consecutive scanline indexes`() {
      ppu.executeScanline()
      ppu.executeScanline()
      ppu.executeScanline()

      val captor = argumentCaptor<Int>()
      verify(renderer, times(3)).renderScanlineAndDetectHit(captor.capture(), any())

      assertEquals(listOf(0, 1, 2), captor.allValues)
    }

    @Test
    fun `passes scroll values`() {
      ppu.readReg(REG_PPUSTATUS)  // Reset address latch
      ppu.writeReg(REG_PPUSCROLL, 0x23)
      ppu.writeReg(REG_PPUSCROLL, 0x45)

      val context = executeAndCapture()

      with(context) {
        assertEquals(0x23, scrollX)
        assertEquals(0x45, scrollY)
      }
    }

    @ParameterizedTest(name = "idx = {0}")
    @ValueSource(ints = [0, 1])
    fun `passes bg pattern table addr`(idx: Int) {
      ppu.writeReg(REG_PPUCTRL, idx shl 4)

      with(executeAndCapture()) {
        assertEquals(idx * PATTERN_TABLE_SIZE_BYTES, bgPatternTableAddr)
      }
    }

    @ParameterizedTest(name = "idx = {0}")
    @ValueSource(ints = [0, 1])
    fun `passes spr pattern table addr`(idx: Int) {
      ppu.writeReg(REG_PPUCTRL, idx shl 3)

      with(executeAndCapture()) {
        assertEquals(idx * PATTERN_TABLE_SIZE_BYTES, sprPatternTableAddr)
      }
    }

    // TODO
//    @ParameterizedTest(name = "idx = {0}")
//    @ValueSource(ints = [0, 1, 2, 3])
//    fun `passes nametable addr`(idx: Int) {
//      ppu.writeReg(REG_PPUCTRL, idx)
//
//      with(executeAndCapture()) {
//        assertEquals(BASE_NAMETABLES + (idx * NAMETABLE_SIZE_BYTES), nametableAddr)
//      }
//    }

    private fun executeAndCapture(): Renderer.Context {
      ppu.executeScanline()
      val captor = argumentCaptor<Renderer.Context>()
      verify(renderer).renderScanlineAndDetectHit(any(), captor.capture())
      return captor.firstValue
    }
  }

  @Nested
  inner class SpriteHit {
    @Test
    fun `status flag not set if not hit`() {
      ppu.executeScanline()

      assertEquals(_0, getHitStatus())
    }

    @Test
    fun `status flag set if hit`() {
      whenever(renderer.renderScanlineAndDetectHit(any(), any())) doReturn true

      ppu.executeScanline()

      assertEquals(_1, getHitStatus())
    }

    @Test
    fun `status flag not cleared by reading it`() {
      whenever(renderer.renderScanlineAndDetectHit(any(), any())) doReturn true

      ppu.executeScanline()

      assertEquals(_1, getHitStatus())
      assertEquals(_1, getHitStatus())
    }

    @Test
    fun `status flag cleared on final scanline`() {
      whenever(renderer.renderScanlineAndDetectHit(any(), any())) doReturn true

      for (i in 0..(NUM_SCANLINES - 1)) { ppu.executeScanline() }

      assertEquals(_0, getHitStatus())
    }

    private fun getHitStatus() = ppu.readReg(REG_PPUSTATUS).isBitSet(6)
  }

  private fun setPpuAddress(addr: Address) {
    ppu.readReg(REG_PPUSTATUS)  // Reset address latch
    ppu.writeReg(REG_PPUADDR, addr.hi())
    ppu.writeReg(REG_PPUADDR, addr.lo())
  }
}
