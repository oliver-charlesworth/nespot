package choliver.nespot.ppu

import choliver.nespot.Address
import choliver.nespot.Memory
import choliver.nespot.apu.repeat
import choliver.nespot.hi
import choliver.nespot.lo
import choliver.nespot.ppu.Ppu.Companion.BASE_NAMETABLES
import choliver.nespot.ppu.model.Coords
import choliver.nespot.ppu.model.State
import com.nhaarman.mockitokotlin2.*
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import java.nio.IntBuffer


class RendererTest {
  private val colors = (0..63).toList()
  private val memory = mock<Memory>()
  private val palette = mock<Memory>()
  private val oam = mock<Memory>()
  private val renderer = Renderer(
    memory = memory,
    palette = palette,
    oam = oam,
    colors = colors
  )

  @Nested
  inner class BackgroundRender {
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
    fun `coarse horizontal offset`() {
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
    fun `fine horizontal offset`() {
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
      xCoarse: Int = 0,
      xFine: Int = 0,
      yCoarse: Int = Y_COARSE
    ) {
      val state = State(
        bgEnabled = bgEnabled,
        bgLeftTileEnabled = bgLeftTileEnabled,
        bgPatternTable = BG_PATTERN_TABLE,
        coords = Coords(xCoarse = xCoarse, xFine = xFine, yCoarse = yCoarse, yFine = Y_FINE)
      )
      renderer.loadAndRenderBackground(state)
    }
  }

  @Nested
  inner class SpriteEvaluation {
    @Test
    fun `address of top row`() {
      val iPattern = 33
      initSpriteMemory(x = 0, y = Y_SCANLINE, iPattern = iPattern, attrs = 0)

      evaluate()

      assertEquals(SPR_PATTERN_TABLE * 4096 + iPattern * 16, sprites[0].patternAddr)
    }

    @Test
    fun `address of bottom row`() {
      val iPattern = 33
      initSpriteMemory(x = 0, y = Y_SCANLINE - 7, iPattern = iPattern, attrs = 0)

      evaluate()

      assertEquals(SPR_PATTERN_TABLE * 4096 + iPattern * 16 + 7, sprites[0].patternAddr)
    }

    @Test
    fun `address of top row if bottom row specified when vertically flipped`() {
      val iPattern = 33
      initSpriteMemory(x = 0, y = Y_SCANLINE - 7, iPattern = iPattern, attrs = 0x80)

      evaluate()

      assertEquals(SPR_PATTERN_TABLE * 4096 + iPattern * 16, sprites[0].patternAddr)
    }

    @Test
    fun `large sprite - address of top row`() {
      val iPattern = 32
      initSpriteMemory(x = 0, y = Y_SCANLINE, iPattern = iPattern, attrs = 0)

      evaluate(largeSprites = true)

      assertEquals(iPattern * 16, sprites[0].patternAddr)
    }

    @Test
    fun `large sprite - address of bottom row`() {
      val iPattern = 32
      initSpriteMemory(x = 0, y = Y_SCANLINE - 15, iPattern = iPattern, attrs = 0)

      evaluate(largeSprites = true)

      assertEquals((iPattern + 1) * 16 + 7, sprites[0].patternAddr)
    }

    @Test
    fun `large sprite - address of top row if bottom row specified when vertically flipped`() {
      val iPattern = 32
      initSpriteMemory(x = 0, y = Y_SCANLINE - 15, iPattern = iPattern, attrs = 0x80)

      evaluate(largeSprites = true)

      assertEquals(iPattern * 16, sprites[0].patternAddr)
    }

    @Test
    fun `large sprite - address of bottom row if top row specified when vertically flipped`() {
      val iPattern = 32
      initSpriteMemory(x = 0, y = Y_SCANLINE, iPattern = iPattern, attrs = 0x80)

      evaluate(largeSprites = true)

      assertEquals((iPattern + 1) * 16 + 7, sprites[0].patternAddr)
    }

    @Test
    fun `large sprite - upper pattern table`() {
      val iPattern = 33 // LSB specifies which pattern table
      initSpriteMemory(x = 0, y = Y_SCANLINE, iPattern = iPattern, attrs = 0)

      evaluate(largeSprites = true)

      assertEquals(4096 + (iPattern - 1) * 16, sprites[0].patternAddr)
    }

    @Test
    fun `specifies x`() {
      val x = 5
      initSpriteMemory(x = x, y = Y_SCANLINE, iPattern = 0, attrs = 0x00)

      evaluate()

      assertEquals(x, sprites[0].x)
    }

    @Test
    fun `specifies palette`() {
      val palette = 2
      initSpriteMemory(x = 0, y = Y_SCANLINE, iPattern = 0, attrs = palette)

      evaluate()

      assertEquals(palette + 4, sprites[0].palette) // Sprite palette, so offset by 4
    }

    @ParameterizedTest
    @ValueSource(booleans = [false, true])
    fun `specifies behind`(behind: Boolean) {
      initSpriteMemory(x = 0, y = Y_SCANLINE, iPattern = 0, attrs = if (behind) 0x20 else 0x00)

      evaluate()

      assertEquals(behind, sprites[0].behind)
    }

    @ParameterizedTest
    @ValueSource(booleans = [false, true])
    fun `specifies flipX`(flipX: Boolean) {
      initSpriteMemory(x = 0, y = Y_SCANLINE, iPattern = 0, attrs = if (flipX) 0x40 else 0x00)

      evaluate()

      assertEquals(flipX, sprites[0].flipX)
    }

    @ParameterizedTest
    @ValueSource(booleans = [false, true])
    fun `specifies whether sprite #0`(sprite0InRange: Boolean) {
      initSpriteMemory(x = 0, y = Y_SCANLINE + if (sprite0InRange) 0 else 1, iPattern = 0, attrs = 0x00, iSprite = 0)
      initSpriteMemory(x = 0, y = Y_SCANLINE, iPattern = 0, attrs = 0x00, iSprite = 5)

      evaluate()

      assertEquals(sprite0InRange, sprites[0].sprite0)
    }

    @Test
    fun `lowest-index output corresponds to lowest-index OAM entries`() {
      val oamIndices = listOf(1, 2, 3, 5, 8, 13, 21, 34)
      oamIndices.forEach {
        initSpriteMemory(x = 0, y = Y_SCANLINE, iPattern = it, attrs = 0x00, iSprite = it)
      }

      evaluate()

      oamIndices.forEachIndexed { idx, oamIdx ->
        assertEquals(SPR_PATTERN_TABLE * 4096 + oamIdx * 16, sprites[idx].patternAddr)
      }
    }

    @Test
    fun `creates invalid entries if not enough in-range sprites`() {
      initSpriteMemory(x = 0, y = Y_SCANLINE, iPattern = 0, attrs = 0x00, iSprite = 0)
      initSpriteMemory(x = 0, y = Y_SCANLINE, iPattern = 0, attrs = 0x00, iSprite = 1)
      initSpriteMemory(x = 0, y = Y_SCANLINE, iPattern = 0, attrs = 0x00, iSprite = 2)

      evaluate()

      assertTrue(sprites[0].valid)
      assertTrue(sprites[1].valid)
      assertTrue(sprites[2].valid)
      assertFalse(sprites[3].valid)
      assertFalse(sprites[4].valid)
      assertFalse(sprites[5].valid)
      assertFalse(sprites[6].valid)
      assertFalse(sprites[7].valid)
    }

    @Test
    fun `sets overflow if more than 8 valid sprites`() {
      val oamIndices = listOf(1, 2, 3, 5, 8, 13, 21, 34, 55)
      oamIndices.forEach {
        initSpriteMemory(x = 0, y = Y_SCANLINE, iPattern = it, attrs = 0x00, iSprite = it)
      }

      assertTrue(evaluate().spriteOverflow)
    }

    @Test
    fun `doesn't set overflow if only 8 valid sprites`() {
      val oamIndices = listOf(1, 2, 3, 5, 8, 13, 21, 34)
      oamIndices.forEach {
        initSpriteMemory(x = 0, y = Y_SCANLINE, iPattern = it, attrs = 0x00, iSprite = it)
      }

      assertFalse(evaluate().spriteOverflow)
    }

    @Test
    fun `sets overflow if rendering disabled`() {
      val oamIndices = listOf(1, 2, 3, 5, 8, 13, 21, 34, 55)
      oamIndices.forEach {
        initSpriteMemory(x = 0, y = Y_SCANLINE, iPattern = it, attrs = 0x00, iSprite = it)
      }

      assertTrue(evaluate(bgEnabled = false, sprEnabled = true).spriteOverflow)
      assertTrue(evaluate(bgEnabled = true, sprEnabled = false).spriteOverflow)
      assertFalse(evaluate(bgEnabled = false, sprEnabled = false).spriteOverflow)
    }

    private fun evaluate(
      bgEnabled: Boolean = true,
      sprEnabled: Boolean = true,
      largeSprites: Boolean = false
    ): State {
      val state = State(
        bgEnabled = bgEnabled,
        sprEnabled = sprEnabled,
        sprPatternTable = SPR_PATTERN_TABLE,
        scanline = Y_SCANLINE,
        largeSprites = largeSprites
      )
      renderer.evaluateSprites(state)
      return state
    }
  }

  @Nested
  inner class SpriteLoad {
    @Test
    fun `loads pattern for valid sprite`() {
      whenever(memory[4096+0]) doReturn 0xCC
      whenever(memory[4096+8]) doReturn 0xAA

      sprites[0].patternAddr = 4096
      sprites[0].valid = true

      load()

      assertEquals(0xAACC, sprites[0].pattern)
    }

    // MMC3 relies on specific PPU memory access pattern during scanline
    @Test
    fun `performs dummy load from nametable #1 and sets transparent pattern for invalid sprite`() {
      (1..7).forEach { sprites[it].valid = true } // Prevent interference
      sprites[0].patternAddr = 4096
      sprites[0].valid = false

      load()

      assertEquals(0x0000, sprites[0].pattern)  // Transparent pattern
      verify(memory)[0x1FF0]
      verify(memory)[0x1FF8]
    }

    @Test
    fun `performs 8 loads if sprite rendering enabled`() {
      load()

      verify(memory, times(8 * 2))[any()]   // x2 because upper and lower slice per pattern
    }

    @Test
    fun `performs no loads if sprite rendering disabled`() {
      load(sprEnabled = false)

      verifyZeroInteractions(memory)
    }

    private fun load(sprEnabled: Boolean = true) {
      val state = State(
        sprEnabled = sprEnabled
      )
      renderer.loadSprites(state)
    }
  }

  @Nested
  inner class SpriteRender {
    private val pat = listOf(1, 2, 3, 3, 2, 1, 1, 2)

    @Test
    fun `opaque sprite`() {
      with(sprites[0]) {
        pattern = encodePattern(pat)
        x = 5
        palette = 4
      }

      render()

      assertBuffer { calcPalIdx(it, 5, 0, pat) }
    }

    @Test
    fun `horizontally flipped`() {
      with(sprites[0]) {
        this.pattern = encodePattern(pat)
        x = 5
        palette = 4
        flipX = true
      }

      render()

      assertBuffer { calcPalIdx(it, 5, 0, pat.reversed()) }
    }

    @Test
    fun `partially off the right-hand edge`() {
      with(sprites[0]) {
        this.pattern = encodePattern(pat)
        x = 252
        palette = 4
      }

      render()

      assertBuffer { calcPalIdx(it, 252, 0, pat) }
    }

    @Test
    fun `not rendered if rendering disabled`() {
      with(sprites[0]) {
        pattern = encodePattern(pat)
        x = 5
        palette = 4
      }

      render(sprEnabled = false)

      assertBuffer { 0 }
    }

    @Test
    fun `not rendered if clipped`() {
      with(sprites[0]) {
        pattern = encodePattern(pat)
        x = 5
        palette = 4
      }

      render(sprLeftTileEnabled = false)

      assertBuffer { calcPalIdx(it, 5, 0, listOf(0, 0, 0) + pat.drop(3)) }
    }

    private fun calcPalIdx(x: Int, xOffset: Int, iPalette: Int, pattern: List<Int>) =
      if (x in xOffset until TILE_SIZE + xOffset) {
        val p = pattern[x - xOffset]
        if (p > 0) (p + ((NUM_PALETTES + iPalette) * NUM_ENTRIES_PER_PALETTE)) else 0
      } else 0

    private fun render(
      sprEnabled: Boolean = true,
      sprLeftTileEnabled: Boolean = true
    ) {
      val state = State(
        sprEnabled = sprEnabled,
        sprLeftTileEnabled = sprLeftTileEnabled
      )
      renderer.renderSprites(state)
    }
  }

  @Nested
  inner class SpriteRenderPriority {
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
      with(sprites[iSprite]) {
        pattern = encodePattern(listOf(if (opaque) 1 else 0, 0, 0, 0, 0, 0, 0, 0))
        this.palette = palette + 4
        this.behind = behind
      }
    }

    private fun initBackground(opaque: Boolean) {
      paletteIndices[0] = if (opaque) bgEntry else 0
    }

    private fun assertPixelColour(expected: Int) {
      render()
      assertEquals(expected, paletteIndices[0])
    }

    private fun render() {
      val state = State(
        sprEnabled = true,
        sprLeftTileEnabled = true
      )
      renderer.renderSprites(state)
    }
  }

