package choliver.nespot.ppu

import choliver.nespot.*
import choliver.nespot.ppu.Ppu.Companion.BASE_PALETTE
import choliver.nespot.ppu.Ppu.Companion.REG_OAMADDR
import choliver.nespot.ppu.Ppu.Companion.REG_OAMDATA
import choliver.nespot.ppu.Ppu.Companion.REG_PPUADDR
import choliver.nespot.ppu.Ppu.Companion.REG_PPUCTRL
import choliver.nespot.ppu.Ppu.Companion.REG_PPUDATA
import choliver.nespot.ppu.Ppu.Companion.REG_PPUMASK
import choliver.nespot.ppu.Ppu.Companion.REG_PPUSCROLL
import choliver.nespot.ppu.Ppu.Companion.REG_PPUSTATUS
import choliver.nespot.ppu.model.State
import choliver.nespot.sixfiveohtwo.utils._0
import choliver.nespot.sixfiveohtwo.utils._1
import com.nhaarman.mockitokotlin2.*
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertDoesNotThrow

class PpuTest {
  private val memory = mock<Memory>()
  private val renderer = mock<Renderer>()
  private val ppu = Ppu(
    memory = memory,
    videoBuffer = mock(),
    renderer = renderer
  )

  @Test
  fun `write to disallowed register doesn't fail`() {
    assertDoesNotThrow {
      ppu.writeReg(REG_PPUSTATUS, 0x12)   // Micro Machines does this on startup (probably by accident)
    }
  }

  @Nested
  inner class ExternalMemory {
    @Test
    fun `writes to incrementing memory locations`() {
      setPpuAddress(0x1230)

      ppu.writeReg(REG_PPUDATA, 0x20)
      ppu.writeReg(REG_PPUDATA, 0x30)
      ppu.writeReg(REG_PPUDATA, 0x40)

      verify(memory)[0x1230] = 0x20
      verify(memory)[0x1231] = 0x30
      verify(memory)[0x1232] = 0x40
    }

    @Test
    fun `reads from incrementing memory locations, first read is garbage`() {
      whenever(memory[0x1230]) doReturn 0x20
      whenever(memory[0x1231]) doReturn 0x30
      whenever(memory[0x1232]) doReturn 0x40

      setPpuAddress(0x1230)

      ppu.readReg(REG_PPUDATA)  // Ignore garbage read
      assertEquals(0x20, ppu.readReg(REG_PPUDATA))
      assertEquals(0x30, ppu.readReg(REG_PPUDATA))
      assertEquals(0x40, ppu.readReg(REG_PPUDATA))
    }

    @Test
    fun `reads from successive latched addresses, first read is garbage in each case`() {
      whenever(memory[0x1230]) doReturn 0x20
      whenever(memory[0x1590]) doReturn 0x30

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

      verify(memory)[0x1230] = 0x20
      verify(memory)[0x1250] = 0x30
      verify(memory)[0x1270] = 0x40
    }

    @Test
    fun `address reset works`() {
      ppu.writeReg(REG_PPUADDR, 0x33)
      ppu.readReg(REG_PPUSTATUS)  // Would fail without this to reset the flip-flop after the above write
      setPpuAddress(0x1230)

      ppu.writeReg(REG_PPUDATA, 0x20)

      verify(memory)[0x1230] = 0x20
    }
  }

