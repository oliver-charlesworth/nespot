package choliver.nespot.ppu

import choliver.nespot.Address
import choliver.nespot.Memory
import com.nhaarman.mockitokotlin2.doReturn
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.whenever
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import java.nio.IntBuffer

// TODO - greyscale
// TODO - colour emphasis
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
  private val screen = IntBuffer.allocate(SCREEN_WIDTH * SCREEN_HEIGHT)
  private val renderer = Renderer(
    memory = memory,
    palette = palette,
    oam = oam,
    screen = screen,
    colors = colors
  )

  private val nametableAddr = 0x2000
  private val bgPatternTable = 1
  private val sprPatternTable = 0

  // Chosen scanline
  private val yTile = 14
  private val yPixel = 4

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

      initAttributeMemory(attrEntries, yTile = 13)
      initBgPatternMemory(mapOf(0 to pattern))

      assertRendersAs(yTile = 13) { 1 + attrEntries[it / METATILE_SIZE] * NUM_ENTRIES_PER_PALETTE }
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
      initSprPatternMemory(mapOf(1 to pattern), yRow = 0)
      initSpriteMemory(x = xOffset, y = (yTile * TILE_SIZE) + yPixel, iPattern = 1, attrs = 0)

      assertRendersAs { calcPalIdx(x = it, xOffset = xOffset, iPalette = 0, pattern = pattern) }
    }

    @Test
    fun `bottom row`() {
      initSprPatternMemory(mapOf(1 to pattern), yRow = 7)
      initSpriteMemory(x = xOffset, y = (yTile * TILE_SIZE) + yPixel - 7, iPattern = 1, attrs = 0)

      assertRendersAs { calcPalIdx(x = it, xOffset = xOffset, iPalette = 0, pattern = pattern) }
    }

    @Test
    fun `horizontally-flipped`() {
      initSprPatternMemory(mapOf(1 to pattern), yRow = 0)
      initSpriteMemory(x = xOffset, y = (yTile * TILE_SIZE) + yPixel, iPattern = 1, attrs = 0x40)

      assertRendersAs { calcPalIdx(x = it, xOffset = xOffset, iPalette = 0, pattern = pattern.reversed()) }
    }

    @Test
    fun `vertically-flipped`() {
      initSprPatternMemory(mapOf(1 to pattern), yRow = 7)
      initSpriteMemory(x = xOffset, y = (yTile * TILE_SIZE) + yPixel, iPattern = 1, attrs = 0x80)

      assertRendersAs { calcPalIdx(x = it, xOffset = xOffset, iPalette = 0, pattern = pattern) }
    }

    @Test
    fun `with different palette`() {
      initSprPatternMemory(mapOf(1 to pattern), yRow = 0)
      initSpriteMemory(x = xOffset, y = (yTile * TILE_SIZE) + yPixel, iPattern = 1, attrs = 2)

      assertRendersAs { calcPalIdx(x = it, xOffset = xOffset, iPalette = 2, pattern = pattern) }
    }

    @Test
    fun `off the right-hand-edge`() {
      initSprPatternMemory(mapOf(1 to pattern), yRow = 0)
      initSpriteMemory(x = 252, y = (yTile * TILE_SIZE) + yPixel, iPattern = 1, attrs = 0)

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
      initSpriteMemory(x = xOffset, y = (yTile * TILE_SIZE) + yPixel, iPattern = 2, attrs = 0)

      assertRendersAs(isLargeSprites = true) { calcPalIdx(x = it) }
    }

    @Test
    fun `bottom row`() {
      initSprPatternMemory(mapOf(3 to pattern), yRow = 7) // Note - next pattern index!
      initSpriteMemory(x = xOffset, y = (yTile * TILE_SIZE) + yPixel - 15, iPattern = 2, attrs = 0)

      assertRendersAs(isLargeSprites = true) { calcPalIdx(x = it) }
    }

    @Test
    fun `top row - vertically-flipped`() {
      initSprPatternMemory(mapOf(3 to pattern), yRow = 7) // Note - next pattern index!
      initSpriteMemory(x = xOffset, y = (yTile * TILE_SIZE) + yPixel, iPattern = 2, attrs = 0x80)

      assertRendersAs(isLargeSprites = true) { calcPalIdx(x = it) }
    }

    @Test
    fun `bottom row - vertically-flipped`() {
      initSprPatternMemory(mapOf(2 to pattern), yRow = 0)
      initSpriteMemory(x = xOffset, y = (yTile * TILE_SIZE) + yPixel - 15, iPattern = 2, attrs = 0x80)

      assertRendersAs(isLargeSprites = true) { calcPalIdx(x = it) }
    }

    @Test
    fun `ignore sprPatternTable`() {
      val largeIdx = 258  // Larger than one palette table
      initSprPatternMemory(mapOf(largeIdx to pattern), yRow = 0)
      initSpriteMemory(
        x = xOffset,
        y = (yTile * TILE_SIZE) + yPixel,
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
      val y = (yTile * TILE_SIZE) + yPixel

      initAttributeMemory(List(NUM_METATILE_COLUMNS) { paletteBg })
      initBgPatternMemory(mapOf(0 to List(TILE_SIZE) { if (bgOpaque) 1 else 0 }))
      initSprPatternMemory(mapOf(1 to List(TILE_SIZE) { if (sprOpaque) 1 else 0 }), yRow = 0)
      initSpriteMemory(
        x = 0,
        y = y,
        iPattern = 1,
        attrs = (if (sprBehind) 0x20 else 0x00) or paletteSpr
      )

      render()

      assertEquals(colors[paletteEntries[expected]], screen.array()[y * SCREEN_WIDTH])
    }
  }

  @Nested
  inner class Collisions {
    @Test
    fun `triggers hit if opaque sprite #0 overlaps opaque background`() {
      initBgPatternMemory(mapOf(0 to listOf(1, 1, 1, 1, 1, 1, 1, 1)))
      initSprPatternMemory(mapOf(1 to listOf(1, 0, 0, 0, 0, 0, 0, 0)), yRow = 0)
      initSpriteMemory(x = 5, y = (yTile * TILE_SIZE) + yPixel, iPattern = 1, attrs = 0)

      assertTrue(render())
    }

    @Test
    fun `doesn't trigger hit if opaque sprite #0 overlaps transparent background`() {
      initBgPatternMemory(mapOf(0 to listOf(0, 0, 0, 0, 0, 0, 0, 0)))
      initSprPatternMemory(mapOf(1 to listOf(1, 0, 0, 0, 0, 0, 0, 0)), yRow = 0)
      initSpriteMemory(x = 5, y = (yTile * TILE_SIZE) + yPixel, iPattern = 1, attrs = 0)

      assertFalse(render())
    }

    @Test
    fun `doesn't trigger hit if transparent sprite #0 overlaps opaque background`() {
      initBgPatternMemory(mapOf(0 to listOf(1, 1, 1, 1, 1, 1, 1, 1)))
      initSprPatternMemory(mapOf(1 to listOf(0, 0, 0, 0, 0, 0, 0, 0)), yRow = 0)
      initSpriteMemory(x = 5, y = (yTile * TILE_SIZE) + yPixel, iPattern = 1, attrs = 0)

      assertFalse(render())
    }

    @Test
    fun `triggers single hit even when multiple sprite #0 overlaps`() {
      initBgPatternMemory(mapOf(0 to listOf(1, 1, 1, 1, 1, 1, 1, 1)))
      initSprPatternMemory(mapOf(1 to listOf(1, 0, 1, 0, 1, 0, 1, 0)), yRow = 0)
      initSpriteMemory(x = 5, y = (yTile * TILE_SIZE) + yPixel, iPattern = 1, attrs = 0)

      assertTrue(render())
    }

    @Test
    fun `doesn't trigger from overlap from sprites other than #0`() {
      initBgPatternMemory(mapOf(0 to listOf(1, 1, 1, 1, 1, 1, 1, 1)))
      initSprPatternMemory(mapOf(1 to listOf(1, 0, 0, 0, 0, 0, 0, 0)), yRow = 0)
      initSpriteMemory(x = 5, y = (yTile * TILE_SIZE) + yPixel, iPattern = 1, attrs = 0, iSprite = 1)

      assertFalse(render())
    }

    @Test
    fun `doesn't trigger from overlap at x == 255`() {
      initBgPatternMemory(mapOf(0 to listOf(1, 1, 1, 1, 1, 1, 1, 1)))
      initSprPatternMemory(mapOf(1 to listOf(1, 0, 0, 0, 0, 0, 0, 0)), yRow = 0)
      initSpriteMemory(x = 255, y = (yTile * TILE_SIZE) + yPixel, iPattern = 1, attrs = 0)

      assertFalse(render())
    }
    @Test
    fun `triggers hit from behind`() {
      initBgPatternMemory(mapOf(0 to listOf(1, 1, 1, 1, 1, 1, 1, 1)))
      initSprPatternMemory(mapOf(1 to listOf(1, 0, 0, 0, 0, 0, 0, 0)), yRow = 0)
      initSpriteMemory(x = 5, y = (yTile * TILE_SIZE) + yPixel, iPattern = 1, attrs = 0x20) // Behind

      assertTrue(render())
    }

    // TODO - not detected in active clipping region (background or sprite)
  }

  private fun assertRendersAs(
    yTile: Int = this.yTile,
    yPixel: Int = this.yPixel,
    isLargeSprites: Boolean = false,
    expected: (Int) -> Int
  ) {
    val y = (yTile * TILE_SIZE) + yPixel

    render(yTile, yPixel, isLargeSprites)

    assertEquals(
      (0 until SCREEN_WIDTH).map { colors[paletteEntries[expected(it)]] },
      screen.array().toList().subList(y * SCREEN_WIDTH, (y + 1) * SCREEN_WIDTH)
    )
  }

  private fun render(
    yTile: Int = this.yTile,
    yPixel: Int = this.yPixel,
    isLargeSprites: Boolean = false
  ) = renderer.renderScanlineAndDetectHit(
    y = (yTile * TILE_SIZE) + yPixel,
    ctx = Renderer.Context(
      isLargeSprites = isLargeSprites,
      nametableAddr = nametableAddr,
      bgPatternTable = bgPatternTable,
      sprPatternTable = sprPatternTable,
      scrollX = 0,
      scrollY = 0
    )
  )

  private fun initNametableMemory(nametableEntries: List<Int>, yTile: Int = this.yTile) {
    nametableEntries.forEachIndexed { idx, data ->
      whenever(memory.load(nametableAddr + (yTile * NUM_TILE_COLUMNS) + idx)) doReturn data
    }
  }

  private fun initAttributeMemory(attrEntries: List<Int>, yTile: Int = this.yTile) {
    attrEntries.chunked(2).forEachIndexed { idx, data ->
      val attr = if ((yTile % 4) / 2 == 0) {
        (data[1] shl 2) or (data[0] shl 0)
      } else {
        (data[1] shl 6) or (data[0] shl 4)
      }
      whenever(memory.load(
        nametableAddr + (NUM_TILE_COLUMNS * NUM_TILE_ROWS) + ((yTile / 4) * (NUM_METATILE_COLUMNS / 2)) + idx)
      ) doReturn attr
    }
  }

  private fun initBgPatternMemory(patterns: Map<Int, List<Int>>, yRow: Int = this.yPixel) {
    initPatternMemory(patterns, yRow, bgPatternTable * 0x1000)
  }

  private fun initSprPatternMemory(patterns: Map<Int, List<Int>>, yRow: Int) {
    initPatternMemory(patterns, yRow, sprPatternTable * 0x1000)
  }

  private fun initPatternMemory(patterns: Map<Int, List<Int>>, yRow: Int, baseAddr: Address) {
    patterns.forEach { (idx, v) ->
      var lo = 0
      var hi = 0
      v.forEach {
        lo = (lo shl 1) or (it and 1)
        hi = (hi shl 1) or ((it / 2) and 1)
      }
      whenever(memory.load(baseAddr + (idx * PATTERN_SIZE_BYTES) + yRow)) doReturn lo
      whenever(memory.load(baseAddr + (idx * PATTERN_SIZE_BYTES) + yRow + TILE_SIZE)) doReturn hi
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
