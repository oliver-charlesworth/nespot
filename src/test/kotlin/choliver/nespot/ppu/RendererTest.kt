package choliver.nespot.ppu

import choliver.nespot.Address
import choliver.nespot.Memory
import choliver.nespot.ppu.Ppu.Companion.BASE_NAMETABLES
import choliver.nespot.ppu.Renderer.*
import com.nhaarman.mockitokotlin2.doReturn
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.whenever
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import java.nio.IntBuffer

// TODO - greyscale
// TODO - colour emphasis
// TODO - scrolling
class RendererTest {
  private val colors = (0..63).toList()
  private val paletteEntries = (0..31).map { it + 5 }

  private val memory = mock<Memory>()
  private val palette = mock<Memory> {
    paletteEntries.forEachIndexed { idx, data ->
      on { load(idx) } doReturn data
    }
  }
  private val oam = mock<Memory>()
  private val videoBuffer = IntBuffer.allocate(SCREEN_WIDTH * SCREEN_HEIGHT)
  private val renderer = Renderer(
    memory = memory,
    palette = palette,
    oam = oam,
    videoBuffer = videoBuffer,
    colors = colors
  )

  private val nametable = 0
  private val bgPatternTable = 1
  private val sprPatternTable = 0

  // Chosen scanline
  private val yCoarse = 14
  private val yFine = 4

  @Nested
  inner class Background {
    @Test
    fun `patterns for palette #0`() {
      val pattern = listOf(0, 1, 2, 3, 2, 3, 0, 1)

      initBgPatternMemory(mapOf(0 to pattern))

      assertRendersAs { pattern[it % TILE_SIZE] }
    }

    @Test
    fun `patterns for higher palettes use universal background color`() {
      val pattern = listOf(0, 1, 2, 3, 2, 3, 0, 1)
      val attrEntries = List(NUM_METATILE_COLUMNS) { 1 }  // Arbitrary non-zero palette #

      initAttributeMemory(attrEntries)
      initBgPatternMemory(mapOf(0 to pattern))

      assertRendersAs { pattern[it % TILE_SIZE].let { if (it == 0) 0 else (it + NUM_ENTRIES_PER_PALETTE) } }
    }

    @Test
    fun `location-based attributes - bottom metatile`() {
      val pattern = List(TILE_SIZE) { 1 } // Arbitrary non-zero pixel
      val attrEntries = listOf(0, 1, 2, 3, 2, 3, 0, 1, 3, 2, 1, 0, 1, 0, 3, 2)

      initAttributeMemory(attrEntries)
      initBgPatternMemory(mapOf(0 to pattern))

      assertRendersAs { 1 + attrEntries[it / METATILE_SIZE] * NUM_ENTRIES_PER_PALETTE }
    }

    @Test
    fun `location-based attributes - top metatile`() {
      val pattern = List(TILE_SIZE) { 1 } // Arbitrary non-zero pixel
      val attrEntries = listOf(0, 1, 2, 3, 2, 3, 0, 1, 3, 2, 1, 0, 1, 0, 3, 2)

      initAttributeMemory(attrEntries, yCoarse = 13)
      initBgPatternMemory(mapOf(0 to pattern))

      assertRendersAs(yCoarse = 13) { 1 + attrEntries[it / METATILE_SIZE] * NUM_ENTRIES_PER_PALETTE }
    }

    @Test
    fun `location-based patterns`() {
      val patterns = mapOf(
        11 to List(TILE_SIZE) { 1 },
        22 to List(TILE_SIZE) { 2 },
        33 to List(TILE_SIZE) { 3 },
        44 to List(TILE_SIZE) { 0 }
      )

      val nametableEntries = (0 until NUM_TILE_COLUMNS / 4).flatMap { listOf(11, 22, 33, 44) }

      initNametableMemory(nametableEntries)
      initBgPatternMemory(patterns)

      assertRendersAs { patterns[nametableEntries[it / TILE_SIZE]]!![it % TILE_SIZE] }
    }

    // TODO - conditional rendering
    // TODO - clipping
    // TODO - scrolling / offset (inc. nametable calculations)
  }