  @Nested
  inner class SpriteRenderCollisions {
    init {
      paletteIndices[0] = 1
      with(sprites[0]) {
        pattern = encodePattern(listOf(1, 0, 0, 0, 0, 0, 0, 0))
        palette = 4
        sprite0 = true
      }
    }

    @Test
    fun `detected if opaque sprite #0 overlaps opaque background`() {
      assertTrue(render().sprite0Hit)
    }

    @Test
    fun `not detected if opaque sprite #0 overlaps transparent background`() {
      paletteIndices[0] = 0

      assertFalse(render().sprite0Hit)
    }

    @Test
    fun `not detected if transparent sprite #0 overlaps opaque background`() {
      sprites[0].pattern = 0

      assertFalse(render().sprite0Hit)
    }

    @Test
    fun `not detected for overlap from sprites other than #0`() {
      sprites[0].sprite0 = false

      assertFalse(render().sprite0Hit)
    }

    @Test
    fun `detected for overlap at x == 254`() {
      paletteIndices[254] = 1
      sprites[0].x = 254

      assertTrue(render().sprite0Hit)
    }

    @Test
    fun `not detected for overlap at x == 255`() {
      paletteIndices[255] = 1
      sprites[0].x = 255

      assertFalse(render().sprite0Hit)
    }

    @Test
    fun `detected if sprite behind`() {
      sprites[0].behind = true

      assertTrue(render().sprite0Hit)
    }

    @Test
    fun `not detected if rendering disabled`() {
      assertFalse(render(sprEnabled = false).sprite0Hit)
    }

    @Test
    fun `not detected if clipped`() {
      assertFalse(render(sprLeftTileEnabled = false).sprite0Hit)
    }

    private fun render(
      sprEnabled: Boolean = true,
      sprLeftTileEnabled: Boolean = true
    ): State {
      val state = State(
        sprEnabled = sprEnabled,
        sprLeftTileEnabled = sprLeftTileEnabled
      )
      renderer.renderSprites(state)
      return state
    }
  }

