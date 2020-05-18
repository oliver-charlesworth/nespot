package choliver.nespot.ppu

import choliver.nespot.*
import choliver.nespot.cpu.utils._0
import choliver.nespot.cpu.utils._1
import choliver.nespot.ppu.Ppu.Companion.BASE_PALETTE
import choliver.nespot.ppu.Ppu.Companion.REG_OAMADDR
import choliver.nespot.ppu.Ppu.Companion.REG_OAMDATA
import choliver.nespot.ppu.Ppu.Companion.REG_PPUADDR
import choliver.nespot.ppu.Ppu.Companion.REG_PPUCTRL1
import choliver.nespot.ppu.Ppu.Companion.REG_PPUCTRL2
import choliver.nespot.ppu.Ppu.Companion.REG_PPUDATA
import choliver.nespot.ppu.Ppu.Companion.REG_PPUSCROLL
import choliver.nespot.ppu.Ppu.Companion.REG_PPUSTATUS
import choliver.nespot.ppu.model.State
import com.nhaarman.mockitokotlin2.*
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertDoesNotThrow
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import java.nio.IntBuffer
import kotlin.math.ceil

class PpuTest {
  private val memory = mock<Memory>()
  private val renderer = mock<Renderer>()
  private val onVideoBufferReady = mock<(IntBuffer) -> Unit>()
  private val ppu = Ppu(
    memory = memory,
    renderer = renderer,
    onVideoBufferReady = onVideoBufferReady
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
      ppu.writeReg(REG_PPUCTRL1, 0x04)
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
    fun `invokes callback once per frame at beginning of post-post-render scanline`() {
      advanceScanlines(SCREEN_HEIGHT + 1)
      verifyZeroInteractions(onVideoBufferReady)

      advanceDots(3)
      verify(onVideoBufferReady)(any())

      advanceDots(DOTS_PER_SCANLINE - 3)
      advanceScanlines(SCANLINES_PER_FRAME - SCREEN_HEIGHT - 2)
      verifyNoMoreInteractions(onVideoBufferReady)
    }

    @Test
    fun `interrupt asserted at beginning of post-post-render scanline til beginning of pre-render scanline`() {
      ppu.writeReg(REG_PPUCTRL1, 0x80)

      advanceScanlines(SCREEN_HEIGHT + 1)
      assertFalse(ppu.vbl)

      advanceDots(3)
      assertTrue(ppu.vbl)

      advanceDots(DOTS_PER_SCANLINE - 6)
      advanceScanlines(SCANLINES_PER_FRAME - SCREEN_HEIGHT - 3)
      assertTrue(ppu.vbl)

      advanceDots(3)
      assertFalse(ppu.vbl)
    }

    @Test
    fun `interrupt not asserted if disabled`() {
      advanceScanlines(SCREEN_HEIGHT + 2)
      assertFalse(ppu.vbl)
    }

    @Test
    fun `interrupt assertion can be modulated`() {
      advanceScanlines(SCREEN_HEIGHT + 2)
      assertFalse(ppu.vbl)

      ppu.writeReg(REG_PPUCTRL1, 0x80)
      advanceScanlines(1)
      assertTrue(ppu.vbl)

      ppu.writeReg(REG_PPUCTRL1, 0x00)
      advanceScanlines(1)
      assertFalse(ppu.vbl)

      ppu.writeReg(REG_PPUCTRL1, 0x80)
      advanceScanlines(1)
      assertTrue(ppu.vbl)
    }

    @Test
    fun `status flag not set on scanline 0`() {
      assertEquals(_0, getVblStatusFromReg())
    }

    @Test
    fun `status flag still not set after scanline SCREEN_HEIGHT`() {
      advanceScanlines(SCREEN_HEIGHT + 1)

      assertEquals(_0, getVblStatusFromReg())
    }

    @Test
    fun `status flag set after scanline (SCREEN_HEIGHT + 1)`() {
      advanceScanlines(SCREEN_HEIGHT + 2)

      assertEquals(_1, getVblStatusFromReg())
    }

    @Test
    fun `status flag still set before pre-render scanline`() {
      advanceScanlines(SCANLINES_PER_FRAME - 1)

      assertEquals(_1, getVblStatusFromReg())
    }

    @Test
    fun `status flag cleared after pre-render scanline`() {
      advanceScanlines(SCANLINES_PER_FRAME)

      assertEquals(_0, getVblStatusFromReg())
    }

    @Test
    fun `status flag cleared by reading it, and not set again on next scanline`() {
      advanceScanlines(SCREEN_HEIGHT + 2)

      assertEquals(_1, getVblStatusFromReg())
      assertEquals(_0, getVblStatusFromReg())

      advanceScanlines(1)

      assertEquals(_0, getVblStatusFromReg())
    }

    private fun getVblStatusFromReg() = ppu.readReg(REG_PPUSTATUS).isBitSet(7)
  }