  @Nested
  inner class Sprite {
    private val xOffset = 5
    private val pattern = listOf(1, 2, 3, 3, 2, 1, 1, 2)  // No zero values

    @Test
    fun `top row`() {
      initSprPatternMemory(mapOf(1 to pattern), yFine = 0)
      initSpriteMemory(x = xOffset, y = (yCoarse * TILE_SIZE) + yFine, iPattern = 1, attrs = 0)

      assertRendersAs { calcPalIdx(x = it, xOffset = xOffset, iPalette = 0, pattern = pattern) }
    }

    @Test
    fun `bottom row`() {
      initSprPatternMemory(mapOf(1 to pattern), yFine = 7)
      initSpriteMemory(x = xOffset, y = (yCoarse * TILE_SIZE) + yFine - 7, iPattern = 1, attrs = 0)

      assertRendersAs { calcPalIdx(x = it, xOffset = xOffset, iPalette = 0, pattern = pattern) }
    }

    @Test
    fun `horizontally-flipped`() {
      initSprPatternMemory(mapOf(1 to pattern), yFine = 0)
      initSpriteMemory(x = xOffset, y = (yCoarse * TILE_SIZE) + yFine, iPattern = 1, attrs = 0x40)

      assertRendersAs { calcPalIdx(x = it, xOffset = xOffset, iPalette = 0, pattern = pattern.reversed()) }
    }

    @Test
    fun `vertically-flipped`() {
      initSprPatternMemory(mapOf(1 to pattern), yFine = 7)
      initSpriteMemory(x = xOffset, y = (yCoarse * TILE_SIZE) + yFine, iPattern = 1, attrs = 0x80)

      assertRendersAs { calcPalIdx(x = it, xOffset = xOffset, iPalette = 0, pattern = pattern) }
    }

    @Test
    fun `with different palette`() {
      initSprPatternMemory(mapOf(1 to pattern), yFine = 0)
      initSpriteMemory(x = xOffset, y = (yCoarse * TILE_SIZE) + yFine, iPattern = 1, attrs = 2)

      assertRendersAs { calcPalIdx(x = it, xOffset = xOffset, iPalette = 2, pattern = pattern) }
    }

    @Test
    fun `off the right-hand-edge`() {
      initSprPatternMemory(mapOf(1 to pattern), yFine = 0)
      initSpriteMemory(x = 252, y = (yCoarse * TILE_SIZE) + yFine, iPattern = 1, attrs = 0)

      assertRendersAs { calcPalIdx(x = it, xOffset = 252, iPalette = 0, pattern = pattern) }
    }

    // TODO - max sprites per scanline
    // TODO - conditional rendering
    // TODO - clipping

    private fun calcPalIdx(x: Int, xOffset: Int, iPalette: Int, pattern: List<Int>) =
      if (x in xOffset until TILE_SIZE + xOffset) {
        pattern[x - xOffset] + ((NUM_PALETTES + iPalette) * NUM_ENTRIES_PER_PALETTE)
      } else 0
  }

  @Nested
  inner class LargeSprite {
    private val iPalette = 0
    private val xOffset = 5
    private val pattern = listOf(1, 2, 3, 3, 2, 1, 1, 2)  // No zero values

    @Test
    fun `top row`() {
      initSprPatternMemory(mapOf(2 to pattern), yRow = 0)
      initSpriteMemory(x = xOffset, y = (yCoarse * TILE_SIZE) + yFine, iPattern = 2, attrs = 0)

      assertRendersAs(isLargeSprites = true) { calcPalIdx(x = it) }
    }

    @Test
    fun `bottom row`() {
      initSprPatternMemory(mapOf(3 to pattern), yRow = 7) // Note - next pattern index!
      initSpriteMemory(x = xOffset, y = (yCoarse * TILE_SIZE) + yFine - 15, iPattern = 2, attrs = 0)

      assertRendersAs(isLargeSprites = true) { calcPalIdx(x = it) }
    }

    @Test
    fun `top row - vertically-flipped`() {
      initSprPatternMemory(mapOf(3 to pattern), yRow = 7) // Note - next pattern index!
      initSpriteMemory(x = xOffset, y = (yCoarse * TILE_SIZE) + yFine, iPattern = 2, attrs = 0x80)

      assertRendersAs(isLargeSprites = true) { calcPalIdx(x = it) }
    }

    @Test
    fun `bottom row - vertically-flipped`() {
      initSprPatternMemory(mapOf(2 to pattern), yRow = 0)
      initSpriteMemory(x = xOffset, y = (yCoarse * TILE_SIZE) + yFine - 15, iPattern = 2, attrs = 0x80)

      assertRendersAs(isLargeSprites = true) { calcPalIdx(x = it) }
    }

    @Test
    fun `ignore sprPatternTable`() {
      val largeIdx = 258  // Larger than one palette table
      initSprPatternMemory(mapOf(largeIdx to pattern), yRow = 0)
      initSpriteMemory(
        x = xOffset,
        y = (yCoarse * TILE_SIZE) + yFine,
        iPattern = (largeIdx and 0xFE) or (largeIdx shr 8), // Munged to fit in one byte
        attrs = 0
      )

      assertRendersAs(isLargeSprites = true) { calcPalIdx(x = it) }
    }

    private fun calcPalIdx(x: Int) = if (x in xOffset until TILE_SIZE + xOffset) {
      pattern[x - xOffset] + ((NUM_PALETTES + iPalette) * NUM_ENTRIES_PER_PALETTE)
    } else 0

    private fun initSprPatternMemory(patterns: Map<Int, List<Int>>, yRow: Int) {
      initPatternMemory(patterns, yRow, 0x0000)
    }
  }