  @Nested
  inner class PaletteMemory {
    @Test
    fun `writes and reads from incrementing palette memory locations, without garbage read`() {
      setPpuAddress(BASE_PALETTE)

      ppu.writeReg(REG_PPUDATA, 0x10)
      ppu.writeReg(REG_PPUDATA, 0x20)
      ppu.writeReg(REG_PPUDATA, 0x30)

      setPpuAddress(BASE_PALETTE)

      assertEquals(0x10, ppu.readReg(REG_PPUDATA))
      assertEquals(0x20, ppu.readReg(REG_PPUDATA))
      assertEquals(0x30, ppu.readReg(REG_PPUDATA))
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
    fun `interrupt not asserted if disabled`() {
      repeat(SCREEN_HEIGHT + 2) { ppu.executeScanline() }
      assertFalse(ppu.vbl)
    }

    @Test
    fun `interrupt asserted during VBL phase`() {
      ppu.writeReg(REG_PPUCTRL, 0x80)

      repeat(SCREEN_HEIGHT + 1) { ppu.executeScanline() }
      assertFalse(ppu.vbl)

      ppu.executeScanline()
      assertTrue(ppu.vbl)

      repeat(SCANLINES_PER_FRAME - SCREEN_HEIGHT - 2) { ppu.executeScanline() }
      assertFalse(ppu.vbl)
    }

    @Test
    fun `interrupt assertion can be modulated`() {
      repeat(SCREEN_HEIGHT + 2) { ppu.executeScanline() }
      assertFalse(ppu.vbl)

      ppu.writeReg(REG_PPUCTRL, 0x80)
      ppu.executeScanline()
      assertTrue(ppu.vbl)

      ppu.writeReg(REG_PPUCTRL, 0x00)
      ppu.executeScanline()
      assertFalse(ppu.vbl)

      ppu.writeReg(REG_PPUCTRL, 0x80)
      ppu.executeScanline()
      assertTrue(ppu.vbl)
    }

    @Test
    fun `status flag not set on scanline 0`() {
      assertEquals(_0, getVblStatusFromReg())
    }

    @Test
    fun `status flag still not set after scanline SCREEN_HEIGHT`() {
      for (i in 0..SCREEN_HEIGHT) { ppu.executeScanline() }

      assertEquals(_0, getVblStatusFromReg())
    }

    @Test
    fun `status flag set after scanline (SCREEN_HEIGHT + 1)`() {
      for (i in 0..(SCREEN_HEIGHT + 1)) { ppu.executeScanline() }

      assertEquals(_1, getVblStatusFromReg())
    }

    @Test
    fun `status flag still set on penultimate scanline`() {
      for (i in 0..(SCANLINES_PER_FRAME - 2)) { ppu.executeScanline() }

      assertEquals(_1, getVblStatusFromReg())
    }

    @Test
    fun `status flag cleared on final scanline`() {
      for (i in 0..(SCANLINES_PER_FRAME - 1)) { ppu.executeScanline() }

      assertEquals(_0, getVblStatusFromReg())
    }

    @Test
    fun `status flag cleared by reading it, and not set again on next scanline`() {
      for (i in 0..(SCREEN_HEIGHT + 1)) { ppu.executeScanline() }

      assertEquals(_1, getVblStatusFromReg())
      assertEquals(_0, getVblStatusFromReg())

      ppu.executeScanline()

      assertEquals(_0, getVblStatusFromReg())
    }

    private fun getVblStatusFromReg() = ppu.readReg(REG_PPUSTATUS).isBitSet(7)
  }

  @Nested
  inner class RendererInput {
    init {
      ppu.writeReg(REG_PPUMASK, 0b00001000)   // Rendering enabled
    }

    @Test
    fun `propagates sprEnabled`() {
      ppu.writeReg(REG_PPUMASK, 0b00010000)
      ppu.executeScanline()

      assertEquals(true, captureContext().sprEnabled)
    }

    @Test
    fun `propagates bgEnabled`() {
      ppu.writeReg(REG_PPUMASK, 0b00001000)
      ppu.executeScanline()

      assertEquals(true, captureContext().bgEnabled)
    }

    @Test
    fun `propagates sprLeftTileEnabled`() {
      ppu.writeReg(REG_PPUMASK, 0b00000100)
      ppu.executeScanline()

      assertEquals(true, captureContext().sprLeftTileEnabled)
    }

    @Test
    fun `propagates bgLeftTileEnabled`() {
      ppu.writeReg(REG_PPUMASK, 0b00000010)
      ppu.executeScanline()

      assertEquals(true, captureContext().bgLeftTileEnabled)
    }

    @Test
    fun `propagates largeSprites`() {
      ppu.writeReg(REG_PPUCTRL, 0b00100000)
      ppu.executeScanline()

      assertEquals(true, captureContext().largeSprites)
    }

    @Test
    fun `propagates bgPatternTable`() {
      ppu.writeReg(REG_PPUCTRL, 0b00010000)
      ppu.executeScanline()

      assertEquals(1, captureContext().bgPatternTable)
    }

    @Test
    fun `propagates sprPatternTable`() {
      ppu.writeReg(REG_PPUCTRL, 0b00001000)
      ppu.executeScanline()

      assertEquals(1, captureContext().sprPatternTable)
    }

    @Test
    fun `propagates yNametable`() {
      ppu.writeReg(REG_PPUCTRL, 0b00000010)
      ppu.executeScanline()

      assertEquals(1, captureContext().coords.yNametable)
    }

    @Test
    fun `propagates xNametable`() {
      ppu.writeReg(REG_PPUCTRL, 0b00000001)
      ppu.executeScanline()

      assertEquals(1, captureContext().coords.xNametable)
    }

    @Test
    fun `propagates xCoarse, xFine, yCoarse, yFine`() {
      ppu.writeReg(REG_PPUSCROLL, 0b10101_111)
      ppu.writeReg(REG_PPUSCROLL, 0b11111_101)
      ppu.executeScanline()

      val ctx = captureContext()
      assertEquals(0b10101, ctx.coords.xCoarse)
      assertEquals(0b111, ctx.coords.xFine)
      assertEquals(0b11111, ctx.coords.yCoarse)
      assertEquals(0b101, ctx.coords.yFine)
    }

    @Test
    fun `increments y components every scanline`() {
      ppu.writeReg(REG_PPUSCROLL, 0b00000_000)
      ppu.writeReg(REG_PPUSCROLL, 0b00000_111)
      ppu.executeScanline()
      ppu.executeScanline()

      val ctx = captureContext(2)
      assertEquals(0b00001, ctx[1].coords.yCoarse)
      assertEquals(0b000, ctx[1].coords.yFine)
      assertEquals(2, ppu.scanline)
    }

    @Test
    fun `doesn't increment y components every scanline if rendering disabled`() {
      ppu.writeReg(REG_PPUSCROLL, 0b00000_000)
      ppu.writeReg(REG_PPUSCROLL, 0b00000_111)
      ppu.executeScanline()
      ppu.writeReg(REG_PPUMASK, 0b00000000)   // Rendering disabled
      ppu.executeScanline()

      val ctx = captureContext(2)
      assertEquals(0b00000, ctx[1].coords.yCoarse)    // These haven't advanced
      assertEquals(0b111, ctx[1].coords.yFine)
    }

    @Test
    fun `x components reloaded every scanline`() {
      ppu.writeReg(REG_PPUSCROLL, 0b00000_000)  // Initially zero
      ppu.writeReg(REG_PPUSCROLL, 0b00000_000)
      ppu.executeScanline()
      ppu.writeReg(REG_PPUSCROLL, 0b10101_111)  // New values
      ppu.executeScanline()

      val ctx = captureContext(2)
      assertEquals(0b10101, ctx[1].coords.xCoarse)    // New values reloaded
      assertEquals(0b111, ctx[1].coords.xFine)
    }

    @Test
    fun `y components not reloaded every scanline`() {
      ppu.writeReg(REG_PPUSCROLL, 0b00000_000)  // Initially zero
      ppu.writeReg(REG_PPUSCROLL, 0b00000_000)  // Initially zero
      ppu.executeScanline()
      ppu.writeReg(REG_PPUSCROLL, 0b00000_000)  // Initially zero
      ppu.writeReg(REG_PPUSCROLL, 0b10101_111)  // New values
      ppu.executeScanline()

      val ctx = captureContext(2)
      assertEquals(0b00000, ctx[1].coords.yCoarse)    // New values ignored, just regular increment
      assertEquals(0b001, ctx[1].coords.yFine)
    }

    @Test
    fun `PPUADDR maps to coordinates in a weird way`() {
      ppu.writeReg(REG_PPUADDR, 0b00_10_11_11)
      ppu.writeReg(REG_PPUADDR, 0b111_10101)
      ppu.executeScanline()

      val ctx = captureContext()
      assertEquals(1, ctx.coords.xNametable)
      assertEquals(0b10101, ctx.coords.xCoarse)
      assertEquals(1, ctx.coords.yNametable)
      assertEquals(0b11111, ctx.coords.yCoarse)
      assertEquals(0b010, ctx.coords.yFine)
    }

    @Test
    fun `PPUADDR changes propagate immediately way`() {
      ppu.executeScanline()
      ppu.writeReg(REG_PPUADDR, 0b00_10_11_11)
      ppu.writeReg(REG_PPUADDR, 0b111_10101)
      ppu.executeScanline()

      val ctx = captureContext(2)
      assertEquals(1, ctx[1].coords.xNametable)
      assertEquals(0b10101, ctx[1].coords.xCoarse)
      assertEquals(1, ctx[1].coords.yNametable)
      assertEquals(0b11111, ctx[1].coords.yCoarse)
      assertEquals(0b011, ctx[1].coords.yFine)  // +1 because gets incremented for next scanline
    }

    // TODO - entire coords reloaded per frame

    private fun captureContext() = captureContext(1).first()

    private fun captureContext(num: Int): List<State> {
      val captor = argumentCaptor<State>()
      verify(renderer, times(num)).renderScanline(captor.capture())
      return captor.allValues
    }
  }

  @Nested
  inner class StatusFlags {
    @Test
    fun `not set if no hit or overflow`() {
      mockResult(sprite0Hit = false, spriteOverflow = false)

      ppu.executeScanline()

      assertFalse(getHitStatus())
      assertFalse(getOverflowStatus())
    }

    @Test
    fun `set if hit`() {
      mockResult(sprite0Hit = true, spriteOverflow = false)

      ppu.executeScanline()

      assertTrue(getHitStatus())
    }

    @Test
    fun `set if overflow`() {
      mockResult(sprite0Hit = false, spriteOverflow = true)

      ppu.executeScanline()

      assertTrue(getOverflowStatus())
    }

    @Test
    fun `not cleared by reading`() {
      mockResult(sprite0Hit = true, spriteOverflow = true)

      ppu.executeScanline()

      assertTrue(getHitStatus())
      assertTrue(getHitStatus())
      assertTrue(getOverflowStatus())
      assertTrue(getHitStatus())
    }

    @Test
    fun `cleared on final scanline`() {
      mockResult(sprite0Hit = true, spriteOverflow = true)

      for (i in 0..(SCANLINES_PER_FRAME - 1)) { ppu.executeScanline() }

      assertFalse(getHitStatus())
      assertFalse(getOverflowStatus())
    }

    private fun mockResult(sprite0Hit: Boolean, spriteOverflow: Boolean) {
      whenever(renderer.renderScanline(any())) doAnswer {
        with(it.getArgument(0) as State) {
          this.sprite0Hit = sprite0Hit
          this.spriteOverflow = spriteOverflow
        }
        Unit
      }
    }

    private fun getHitStatus() = ppu.readReg(REG_PPUSTATUS).isBitSet(6)
    private fun getOverflowStatus() = ppu.readReg(REG_PPUSTATUS).isBitSet(5)
  }

  private fun setPpuAddress(addr: Address) {
    ppu.writeReg(REG_PPUADDR, addr.hi())
    ppu.writeReg(REG_PPUADDR, addr.lo())
  }
}