  @Nested
  inner class RendererInput {
    init {
      ppu.writeReg(REG_PPUCTRL2, 0b00001000)   // Rendering enabled
    }

    @ParameterizedTest
    @ValueSource(booleans = [false, true])
    fun `propagates sprEnabled`(flag: Boolean) {
      ppu.writeReg(REG_PPUCTRL2, (if (flag) 1 else 0) shl 4)
      advancePastAction()

      assertEquals(flag, captureContext().sprEnabled)
    }

    @ParameterizedTest
    @ValueSource(booleans = [false, true])
    fun `propagates bgEnabled`(flag: Boolean) {
      ppu.writeReg(REG_PPUCTRL2, (if (flag) 1 else 0) shl 3)
      advancePastAction()

      assertEquals(flag, captureContext().bgEnabled)
    }

    @ParameterizedTest
    @ValueSource(booleans = [false, true])
    fun `propagates sprLeftTileEnabled`(flag: Boolean) {
      ppu.writeReg(REG_PPUCTRL2, (if (flag) 1 else 0) shl 2)
      advancePastAction()

      assertEquals(flag, captureContext().sprLeftTileEnabled)
    }

    @ParameterizedTest
    @ValueSource(booleans = [false, true])
    fun `propagates bgLeftTileEnabled`(flag: Boolean) {
      ppu.writeReg(REG_PPUCTRL2, (if (flag) 1 else 0) shl 1)
      advancePastAction()

      assertEquals(flag, captureContext().bgLeftTileEnabled)
    }

    @ParameterizedTest
    @ValueSource(booleans = [false, true])
    fun `propagates greyscale`(flag: Boolean) {
      ppu.writeReg(REG_PPUCTRL2, (if (flag) 1 else 0) shl 0)
      advancePastAction()

      assertEquals(flag, captureContext().greyscale)
    }

    @ParameterizedTest
    @ValueSource(booleans = [false, true])
    fun `propagates largeSprites`(flag: Boolean) {
      ppu.writeReg(REG_PPUCTRL1, (if (flag) 1 else 0) shl 5)
      advancePastAction()

      assertEquals(flag, captureContext().largeSprites)
    }

    @ParameterizedTest
    @ValueSource(ints = [0, 1])
    fun `propagates bgPatternTable`(idx: Int) {
      ppu.writeReg(REG_PPUCTRL1, idx shl 4)
      advancePastAction()

      assertEquals(idx, captureContext().bgPatternTable)
    }

    @ParameterizedTest
    @ValueSource(ints = [0, 1])
    fun `propagates sprPatternTable`(idx: Int) {
      ppu.writeReg(REG_PPUCTRL1, idx shl 3)
      advancePastAction()

      assertEquals(idx, captureContext().sprPatternTable)
    }

    @ParameterizedTest
    @ValueSource(ints = [0, 1, 2, 3])
    fun `propagates nametable idx`(idx: Int) {
      ppu.writeReg(REG_PPUCTRL1, idx)
      advanceToNextFrameAndResetMock()
      advancePastAction()

      assertEquals(idx, captureContext().coords.nametable)
    }

    @Test
    fun `propagates xCoarse, xFine, yCoarse, yFine`() {
      ppu.writeReg(REG_PPUSCROLL, 0b10101_111)
      ppu.writeReg(REG_PPUSCROLL, 0b11111_101)
      advanceToNextFrameAndResetMock()
      advancePastAction()

      with(captureContext().coords) {
        assertEquals(0b10101, xCoarse)
        assertEquals(0b111, xFine)
        assertEquals(0b11111, yCoarse)
        assertEquals(0b101 + 1, yFine)  // This gets incremented
      }
    }

    @Test
    fun `increments y components every scanline`() {
      ppu.writeReg(REG_PPUSCROLL, 0b00000_000)
      ppu.writeReg(REG_PPUSCROLL, 0b00000_111)
      advanceToNextFrameAndResetMock()
      advanceScanlines(2)

      val ctx = captureContext(2)
      assertEquals(0b00000 + 1, ctx[1].coords.yCoarse)
      assertEquals((0b111 + 2) % 8, ctx[1].coords.yFine)
    }

    @Test
    fun `doesn't increment y components every scanline if rendering disabled`() {
      ppu.writeReg(REG_PPUSCROLL, 0b00000_000)
      ppu.writeReg(REG_PPUSCROLL, 0b00000_111)
      advanceToNextFrameAndResetMock()
      advanceScanlines(1)
      ppu.writeReg(REG_PPUCTRL2, 0b00000000)   // Rendering disabled
      advanceScanlines(1)

      val ctx = captureContext(2)
      assertEquals(0b00000 + 1, ctx[1].coords.yCoarse)    // These have only advanced by 1, not 2
      assertEquals((0b111 + 1) % 8, ctx[1].coords.yFine)
    }

    @Test
    fun `x components reloaded every scanline`() {
      ppu.writeReg(REG_PPUSCROLL, 0b00000_000)  // Initially zero
      ppu.writeReg(REG_PPUSCROLL, 0b00000_000)
      advanceScanlines(1)
      ppu.writeReg(REG_PPUSCROLL, 0b10101_111)  // New values
      advanceScanlines(1)

      val ctx = captureContext(2)
      assertEquals(0b10101, ctx[1].coords.xCoarse)    // New values reloaded
      assertEquals(0b111, ctx[1].coords.xFine)
    }

    @Test
    fun `y components not reloaded every scanline`() {
      ppu.writeReg(REG_PPUSCROLL, 0b00000_000)  // Initially zero
      ppu.writeReg(REG_PPUSCROLL, 0b00000_000)  // Initially zero
      advanceScanlines(1)
      ppu.writeReg(REG_PPUSCROLL, 0b00000_000)  // Initially zero
      ppu.writeReg(REG_PPUSCROLL, 0b10101_111)  // New values
      advanceScanlines(1)

      val ctx = captureContext(2)
      assertEquals(0b00000, ctx[1].coords.yCoarse)    // New values ignored, just regular increment
      assertEquals(0b010, ctx[1].coords.yFine)
    }

    @Test
    fun `PPUADDR maps to coordinates in a weird way`() {
      ppu.writeReg(REG_PPUADDR, 0b00_10_11_11)
      ppu.writeReg(REG_PPUADDR, 0b111_10101)
      advanceScanlines(1)

      val ctx = captureContext()
      assertEquals(0b11, ctx.coords.nametable)
      assertEquals(0b10101, ctx.coords.xCoarse)
      assertEquals(0b11111, ctx.coords.yCoarse)
      assertEquals(0b010 + 1, ctx.coords.yFine)  // This gets incremented
    }

    // TODO - entire coords reloaded per frame

    private fun captureContext() = captureContext(1).first()

    private fun captureContext(num: Int): List<State> {
      val captor = argumentCaptor<State>()
      verify(renderer, times(num)).loadAndRenderBackground(captor.capture())
      return captor.allValues
    }

    private fun advancePastAction() {
      advanceDots(256)
    }

    // y components aren't reloaded until EOF, so skip a whole frame
    private fun advanceToNextFrameAndResetMock() {
      advanceScanlines(SCANLINES_PER_FRAME)
      reset(renderer)
    }
  }