  @Nested
  inner class Priority {
    private val paletteSpr = 1  // Non-zero palette
    private val paletteBg = 2   // Non-zero palette different to that of sprite

    private val ubg = 0
    private val bg = 1 + (paletteBg * NUM_ENTRIES_PER_PALETTE)
    private val spr = 1 + ((NUM_PALETTES + paletteSpr) * NUM_ENTRIES_PER_PALETTE)

    @Test
    fun `transparent bg, transparent spr, in front - ubg`() {
      assertPixelColour(expected = ubg, bgOpaque = false, sprOpaque = false, sprBehind = false)
    }

    @Test
    fun `transparent bg, transparent spr, behind - ubg`() {
      assertPixelColour(expected = ubg, bgOpaque = false, sprOpaque = false, sprBehind = true)
    }

    @Test
    fun `transparent bg, opaque spr, in front - spr`() {
      assertPixelColour(expected = spr, bgOpaque = false, sprOpaque = true, sprBehind = false)
    }

    @Test
    fun `transparent bg, opaque spr, behind - spr`() {
      assertPixelColour(expected = spr, bgOpaque = false, sprOpaque = true, sprBehind = true)
    }

    @Test
    fun `opaque bg, transparent spr, in front - bg`() {
      assertPixelColour(expected = bg, bgOpaque = true, sprOpaque = false, sprBehind = false)
    }

    @Test
    fun `opaque bg, transparent spr, behind - bg`() {
      assertPixelColour(expected = bg, bgOpaque = true, sprOpaque = false, sprBehind = true)
    }

    @Test
    fun `opaque bg, opaque spr, in front - spr`() {
      assertPixelColour(expected = spr, bgOpaque = true, sprOpaque = true, sprBehind = false)
    }

    @Test
    fun `opaque bg, opaque spr, behind - bg`() {
      assertPixelColour(expected = bg, bgOpaque = true, sprOpaque = true, sprBehind = true)
    }

    private fun assertPixelColour(expected: Int, bgOpaque: Boolean, sprOpaque: Boolean, sprBehind: Boolean) {
      val y = (yCoarse * TILE_SIZE) + yFine

      initAttributeMemory(List(NUM_METATILE_COLUMNS) { paletteBg })
      initBgPatternMemory(mapOf(0 to List(TILE_SIZE) { if (bgOpaque) 1 else 0 }))
      initSprPatternMemory(mapOf(1 to List(TILE_SIZE) { if (sprOpaque) 1 else 0 }), yFine = 0)
      initSpriteMemory(
        x = 0,
        y = y,
        iPattern = 1,
        attrs = (if (sprBehind) 0x20 else 0x00) or paletteSpr
      )

      render()

      assertEquals(colors[paletteEntries[expected]], videoBuffer.array()[y * SCREEN_WIDTH])
    }
  }

