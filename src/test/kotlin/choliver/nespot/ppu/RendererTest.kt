package choliver.nespot.ppu

import choliver.nespot.Address
import choliver.nespot.Memory
import choliver.nespot.apu.repeat
import choliver.nespot.ppu.Ppu.Companion.BASE_NAMETABLES
import choliver.nespot.ppu.model.Coords
import choliver.nespot.ppu.model.State
import com.nhaarman.mockitokotlin2.*
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import java.nio.IntBuffer


// TODO - separate test for commit-to-buffer
class RendererTest {
  private val colors = (0..63).toList()
  private val paletteEntries = (0..31).map { it + 5 }

  private val memory = mock<Memory>()
  private val palette = mock<Memory> {
    paletteEntries.forEachIndexed { idx, data ->
      on { get(idx) } doReturn data
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

  @Nested
  inner class Background {
    @Test
    fun `patterns for palette #0`() {
      val pattern = listOf(0, 1, 2, 3, 2, 3, 0, 1)
      initBgPatternMemory(mapOf(0 to pattern))

      render()

      assertBuffer { pattern[it % TILE_SIZE] }
    }

    @Test
    fun `patterns for higher palettes use universal background color`() {
      val pattern = listOf(0, 1, 2, 3, 2, 3, 0, 1)
      val attrEntries = List(NUM_METATILE_COLUMNS) { 1 }  // Arbitrary non-zero palette #
      initAttributeMemory(attrEntries)
      initBgPatternMemory(mapOf(0 to pattern))

      render()

      assertBuffer { pattern[it % TILE_SIZE].let { if (it == 0) 0 else (it + NUM_ENTRIES_PER_PALETTE) } }
    }

    @Test
    fun `location-based attributes - bottom metatile`() {
      val pattern = List(TILE_SIZE) { 1 } // Arbitrary non-zero pixel
      val attrEntries = listOf(0, 1, 2, 3, 2, 3, 0, 1, 3, 2, 1, 0, 1, 0, 3, 2)
      initAttributeMemory(attrEntries)
      initBgPatternMemory(mapOf(0 to pattern))

      render()

      assertBuffer { 1 + attrEntries[it / METATILE_SIZE] * NUM_ENTRIES_PER_PALETTE }
    }

    @Test
    fun `location-based attributes - top metatile`() {
      val pattern = List(TILE_SIZE) { 1 } // Arbitrary non-zero pixel
      val attrEntries = listOf(0, 1, 2, 3, 2, 3, 0, 1, 3, 2, 1, 0, 1, 0, 3, 2)
      initAttributeMemory(attrEntries, yTile = 13)
      initBgPatternMemory(mapOf(0 to pattern))

      render(yCoarse = 13)

      assertBuffer { 1 + attrEntries[it / METATILE_SIZE] * NUM_ENTRIES_PER_PALETTE }
    }

    @Test
    fun `location-based patterns`() {
      initNametableMemory(listOf(11, 22, 33, 44).repeat(NUM_TILE_COLUMNS / 4))
      initBgPatternMemory(mapOf(
        11 to List(TILE_SIZE) { 1 },
        22 to List(TILE_SIZE) { 2 },
        33 to List(TILE_SIZE) { 3 },
        44 to List(TILE_SIZE) { 0 }
      ))

      render()

      assertBuffer { (it / TILE_SIZE + 1) % 4 }
    }

    @Test
    fun `not rendered if disabled`() {
      val pattern = listOf(0, 1, 2, 3, 2, 3, 0, 1)
      initBgPatternMemory(mapOf(0 to pattern))

      render(bgEnabled = false)

      assertBuffer { 0 }
    }

    @Test
    fun `not rendered if clipped`() {
      // Use varying tiles to demonstrate that coordinates are incremented during clipping
      initNametableMemory(listOf(11, 22, 33, 44).repeat(NUM_TILE_COLUMNS / 4))
      initBgPatternMemory(mapOf(
        11 to List(TILE_SIZE) { 1 },
        22 to List(TILE_SIZE) { 2 },
        33 to List(TILE_SIZE) { 3 },
        44 to List(TILE_SIZE) { 0 }
      ))

      render(bgLeftTileEnabled = false)

      assertBuffer { if (it < TILE_SIZE) 0 else (it / TILE_SIZE + 1) % 4 }
    }

    private fun render(
      bgEnabled: Boolean = true,
      bgLeftTileEnabled: Boolean = true,
      yCoarse: Int = Y_COARSE
    ) {
      val state = State(
        bgEnabled = bgEnabled,
        bgLeftTileEnabled = bgLeftTileEnabled,
        bgPatternTable = BG_PATTERN_TABLE,
        coords = Coords(xCoarse = 0, xFine = 0, yCoarse = yCoarse, yFine = Y_FINE)
      )
      renderer.renderBackground(state)
    }
  }

  @Nested
  inner class Sprite {
    private val xOffset = 5
    private val pattern = listOf(1, 2, 3, 3, 2, 1, 1, 2)  // No zero values

    @Test
    fun `top row`() {
      initSprPatternMemory(mapOf(1 to pattern), yRow = 0)
      initSpriteMemory(x = xOffset, y = Y_SCANLINE, iPattern = 1, attrs = 0)

      render()

      assertBuffer { calcPalIdx(x = it, xOffset = xOffset, iPalette = 0, pattern = pattern) }
    }

    @Test
    fun `bottom row`() {
      initSprPatternMemory(mapOf(1 to pattern), yRow = 7)
      initSpriteMemory(x = xOffset, y = Y_SCANLINE - 7, iPattern = 1, attrs = 0)

      render()

      assertBuffer { calcPalIdx(x = it, xOffset = xOffset, iPalette = 0, pattern = pattern) }
    }

    @Test
    fun `horizontally-flipped`() {
      initSprPatternMemory(mapOf(1 to pattern), yRow = 0)
      initSpriteMemory(x = xOffset, y = Y_SCANLINE, iPattern = 1, attrs = 0x40)

      render()

      assertBuffer { calcPalIdx(x = it, xOffset = xOffset, iPalette = 0, pattern = pattern.reversed()) }
    }

    @Test
    fun `vertically-flipped`() {
      initSprPatternMemory(mapOf(1 to pattern), yRow = 7)
      initSpriteMemory(x = xOffset, y = Y_SCANLINE, iPattern = 1, attrs = 0x80)

      render()

      assertBuffer { calcPalIdx(x = it, xOffset = xOffset, iPalette = 0, pattern = pattern) }
    }

    @Test
    fun `with different palette`() {
      initSprPatternMemory(mapOf(1 to pattern), yRow = 0)
      initSpriteMemory(x = xOffset, y = Y_SCANLINE, iPattern = 1, attrs = 2)

      render()

      assertBuffer { calcPalIdx(x = it, xOffset = xOffset, iPalette = 2, pattern = pattern) }
    }

    @Test
    fun `partially off the right-hand-edge`() {
      initSprPatternMemory(mapOf(1 to pattern), yRow = 0)
      initSpriteMemory(x = 252, y = Y_SCANLINE, iPattern = 1, attrs = 0)

      render()

      assertBuffer { calcPalIdx(x = it, xOffset = 252, iPalette = 0, pattern = pattern) }
    }

    @Test
    fun `not rendered if disabled`() {
      initSprPatternMemory(mapOf(1 to pattern), yRow = 0)
      initSpriteMemory(x = xOffset, y = Y_SCANLINE, iPattern = 1, attrs = 0)

      render(sprEnabled = false)

      assertBuffer { 0 }
    }

    @Test
    fun `not rendered if clipped`() {
      initSprPatternMemory(mapOf(1 to pattern), yRow = 0)
      initSpriteMemory(x = xOffset, y = Y_SCANLINE, iPattern = 1, attrs = 0)

      render(sprLeftTileEnabled = false)

      assertBuffer { if (it < TILE_SIZE) 0 else calcPalIdx(x = it, xOffset = xOffset, iPalette = 0, pattern = pattern) }
    }

    private fun calcPalIdx(x: Int, xOffset: Int, iPalette: Int, pattern: List<Int>) =
      if (x in xOffset until TILE_SIZE + xOffset) {
        pattern[x - xOffset] + ((NUM_PALETTES + iPalette) * NUM_ENTRIES_PER_PALETTE)
      } else 0

    private fun render(
      sprEnabled: Boolean = true,
      sprLeftTileEnabled: Boolean = true
    ) {
      val state = State(
        sprEnabled = sprEnabled,
        sprLeftTileEnabled = sprLeftTileEnabled,
        sprPatternTable = SPR_PATTERN_TABLE,
        scanline = Y_SCANLINE
      )
      renderer.evaluateSprites(state)
      renderer.loadSprites(state)
      renderer.renderSprites(state)
      renderer.commitToBuffer(state)
    }
  }

  @Nested
  inner class LargeSprite {
    private val iPalette = 0
    private val xOffset = 5
    private val pattern = listOf(1, 2, 3, 3, 2, 1, 1, 2)  // No zero values

    @Test
    fun `top row`() {
      initSprPatternMemory(mapOf(2 to pattern), yRow = 0)
      initSpriteMemory(x = xOffset, y = Y_SCANLINE, iPattern = 2, attrs = 0)

      render()

      assertBuffer { calcPalIdx(x = it) }
    }

    @Test
    fun `bottom row`() {
      initSprPatternMemory(mapOf(3 to pattern), yRow = 7) // Note - next pattern index!
      initSpriteMemory(x = xOffset, y = Y_SCANLINE - 15, iPattern = 2, attrs = 0)

      render()

      assertBuffer { calcPalIdx(x = it) }
    }

    @Test
    fun `top row - vertically-flipped`() {
      initSprPatternMemory(mapOf(3 to pattern), yRow = 7) // Note - next pattern index!
      initSpriteMemory(x = xOffset, y = Y_SCANLINE, iPattern = 2, attrs = 0x80)

      render()

      assertBuffer { calcPalIdx(x = it) }
    }

    @Test
    fun `bottom row - vertically-flipped`() {
      initSprPatternMemory(mapOf(2 to pattern), yRow = 0)
      initSpriteMemory(x = xOffset, y = Y_SCANLINE - 15, iPattern = 2, attrs = 0x80)

      render()

      assertBuffer { calcPalIdx(x = it) }
    }

    @Test
    fun `ignore sprPatternTable`() {
      val largeIdx = 258  // Larger than one palette table
      initSprPatternMemory(mapOf(largeIdx to pattern), yRow = 0)
      initSpriteMemory(
        x = xOffset,
        y = Y_SCANLINE,
        iPattern = (largeIdx and 0xFE) or (largeIdx shr 8), // Munged to fit in one byte
        attrs = 0
      )

      render()

      assertBuffer { calcPalIdx(x = it) }
    }

    private fun calcPalIdx(x: Int) = if (x in xOffset until TILE_SIZE + xOffset) {
      pattern[x - xOffset] + ((NUM_PALETTES + iPalette) * NUM_ENTRIES_PER_PALETTE)
    } else 0

    private fun initSprPatternMemory(patterns: Map<Int, List<Int>>, yRow: Int) {
      initPatternMemory(patterns, yRow, 0x0000)
    }

    private fun render() {
      val state = State(
        largeSprites = true,
        sprEnabled = true,
        sprLeftTileEnabled = true,
        sprPatternTable = SPR_PATTERN_TABLE,
        scanline = Y_SCANLINE
      )
      renderer.evaluateSprites(state)
      renderer.loadSprites(state)
      renderer.renderSprites(state)
      renderer.commitToBuffer(state)
    }
  }

  @Nested
  inner class Priority {
    private val paletteSpr = 1  // Non-zero palette
    private val paletteSpr2 = 3 // Non-zero palette
    private val paletteBg = 2   // Non-zero palette different to that of sprites

    private val ubg = 0
    private val bgEntry = 1 + (paletteBg * NUM_ENTRIES_PER_PALETTE)
    private val sprEntry = 1 + ((NUM_PALETTES + paletteSpr) * NUM_ENTRIES_PER_PALETTE)
    private val sprEntry2 = 1 + ((NUM_PALETTES + paletteSpr2) * NUM_ENTRIES_PER_PALETTE)

    @Test
    fun `transparent bg, transparent spr, in front - ubg`() {
      initBackground(false)
      initSprite(behind = false, opaque = false, palette = paletteSpr)

      assertPixelColour(expected = ubg)
    }

    @Test
    fun `transparent bg, transparent spr, behind - ubg`() {
      initBackground(false)
      initSprite(behind = true, opaque = false, palette = paletteSpr)

      assertPixelColour(expected = ubg)
    }

    @Test
    fun `transparent bg, opaque spr, in front - spr`() {
      initBackground(false)
      initSprite(behind = false, opaque = true, palette = paletteSpr)

      assertPixelColour(expected = sprEntry)
    }

    @Test
    fun `transparent bg, opaque spr, behind - spr`() {
      initBackground(false)
      initSprite(behind = true, opaque = true, palette = paletteSpr)

      assertPixelColour(expected = sprEntry)
    }

    @Test
    fun `opaque bg, transparent spr, in front - bg`() {
      initBackground(true)
      initSprite(behind = false, opaque = false, palette = paletteSpr)

      assertPixelColour(expected = bgEntry)
    }

    @Test
    fun `opaque bg, transparent spr, behind - bg`() {
      initBackground(true)
      initSprite(behind = true, opaque = false, palette = paletteSpr)

      assertPixelColour(expected = bgEntry)
    }

    @Test
    fun `opaque bg, opaque spr, in front - spr`() {
      initBackground(true)
      initSprite(behind = false, opaque = true, palette = paletteSpr)

      assertPixelColour(expected = sprEntry)
    }

    @Test
    fun `opaque bg, opaque spr, behind - bg`() {
      initBackground(true)
      initSprite(behind = true, opaque = true, palette = paletteSpr)

      assertPixelColour(expected = bgEntry)
    }

    @Test
    fun `lowest-index opaque in-front sprite wins`() {
      initBackground(true)
      initSprite(behind = false, opaque = true, palette = paletteSpr, iSprite = 0)
      initSprite(behind = false, opaque = true, palette = paletteSpr2, iSprite = 1)

      assertPixelColour(expected = sprEntry)
    }

    @Test
    fun `lowest-index opaque behind sprite wins`() {
      initBackground(false)
      initSprite(behind = true, opaque = true, palette = paletteSpr, iSprite = 0)
      initSprite(behind = true, opaque = true, palette = paletteSpr2, iSprite = 1)

      assertPixelColour(expected = sprEntry)
    }

    @Test
    fun `lower-index opaque behind sprite wins over opaque in-front sprite`() {
      initBackground(true)
      initSprite(behind = true, opaque = true, palette = paletteSpr, iSprite = 0)
      initSprite(behind = false, opaque = true, palette = paletteSpr2, iSprite = 1)

      assertPixelColour(expected = bgEntry) // Behind sprite wins, so we see the background!
    }

    @Test
    fun `lower-index transparent behind sprite doesn't win over opaque in-front sprite`() {
      initBackground(true)
      initSprite(behind = true, opaque = false, palette = paletteSpr, iSprite = 0)
      initSprite(behind = false, opaque = true, palette = paletteSpr2, iSprite = 1)

      assertPixelColour(expected = sprEntry2)
    }

    private fun initSprite(iSprite: Int = 0, behind: Boolean, opaque: Boolean, palette: Int) {
      initSpriteMemory(
        x = 0,
        y = Y_SCANLINE,
        iPattern = iSprite,
        attrs = (if (behind) 0x20 else 0x00) or palette,
        iSprite = iSprite
      )
      initSprPatternMemory(mapOf(iSprite to List(TILE_SIZE) { if (opaque) 1 else 0 }), yRow = 0)
    }

    private fun initBackground(opaque: Boolean) {
      initAttributeMemory(List(NUM_METATILE_COLUMNS) { paletteBg })
      initBgPatternMemory(mapOf(0 to List(TILE_SIZE) { if (opaque) 1 else 0 }))
    }

    private fun assertPixelColour(expected: Int) {
      render()

      assertEquals(colors[paletteEntries[expected]], videoBuffer.array()[Y_SCANLINE * SCREEN_WIDTH])
    }

    private fun render() {
      val state = State(
        bgEnabled = true,
        bgLeftTileEnabled = true,
        bgPatternTable = BG_PATTERN_TABLE,
        sprEnabled = true,
        sprLeftTileEnabled = true,
        sprPatternTable = SPR_PATTERN_TABLE,
        coords = Coords(xCoarse = 0, xFine = 0, yCoarse = Y_COARSE, yFine = Y_FINE),
        scanline = Y_SCANLINE
      )
      renderer.renderBackground(state)
      renderer.evaluateSprites(state)
      renderer.loadSprites(state)
      renderer.renderSprites(state)
      renderer.commitToBuffer(state)
    }
  }

  @Nested
  inner class Collisions {
    @Test
    fun `triggers hit if opaque sprite #0 overlaps opaque background`() {
      initBgPatternMemory(mapOf(0 to listOf(1, 1, 1, 1, 1, 1, 1, 1)))
      initSprPatternMemory(mapOf(1 to listOf(1, 0, 0, 0, 0, 0, 0, 0)), yRow = 0)
      initSpriteMemory(x = 5, y = Y_SCANLINE, iPattern = 1, attrs = 0)

      assertTrue(render().sprite0Hit)
    }

    @Test
    fun `doesn't trigger hit if opaque sprite #0 overlaps transparent background`() {
      initBgPatternMemory(mapOf(0 to listOf(0, 0, 0, 0, 0, 0, 0, 0)))
      initSprPatternMemory(mapOf(1 to listOf(1, 0, 0, 0, 0, 0, 0, 0)), yRow = 0)
      initSpriteMemory(x = 5, y = Y_SCANLINE, iPattern = 1, attrs = 0)

      assertFalse(render().sprite0Hit)
    }

    @Test
    fun `doesn't trigger hit if transparent sprite #0 overlaps opaque background`() {
      initBgPatternMemory(mapOf(0 to listOf(1, 1, 1, 1, 1, 1, 1, 1)))
      initSprPatternMemory(mapOf(1 to listOf(0, 0, 0, 0, 0, 0, 0, 0)), yRow = 0)
      initSpriteMemory(x = 5, y = Y_SCANLINE, iPattern = 1, attrs = 0)

      assertFalse(render().sprite0Hit)
    }

    @Test
    fun `triggers single hit even when multiple sprite #0 overlaps`() {
      initBgPatternMemory(mapOf(0 to listOf(1, 1, 1, 1, 1, 1, 1, 1)))
      initSprPatternMemory(mapOf(1 to listOf(1, 0, 1, 0, 1, 0, 1, 0)), yRow = 0)
      initSpriteMemory(x = 5, y = Y_SCANLINE, iPattern = 1, attrs = 0)

      assertTrue(render().sprite0Hit)
    }

    @Test
    fun `doesn't trigger from overlap from sprites other than #0`() {
      initBgPatternMemory(mapOf(0 to listOf(1, 1, 1, 1, 1, 1, 1, 1)))
      initSprPatternMemory(mapOf(1 to listOf(1, 0, 0, 0, 0, 0, 0, 0)), yRow = 0)
      initSpriteMemory(x = 5, y = Y_SCANLINE, iPattern = 1, attrs = 0, iSprite = 1)

      assertFalse(render().sprite0Hit)
    }

    @Test
    fun `doesn't trigger from overlap at x == 255`() {
      initBgPatternMemory(mapOf(0 to listOf(1, 1, 1, 1, 1, 1, 1, 1)))
      initSprPatternMemory(mapOf(1 to listOf(1, 0, 0, 0, 0, 0, 0, 0)), yRow = 0)
      initSpriteMemory(x = 255, y = Y_SCANLINE, iPattern = 1, attrs = 0)

      assertFalse(render().sprite0Hit)
    }

    @Test
    fun `triggers hit from behind`() {
      initBgPatternMemory(mapOf(0 to listOf(1, 1, 1, 1, 1, 1, 1, 1)))
      initSprPatternMemory(mapOf(1 to listOf(1, 0, 0, 0, 0, 0, 0, 0)), yRow = 0)
      initSpriteMemory(x = 5, y = Y_SCANLINE, iPattern = 1, attrs = 0x20) // Behind

      assertTrue(render().sprite0Hit)
    }

    @Test
    fun `doesn't trigger hit if rendering disabled`() {
      initBgPatternMemory(mapOf(0 to listOf(1, 1, 1, 1, 1, 1, 1, 1)))
      initSprPatternMemory(mapOf(1 to listOf(1, 0, 0, 0, 0, 0, 0, 0)), yRow = 0)
      initSpriteMemory(x = 5, y = Y_SCANLINE, iPattern = 1, attrs = 0)

      assertFalse(render(sprEnabled = false).sprite0Hit)
      assertFalse(render(bgEnabled = false).sprite0Hit)
    }

    @Test
    fun `doesn't trigger hit if clipped`() {
      initBgPatternMemory(mapOf(0 to listOf(1, 1, 1, 1, 1, 1, 1, 1)))
      initSprPatternMemory(mapOf(1 to listOf(1, 0, 0, 0, 0, 0, 0, 0)), yRow = 0)
      initSpriteMemory(x = 5, y = Y_SCANLINE, iPattern = 1, attrs = 0)

      assertFalse(render(sprLeftTileEnabled = false).sprite0Hit)
      assertFalse(render(bgLeftTileEnabled = false).sprite0Hit)
    }

    @Test
    fun `doesn't trigger hit if opaque sprite overlaps opaque sprite`() {
      initBgPatternMemory(mapOf(0 to listOf(0, 0, 0, 0, 0, 0, 0, 0)))
      initSprPatternMemory(mapOf(1 to listOf(1, 0, 0, 0, 0, 0, 0, 0)), yRow = 0)
      initSpriteMemory(x = 5, y = Y_SCANLINE, iPattern = 1, attrs = 0, iSprite = 0)
      initSpriteMemory(x = 5, y = Y_SCANLINE, iPattern = 1, attrs = 0, iSprite = 1)

      assertFalse(render().sprite0Hit)
    }

    private fun render(
      bgEnabled: Boolean = true,
      sprEnabled: Boolean = true,
      bgLeftTileEnabled: Boolean = true,
      sprLeftTileEnabled: Boolean = true
    ): State {
      val state = State(
        bgEnabled = bgEnabled,
        bgLeftTileEnabled = bgLeftTileEnabled,
        bgPatternTable = BG_PATTERN_TABLE,
        sprEnabled = sprEnabled,
        sprLeftTileEnabled = sprLeftTileEnabled,
        sprPatternTable = SPR_PATTERN_TABLE,
        coords = Coords(xCoarse = 0, xFine = 0, yCoarse = Y_COARSE, yFine = Y_FINE),
        scanline = Y_SCANLINE
      )
      renderer.renderBackground(state)
      renderer.evaluateSprites(state)
      renderer.loadSprites(state)
      renderer.renderSprites(state)
      renderer.commitToBuffer(state)
      return state
    }
  }

  @Nested
  inner class Overflow {
    @Test
    fun `doesn't trigger overflow for 8 or less`() {
      repeat(8) {
        initSpriteMemory(x = 5, y = Y_SCANLINE, iPattern = 1, attrs = 0, iSprite = (it * 2) + 1)
      }

      assertFalse(render().spriteOverflow)
    }

    @Test
    fun `triggers overflow for 9 or more`() {
      repeat(9) {
        initSpriteMemory(x = 5, y = Y_SCANLINE, iPattern = 1, attrs = 0, iSprite = (it * 2) + 1)
      }

      assertTrue(render().spriteOverflow)
    }

    @Test
    fun `doesn't trigger overflow if rendering completely disabled`() {
      repeat(9) {
        initSpriteMemory(x = 5, y = Y_SCANLINE, iPattern = 1, attrs = 0, iSprite = (it * 2) + 1)
      }

      assertTrue(render(sprEnabled = false).spriteOverflow)
      assertTrue(render(bgEnabled = false).spriteOverflow)
      assertFalse(render(bgEnabled = false, sprEnabled = false).spriteOverflow)
    }

    @Test
    fun `renders at most 8 sprites, with the highest priority`() {
      val pattern = List(TILE_SIZE) { 1 }
      initSprPatternMemory(mapOf(1 to pattern), yRow = 0)
      repeat(9) {
        initSpriteMemory(x = (it * (TILE_SIZE + 1)), y = Y_SCANLINE, iPattern = 1, attrs = 0, iSprite = (it * 2) + 1)
      }

      render()

      assertEquals(8 * 8, extractScanline().count { it != paletteEntries[0] })
    }

    @Test
    fun `renders less than 8 sprites`() {
      val pattern = List(TILE_SIZE) { 1 }
      initSprPatternMemory(mapOf(1 to pattern), yRow = 0)
      repeat(6) {
        initSpriteMemory(x = (it * (TILE_SIZE + 1)), y = Y_SCANLINE, iPattern = 1, attrs = 0, iSprite = (it * 2) + 1)
      }

      render()

      assertEquals(6 * 8, extractScanline().count { it != paletteEntries[0] })
    }

    private fun render(
      bgEnabled: Boolean = true,
      sprEnabled: Boolean = true
    ): State {
      val state = State(
        bgEnabled = bgEnabled,
        sprEnabled = sprEnabled,
        sprLeftTileEnabled = true,
        sprPatternTable = SPR_PATTERN_TABLE,
        scanline = Y_SCANLINE
      )
      renderer.evaluateSprites(state)
      renderer.loadSprites(state)
      renderer.renderSprites(state)
      renderer.commitToBuffer(state)
      return state
    }
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

      render(xCoarse = 5)

      assertBuffer { patterns[nametableEntries[(it / TILE_SIZE) + 5]]!![it % TILE_SIZE] }
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

      render(xFine = 5)

      assertBuffer { patterns[nametableEntries[(it + 5) / TILE_SIZE]]!![(it + 5) % TILE_SIZE] }
    }

    private fun render(
      xCoarse: Int = 0,
      xFine: Int = 0
    ) {
      val state = State(
        bgEnabled = true,
        bgLeftTileEnabled = true,
        bgPatternTable = BG_PATTERN_TABLE,
        coords = Coords(xCoarse = xCoarse, xFine = xFine, yCoarse = Y_COARSE, yFine = Y_FINE),
        scanline = Y_SCANLINE
      )
      renderer.renderBackground(state)
      renderer.commitToBuffer(state)
    }
  }

  // MMC3 relies on specific PPU memory access pattern during scanline
  @Nested
  inner class MemoryAccessPattern {
    @Test
    fun `performs dummy loads from pattern table #1 for invalid sprites`() {
      // Note - no valid sprites at all!
      render()

      verify(memory, times(8))[0x1FF0]
      verify(memory, times(8))[0x1FF8]
    }

    @Test
    fun `performs no sprite loads if sprite rendering disabled`() {
      render(sprEnabled = false)

      verifyZeroInteractions(memory)
    }

    private fun render(
      sprEnabled: Boolean = true
    ) {
      val state = State(
        sprEnabled = sprEnabled,
        sprLeftTileEnabled = true,
        sprPatternTable = SPR_PATTERN_TABLE,
        scanline = Y_SCANLINE
      )
      renderer.evaluateSprites(state)
      renderer.loadSprites(state)
      renderer.renderSprites(state)
    }
  }

  @Test
  fun `greyscale mode`() {
    val pattern = listOf(0, 1, 2, 3, 2, 3, 0, 1)
    val attrEntries = List(NUM_METATILE_COLUMNS) { 3 }
    initAttributeMemory(attrEntries)
    initBgPatternMemory(mapOf(0 to pattern))

    val state = State(
      bgEnabled = true,
      bgLeftTileEnabled = true,
      bgPatternTable = BG_PATTERN_TABLE,
      coords = Coords(xCoarse = 0, xFine = 0, yCoarse = Y_COARSE, yFine = Y_FINE),
      scanline = Y_SCANLINE,
      greyscale = true
    )
    renderer.renderBackground(state)
    renderer.commitToBuffer(state)

    assertEquals(
      (0 until SCREEN_WIDTH).map {
        val p = pattern[it % TILE_SIZE]
        colors[paletteEntries[if (p == 0) 0 else (p + NUM_ENTRIES_PER_PALETTE * 3)] and 0x30]
      },
      extractScanline()
    )
  }

  private fun assertBuffer(expected: (Int) -> Int) {
    assertEquals(
      (0 until SCREEN_WIDTH).map { expected(it) },
      renderer.diagnostics.state.paletteIndices
    )
  }

  private fun extractScanline() = renderer.diagnostics.state.paletteIndices

  private fun initNametableMemory(
    nametableEntries: List<Int>,
    nametable: Int = NAMETABLE,
    yTile: Int = Y_COARSE
  ) {
    nametableEntries.forEachIndexed { idx, data ->
      whenever(memory[BASE_NAMETABLES + (nametable * 0x400) + (yTile * NUM_TILE_COLUMNS) + idx]) doReturn data
    }
  }

  private fun initAttributeMemory(
    attrEntries: List<Int>,
    nametable: Int = NAMETABLE,
    yTile: Int = Y_COARSE
  ) {
    attrEntries.chunked(2).forEachIndexed { idx, data ->
      val attr = if ((yTile % 4) / 2 == 0) {
        (data[1] shl 2) or (data[0] shl 0)
      } else {
        (data[1] shl 6) or (data[0] shl 4)
      }
      whenever(memory[BASE_NAMETABLES + (nametable * 0x400) + (NUM_TILE_COLUMNS * NUM_TILE_ROWS) + ((yTile / 4) * (NUM_METATILE_COLUMNS / 2)) + idx]
      ) doReturn attr
    }
  }

  private fun initBgPatternMemory(patterns: Map<Int, List<Int>>, yRow: Int = Y_FINE) {
    initPatternMemory(patterns, yRow, BG_PATTERN_TABLE * 0x1000)
  }

  private fun initSprPatternMemory(patterns: Map<Int, List<Int>>, yRow: Int) {
    initPatternMemory(patterns, yRow, SPR_PATTERN_TABLE * 0x1000)
  }

  private fun initPatternMemory(patterns: Map<Int, List<Int>>, yFine: Int, baseAddr: Address) {
    patterns.forEach { (idx, v) ->
      var lo = 0
      var hi = 0
      v.forEach {
        lo = (lo shl 1) or (it and 1)
        hi = (hi shl 1) or ((it / 2) and 1)
      }
      whenever(memory[baseAddr + (idx * PATTERN_SIZE_BYTES) + yFine]) doReturn lo
      whenever(memory[baseAddr + (idx * PATTERN_SIZE_BYTES) + yFine + TILE_SIZE]) doReturn hi
    }
  }

  private fun initSpriteMemory(x: Int, y: Int, iPattern: Int, attrs: Int, iSprite: Int = 0) {
    val base = iSprite * SPRITE_SIZE_BYTES
    whenever(oam[base + 0]) doReturn y
    whenever(oam[base + 1]) doReturn iPattern
    whenever(oam[base + 2]) doReturn attrs
    whenever(oam[base + 3]) doReturn x
  }

  companion object {
    private const val NAMETABLE = 0
    private const val BG_PATTERN_TABLE = 0
    private const val SPR_PATTERN_TABLE = 1

    // Chosen offset and scanline - these are *not* the same!  They have no relationship.
    private const val Y_COARSE = 14
    private const val Y_FINE = 4
    private const val Y_SCANLINE = 53
  }
}