  @Nested
  inner class StatusFlags {
    @Test
    fun `not set if no hit or overflow`() {
      mockResult(sprite0Hit = false, spriteOverflow = false)
      advanceScanlines(1)

      assertFalse(getHitStatus())
      assertFalse(getOverflowStatus())
    }

    @Test
    fun `set if hit`() {
      mockResult(sprite0Hit = true, spriteOverflow = false)
      advanceScanlines(1)

      assertTrue(getHitStatus())
    }

    @Test
    fun `set if overflow`() {
      mockResult(sprite0Hit = false, spriteOverflow = true)
      advanceScanlines(1)

      assertTrue(getOverflowStatus())
    }

    @Test
    fun `not cleared by reading`() {
      mockResult(sprite0Hit = true, spriteOverflow = true)
      advanceScanlines(1)

      assertTrue(getHitStatus())
      assertTrue(getHitStatus())
      assertTrue(getOverflowStatus())
      assertTrue(getHitStatus())
    }

    @Test
    fun `cleared on first dot of pre-render scanline`() {
      mockResult(sprite0Hit = true, spriteOverflow = true)
      advanceScanlines(SCANLINES_PER_FRAME - 1)

      assertTrue(getHitStatus())
      assertTrue(getOverflowStatus())

      advanceDots(3)

      assertFalse(getHitStatus())
      assertFalse(getOverflowStatus())
    }

    private fun mockResult(sprite0Hit: Boolean, spriteOverflow: Boolean) {
      whenever(renderer.renderSprites(any())) doAnswer {
        it.getArgument<State>(0).sprite0Hit = sprite0Hit
        Unit
      }
      whenever(renderer.evaluateSprites(any())) doAnswer {
        it.getArgument<State>(0).spriteOverflow = spriteOverflow
        Unit
      }
    }

    private fun getHitStatus() = ppu.readReg(REG_PPUSTATUS).isBitSet(6)
    private fun getOverflowStatus() = ppu.readReg(REG_PPUSTATUS).isBitSet(5)
  }