  @Nested
  inner class Collisions {
    @Test
    fun `triggers hit if opaque sprite #0 overlaps opaque background`() {
      initBgPatternMemory(mapOf(0 to listOf(1, 1, 1, 1, 1, 1, 1, 1)))
      initSprPatternMemory(mapOf(1 to listOf(1, 0, 0, 0, 0, 0, 0, 0)), yFine = 0)
      initSpriteMemory(x = 5, y = (yCoarse * TILE_SIZE) + yFine, iPattern = 1, attrs = 0)

      assertTrue(render())
    }

    @Test
    fun `doesn't trigger hit if opaque sprite #0 overlaps transparent background`() {
      initBgPatternMemory(mapOf(0 to listOf(0, 0, 0, 0, 0, 0, 0, 0)))
      initSprPatternMemory(mapOf(1 to listOf(1, 0, 0, 0, 0, 0, 0, 0)), yFine = 0)
      initSpriteMemory(x = 5, y = (yCoarse * TILE_SIZE) + yFine, iPattern = 1, attrs = 0)

      assertFalse(render())
    }

    @Test
    fun `doesn't trigger hit if transparent sprite #0 overlaps opaque background`() {
      initBgPatternMemory(mapOf(0 to listOf(1, 1, 1, 1, 1, 1, 1, 1)))
      initSprPatternMemory(mapOf(1 to listOf(0, 0, 0, 0, 0, 0, 0, 0)), yFine = 0)
      initSpriteMemory(x = 5, y = (yCoarse * TILE_SIZE) + yFine, iPattern = 1, attrs = 0)

      assertFalse(render())
    }

    @Test
    fun `triggers single hit even when multiple sprite #0 overlaps`() {
      initBgPatternMemory(mapOf(0 to listOf(1, 1, 1, 1, 1, 1, 1, 1)))
      initSprPatternMemory(mapOf(1 to listOf(1, 0, 1, 0, 1, 0, 1, 0)), yFine = 0)
      initSpriteMemory(x = 5, y = (yCoarse * TILE_SIZE) + yFine, iPattern = 1, attrs = 0)

      assertTrue(render())
    }

    @Test
    fun `doesn't trigger from overlap from sprites other than #0`() {
      initBgPatternMemory(mapOf(0 to listOf(1, 1, 1, 1, 1, 1, 1, 1)))
      initSprPatternMemory(mapOf(1 to listOf(1, 0, 0, 0, 0, 0, 0, 0)), yFine = 0)
      initSpriteMemory(x = 5, y = (yCoarse * TILE_SIZE) + yFine, iPattern = 1, attrs = 0, iSprite = 1)

      assertFalse(render())
    }

    @Test
    fun `doesn't trigger from overlap at x == 255`() {
      initBgPatternMemory(mapOf(0 to listOf(1, 1, 1, 1, 1, 1, 1, 1)))
      initSprPatternMemory(mapOf(1 to listOf(1, 0, 0, 0, 0, 0, 0, 0)), yFine = 0)
      initSpriteMemory(x = 255, y = (yCoarse * TILE_SIZE) + yFine, iPattern = 1, attrs = 0)

      assertFalse(render())
    }
    @Test
    fun `triggers hit from behind`() {
      initBgPatternMemory(mapOf(0 to listOf(1, 1, 1, 1, 1, 1, 1, 1)))
      initSprPatternMemory(mapOf(1 to listOf(1, 0, 0, 0, 0, 0, 0, 0)), yFine = 0)
      initSpriteMemory(x = 5, y = (yCoarse * TILE_SIZE) + yFine, iPattern = 1, attrs = 0x20) // Behind

      assertTrue(render())
    }

    // TODO - not detected in active clipping region (background or sprite)
  }

  // Renderer knows nothing of vertical scroll, so we only test horizontal scroll
  @Nested
  inner class HorizontalScroll {
    @Test
    fun `coarse offset`() {
      val patterns = mapOf(
        11 to List(TILE_SIZE) { 1 },
        22 to List(TILE_SIZE) { 2 },
        33 to List(TILE_SIZE) { 3 },
        44 to List(TILE_SIZE) { 0 }
      )

      // Straddles both nametables
      val nametableEntries = List(NUM_TILE_COLUMNS * 2) {
        when (it) {
          5 -> 11    // Leftmost
          6 -> 22
          35 -> 22
          36 -> 33   // Rightmost
          else -> 44
        }
      }

      initNametableMemory(nametableEntries.subList(0, 32), nametable = 0)
      initNametableMemory(nametableEntries.subList(32, 64), nametable = 1)
      initBgPatternMemory(patterns)

      assertRendersAs(xCoarse = 5) { patterns[nametableEntries[(it / TILE_SIZE) + 5]]!![it % TILE_SIZE] }
    }

    @Test
    fun `fine offset`() {
      val patterns = mapOf(
        11 to List(TILE_SIZE) { 1 },
        22 to List(TILE_SIZE) { 2 },
        33 to List(TILE_SIZE) { 3 },
        44 to List(TILE_SIZE) { 0 }
      )

      // Needs one extra entry in the second nametable
      val nametableEntries = List(NUM_TILE_COLUMNS * 2) {
        when (it) {
          0 -> 11    // Leftmost
          1 -> 22
          31 -> 22
          32 -> 33   // Rightmost
          else -> 44
        }
      }

      initNametableMemory(nametableEntries.subList(0, 32), nametable = 0)
      initNametableMemory(nametableEntries.subList(32, 64), nametable = 1)
      initBgPatternMemory(patterns)

      assertRendersAs(xFine = 5) { patterns[nametableEntries[(it + 5) / TILE_SIZE]]!![(it + 5) % TILE_SIZE] }
    }

    // TODO - ensure we do minimal memory loads
  }


