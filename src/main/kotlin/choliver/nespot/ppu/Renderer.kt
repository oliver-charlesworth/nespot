package choliver.nespot.ppu

import choliver.nespot.*
import choliver.nespot.ppu.Ppu.Companion.BASE_NAMETABLES
import choliver.nespot.ppu.Ppu.Companion.BASE_PATTERNS
import choliver.nespot.ppu.Ppu.Companion.NAMETABLE_SIZE_BYTES
import choliver.nespot.ppu.model.Coords
import choliver.nespot.ppu.model.State
import java.nio.IntBuffer
import kotlin.math.min


// TODO - eliminate all the magic numbers here
// TODO - colour emphasis
class Renderer(
  private val memory: Memory,
  private val palette: Memory,
  private val oam: Memory,
  private val videoBuffer: IntBuffer,
  private val colors: List<Int> = COLORS
) {
  @MutableForPerfReasons
  private data class Pixel(
    var c: Int = 0,
    var p: Int = 0,
    var opaqueSpr: Boolean = false  // Is an opaque sprite placed here?
  )

  @MutableForPerfReasons
  private data class SpriteToRender(
    var x: Int = 0,
    var sprite0: Boolean = false,
    var patternAddr: Address = 0x0000,
    var pattern: Int = 0,
    var palette: Int = 0,
    var flipX: Boolean = false,
    var behind: Boolean = false,
    var valid: Boolean = false
  )

  private var paletteEntry = 0
  private var pattern: Data = 0x00


  private val pixels = Array(SCREEN_WIDTH) { Pixel() }
  // One extra to detect overflow
  private val sprites = List(MAX_SPRITES_PER_SCANLINE + 1) { SpriteToRender() }.toList()

  fun renderScanline(state: State) {
    // TODO - validate this gating of evaluation happens (does it matter?)
    state.spriteOverflow = (state.bgEnabled || state.sprEnabled) && evaluateSprites(state)

    loadSprites()

    prepareBackground(state)

    state.sprite0Hit = state.sprEnabled && prepareSpritesAndDetectHit(state)

    renderToBuffer(state)
  }

  private fun prepareBackground(state: State) {
    with(state.coords) {
      for (x in 0 until SCREEN_WIDTH) {
        if (state.bgEnabled) {
          if ((x == 0) || (xFine == 0)) {
            loadNextBackgroundTile(state)
          }
          if (state.bgLeftTileEnabled || (x >= TILE_SIZE)) {
            pixels[x].setBackgroundValue(state)
          } else {
            pixels[x].setBlankValue()
          }
          incrementX()
        } else {
          pixels[x].setBlankValue()
        }
      }
    }
  }

  private fun loadNextBackgroundTile(state: State) {
    with(state.coords) {
      val addrNt = BASE_NAMETABLES +
        ((yNametable * 2 + xNametable) * NAMETABLE_SIZE_BYTES) +
        (yCoarse * NUM_TILE_COLUMNS) +
        xCoarse

      val addrAttr = BASE_NAMETABLES +
        ((yNametable * 2 + xNametable) * NAMETABLE_SIZE_BYTES) +
        960 +
        (yCoarse / 4) * (NUM_TILE_COLUMNS / 4) +
        (xCoarse / 4)

      paletteEntry = (memory[addrAttr] shr (((yCoarse / 2) % 2) * 4 + ((xCoarse / 2) % 2) * 2)) and 0x03
      pattern = loadPattern(patternAddr(iTable = state.bgPatternTable, iTile = memory[addrNt], iRow = yFine))
    }
  }

  private fun Pixel.setBackgroundValue(state: State) {
    c = patternPixel(pattern, state.coords.xFine)
    p = paletteEntry
    opaqueSpr = false
  }

  private fun Pixel.setBlankValue() {
    c = 0
    p = 0
    opaqueSpr = false
  }

  private fun evaluateSprites(state: State): Boolean {
    var iCandidate = 0

    sprites.forEach { spr ->
      spr.valid = false

      // Scan until we find a matching sprite
      while (!spr.valid && (iCandidate < NUM_SPRITES)) {
        val y = oam[iCandidate * 4 + 0] + 1
        val iPattern = oam[iCandidate * 4 + 1]
        val attrs = oam[iCandidate * 4 + 2]
        val x = oam[iCandidate * 4 + 3]

        val iRow = state.scanline - y
        val flipY = attrs.isBitSet(7)

        with(spr) {
          this.x = x
          sprite0 = (iCandidate == 0)
          palette = (attrs and 0x03) + 4
          flipX = attrs.isBitSet(6)
          behind = attrs.isBitSet(5)
          patternAddr = patternAddr(
            iTable = when (state.largeSprites) {
              true -> iPattern and 0x01
              false -> state.sprPatternTable
            },
            iTile = when (state.largeSprites) {
              true -> (iPattern and 0xFE) + (if (flipY xor (iRow < TILE_SIZE)) 0 else 1)
              false -> iPattern
            },
            iRow = maybeFlip(iRow % TILE_SIZE, flipY)
          )
          valid = iRow in 0 until if (state.largeSprites) (TILE_SIZE * 2) else TILE_SIZE
        }

        iCandidate++
      }
    }

    return sprites.last().valid
  }

  private fun loadSprites() {
    sprites
      .dropLast(1)
      .forEach { spr ->
        spr.pattern = if (spr.valid) {
          loadPattern(spr.patternAddr)
        } else {
          loadPattern(DUMMY_SPRITE_PATTERN_ADDR)
          0   // All-transparent
        }
      }
  }

  // Lowest index is highest priority, so render last
  private fun prepareSpritesAndDetectHit(state: State) = sprites
    .dropLast(1)
    .map { spr -> prepareSpriteAndDetectHit(spr, state) }
    .any { it }

  private fun prepareSpriteAndDetectHit(spr: SpriteToRender, state: State): Boolean {
    var hit = false

    for (xPixel in 0 until min(TILE_SIZE, SCREEN_WIDTH - spr.x)) {
      val x = spr.x + xPixel
      val c = patternPixel(spr.pattern, maybeFlip(xPixel, spr.flipX))
      val px = pixels[x]

      val opaqueSpr = (c != 0)
      val clipped = !state.sprLeftTileEnabled && (x < TILE_SIZE)

      if (opaqueSpr && !px.opaqueSpr && !clipped) {
        px.opaqueSpr = true

        // There is no previous opaque sprite, so if pixel is opaque then it must be background
        val opaqueBg = (px.c != 0)

        if (!(spr.behind && opaqueBg)) {
          px.c = c
          px.p = spr.palette
        }

        if (spr.sprite0 && opaqueBg && (x < (SCREEN_WIDTH - 1))) {
          hit = true
        }
      }
    }

    return hit
  }

  private fun renderToBuffer(state: State) {
    videoBuffer.position(state.scanline * SCREEN_WIDTH)
    val mask = if (state.greyscale) 0x30 else 0x3F  // TODO - implement greyscale in Palette itself
    pixels.forEach {
      val paletteAddr = if (it.c == 0) 0 else (it.p * 4 + it.c) // Background colour is universal
      videoBuffer.put(colors[palette[paletteAddr] and mask])
    }
  }

  private fun maybeFlip(v: Int, flip: Boolean) = if (flip) (7 - v) else v

  private fun patternPixel(pattern: Int, xPixel: Int) =
    ((pattern shr (7 - xPixel)) and 1) or (((pattern shr (14 - xPixel)) and 2))

  private fun loadPattern(addr: Int): Int {
    val p0 = memory[BASE_PATTERNS + addr]
    val p1 = memory[BASE_PATTERNS + addr + TILE_SIZE]
    return (p1 shl 8) or p0
  }

  private fun patternAddr(iTable: Int, iTile: Int, iRow: Int) = (iTable * 4096) + (iTile * 16) + iRow

  companion object {
    const val MAX_SPRITES_PER_SCANLINE = 8
    private const val DUMMY_SPRITE_PATTERN_ADDR = 0x1FF0
  }
}