  @Nested
  inner class RendererTiming {
    @ParameterizedTest
    @ValueSource(ints=[0, 1, SCREEN_HEIGHT - 2, SCREEN_HEIGHT - 1])
    fun `in-frame scanlines do everything`(scanline: Int) {
      advanceScanlines(scanline)
      reset(renderer)

      advanceDots(126)
      verifyZeroInteractions(renderer)

      advanceDots(3)
      inOrder(renderer) {
        verify(renderer).loadAndRenderBackground(any())
        verify(renderer).renderSprites(any())
      }

      advanceDots(128)
      inOrder(renderer) {
        verify(renderer).commitToBuffer(any(), any())
        verify(renderer).evaluateSprites(any())
      }

      advanceDots(63)
      verify(renderer).loadSprites(any())
    }

    @Test
    fun `vbl phase does nothing`() {
      advanceScanlines(SCREEN_HEIGHT)
      reset(renderer)

      advanceScanlines(SCANLINES_PER_FRAME - SCREEN_HEIGHT - 1)
      verifyZeroInteractions(renderer)
    }

    @Test
    fun `pre-render scanline does extraneous stuff`() {
      advanceScanlines(SCANLINES_PER_FRAME - 1)
      reset(onVideoBufferReady)
      reset(renderer)

      advanceDots(252)
      verifyZeroInteractions(renderer)

      advanceDots(6)
      verify(renderer).loadAndRenderBackground(any())
      verifyNoMoreInteractions(renderer)

      advanceDots(63)
      verify(renderer).loadSprites(any())
      verifyZeroInteractions(onVideoBufferReady)
    }
  }

  private fun advanceScanlines(numScanlines: Int) {
    advanceDots(numScanlines * DOTS_PER_SCANLINE)
  }

  private fun advanceDots(numDots: Int) {
    ppu.advance(ceil(numDots.toDouble() / DOTS_PER_CYCLE).toInt())
  }

  private fun setPpuAddress(addr: Address) {
    ppu.writeReg(REG_PPUADDR, addr.hi())
    ppu.writeReg(REG_PPUADDR, addr.lo())
  }
}