  private fun assertRendersAs(
    xCoarse: Int = 0,
    xFine: Int = 0,
    yCoarse: Int = this.yCoarse,
    yFine: Int = this.yFine,
    isLargeSprites: Boolean = false,
    expected: (Int) -> Int
  ) {
    val y = (yCoarse * TILE_SIZE) + yFine

    render(xCoarse, xFine, yCoarse, yFine, isLargeSprites)

    assertEquals(
      (0 until SCREEN_WIDTH).map { colors[paletteEntries[expected(it)]] },
      videoBuffer.array().toList().subList(y * SCREEN_WIDTH, (y + 1) * SCREEN_WIDTH)
    )
  }

  private fun render(
    xCoarse: Int = 0,
    xFine: Int = 0,
    yCoarse: Int = this.yCoarse,
    yFine: Int = this.yFine,
    isLargeSprites: Boolean = false
  ) = renderer.renderScanlineAndDetectHit(Context(
    isLargeSprites = isLargeSprites,
    bgPatternTable = bgPatternTable,
    sprPatternTable = sprPatternTable,
    coords = Coords(
      xCoarse = xCoarse,
      xFine = xFine,
      yCoarse = yCoarse,
      yFine = yFine
    )
  ))

  private fun initNametableMemory(
    nametableEntries: List<Int>,
    nametable: Int = this.nametable,
    yCoarse: Int = this.yCoarse
  ) {
    nametableEntries.forEachIndexed { idx, data ->
      whenever(memory.load(
        BASE_NAMETABLES + (nametable * 0x400) + (yCoarse * NUM_TILE_COLUMNS) + idx
      )) doReturn data
    }
  }

  private fun initAttributeMemory(
    attrEntries: List<Int>,
    nametable: Int = this.nametable,
    yCoarse: Int = this.yCoarse
  ) {
    attrEntries.chunked(2).forEachIndexed { idx, data ->
      val attr = if ((yCoarse % 4) / 2 == 0) {
        (data[1] shl 2) or (data[0] shl 0)
      } else {
        (data[1] shl 6) or (data[0] shl 4)
      }
      whenever(memory.load(
        BASE_NAMETABLES + (nametable * 0x400) + (NUM_TILE_COLUMNS * NUM_TILE_ROWS) + ((yCoarse / 4) * (NUM_METATILE_COLUMNS / 2)) + idx)
      ) doReturn attr
    }
  }

  private fun initBgPatternMemory(patterns: Map<Int, List<Int>>, yFine: Int = this.yFine) {
    initPatternMemory(patterns, yFine, bgPatternTable * 0x1000)
  }

  private fun initSprPatternMemory(patterns: Map<Int, List<Int>>, yFine: Int) {
    initPatternMemory(patterns, yFine, sprPatternTable * 0x1000)
  }

  private fun initPatternMemory(patterns: Map<Int, List<Int>>, yFine: Int, baseAddr: Address) {
    patterns.forEach { (idx, v) ->
      var lo = 0
      var hi = 0
      v.forEach {
        lo = (lo shl 1) or (it and 1)
        hi = (hi shl 1) or ((it / 2) and 1)
      }
      whenever(memory.load(baseAddr + (idx * PATTERN_SIZE_BYTES) + yFine)) doReturn lo
      whenever(memory.load(baseAddr + (idx * PATTERN_SIZE_BYTES) + yFine + TILE_SIZE)) doReturn hi
    }
  }

  private fun initSpriteMemory(x: Int, y: Int, iPattern: Int, attrs: Int, iSprite: Int = 0) {
    val base = iSprite * SPRITE_SIZE_BYTES
    whenever(oam.load(base + 0)) doReturn y - 1
    whenever(oam.load(base + 1)) doReturn iPattern
    whenever(oam.load(base + 2)) doReturn attrs
    whenever(oam.load(base + 3)) doReturn x
  }
}