  @Nested
  inner class CommitToBuffer {
    private val paletteEntries = (0..31).map { it * 2 + 1 }
    private val videoBuffer = IntBuffer.allocate(SCREEN_WIDTH * SCREEN_HEIGHT)

    init {
      paletteEntries.forEachIndexed { idx, data ->
        whenever(palette[idx]) doReturn data
      }
    }

    @Test
    fun `maps colours`() {
      repeat(32) {
        paletteIndices[it] = it
      }

      commit()

      repeat(32) {
        assertEquals(colors[paletteEntries[it]], videoBuffer[Y_SCANLINE * SCREEN_WIDTH + it])
      }
    }

    @Test
    fun `maps greyscale colours`() {
      repeat(32) {
        paletteIndices[it] = it
      }

      commit(true)

      repeat(32) {
        assertEquals(colors[paletteEntries[it] and 0x30], videoBuffer[Y_SCANLINE * SCREEN_WIDTH + it])
      }
    }

    private fun commit(
      greyscale: Boolean = false
    ) {
      val state = State(
        scanline = Y_SCANLINE,
        greyscale = greyscale
      )
      renderer.commitToBuffer(state, videoBuffer)
    }
  }

  private fun assertBuffer(expected: (Int) -> Int) {
    assertEquals(
      (0 until SCREEN_WIDTH).map { expected(it) },
      paletteIndices
    )
  }

  private val paletteIndices = renderer.diagnostics.state.paletteIndices
  private val sprites = renderer.diagnostics.state.sprites

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

  private fun initPatternMemory(patterns: Map<Int, List<Int>>, yFine: Int, baseAddr: Address) {
    patterns.forEach { (idx, v) ->
      val p = encodePattern(v)
      whenever(memory[baseAddr + (idx * PATTERN_SIZE_BYTES) + yFine]) doReturn p.lo()
      whenever(memory[baseAddr + (idx * PATTERN_SIZE_BYTES) + yFine + TILE_SIZE]) doReturn p.hi()
    }
  }

  private fun encodePattern(pattern: List<Int>): Int {
    var p = 0
    pattern.forEachIndexed { i, b ->
      p = p or ((b and 1) shl (7 - i)) or ((b and 2) shl (14 - i))
    }
    return p
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
