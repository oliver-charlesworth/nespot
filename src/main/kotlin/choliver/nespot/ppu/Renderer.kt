package choliver.nespot.ppu

import choliver.nespot.Data
import choliver.nespot.Memory
import choliver.nespot.MutableForPerfReasons
import choliver.nespot.isBitSet
import choliver.nespot.ppu.Ppu.Companion.BASE_NAMETABLES
import choliver.nespot.ppu.Ppu.Companion.BASE_PATTERNS
import choliver.nespot.ppu.Ppu.Companion.NAMETABLE_SIZE_BYTES
import choliver.nespot.ppu.model.State
import java.nio.IntBuffer
import kotlin.math.max
import kotlin.math.min

// TODO - eliminate all the magic numbers here
// TODO - emphasize
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

  private data class SpriteToRender(
    val x: Int,
    val iSprite: Int,
    val patternAddr: Int,
    val palette: Int,
    val flipX: Boolean,
    val behind: Boolean
  )

  private val pixels = Array(SCREEN_WIDTH) { Pixel() }

  fun renderScanline(state: State) {
    if (state.bgEnabled) {
      prepareBackground(state)
    } else {
      prepareBlankBackground()
    }

    if (!state.bgLeftTileEnabled) {
      blankLeftBackgroundTile()
    }

    val sprites = getSpritesForScanline(state)

    val isHit = if (state.sprEnabled) {
      prepareSpritesAndDetectHit(state, sprites.take(MAX_SPRITES_PER_SCANLINE))
    } else {
      false
    }

    renderToBuffer(state)

    state.sprite0Hit = isHit
    state.spriteOverflow = (state.bgEnabled || state.sprEnabled) && (sprites.size > MAX_SPRITES_PER_SCANLINE)
  }

  private fun prepareBackground(state: State) {
    var palette = 0
    var pattern: Data = 0x00

    with(state.coords) {
      for (x in 0 until SCREEN_WIDTH) {
        if ((x == 0) || (xFine == 0)) {
          val addrNt = BASE_NAMETABLES +
            ((yNametable * 2 + xNametable) * NAMETABLE_SIZE_BYTES) +
            (yCoarse * NUM_TILE_COLUMNS) +
            xCoarse

          val addrAttr = BASE_NAMETABLES +
            ((yNametable * 2 + xNametable) * NAMETABLE_SIZE_BYTES) +
            960 +
            (yCoarse / 4) * (NUM_TILE_COLUMNS / 4) +
            (xCoarse / 4)

          palette = (memory[addrAttr] shr (((yCoarse / 2) % 2) * 4 + ((xCoarse / 2) % 2) * 2)) and 0x03
          pattern = loadPattern(patternAddr(iTable = state.bgPatternTable, iTile = memory[addrNt], iRow = yFine))
        }
        with(pixels[x]) {
          c = patternPixel(pattern, xFine)
          p = palette
          opaqueSpr = false
        }
        incrementX()
      }
    }
  }

  private fun prepareBlankBackground() {
    for (x in 0 until SCREEN_WIDTH) {
      with(pixels[x]) {
        c = 0
        p = 0
        opaqueSpr = false
      }
    }
  }

  private fun blankLeftBackgroundTile() {
    for (x in 0 until TILE_SIZE) {
      with(pixels[x]) {
        c = 0
        p = 0
        opaqueSpr = false
      }
    }
  }

  private fun getSpritesForScanline(state: State): List<SpriteToRender> {
    val sprites = mutableListOf<SpriteToRender>()

    for (iSprite in 0 until NUM_SPRITES) {
      val ySprite = oam[iSprite * 4 + 0] + 1   // Offset of one scanline
      val iPattern = oam[iSprite * 4 + 1]
      val attrs = oam[iSprite * 4 + 2]
      val xSprite = oam[iSprite * 4 + 3]

      val iRow = state.scanline - ySprite
      val flipY = attrs.isBitSet(7)

      val inRange = iRow in 0 until if (state.largeSprites) (TILE_SIZE * 2) else TILE_SIZE

      // TODO - test that we *always* load a pattern (neeeded for MMC3 IRQ)

      if (inRange) {
        sprites += SpriteToRender(
          x = xSprite,
          iSprite = iSprite,
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
          ),
          palette = (attrs and 0x03) + 4,
          flipX = attrs.isBitSet(6),
          behind = attrs.isBitSet(5)
        )
      }
      if (sprites.size == MAX_SPRITES_PER_SCANLINE) {
        break
      }
    }

    repeat(max(0, MAX_SPRITES_PER_SCANLINE - sprites.size)) {
      loadPattern(patternAddr(iTable = 1, iTile = 0xFF, iRow = 0))
    }

    return sprites
  }

  private fun prepareSpritesAndDetectHit(state: State, sprites: List<SpriteToRender>): Boolean {
    var isHit = false

    // Lowest index is highest priority, so render last
    sprites.forEach { spr ->
      val pattern = loadPattern(spr.patternAddr)

      for (xPixel in 0 until min(TILE_SIZE, SCREEN_WIDTH - spr.x)) {
        val x = spr.x + xPixel
        val c = patternPixel(pattern, maybeFlip(xPixel, spr.flipX))
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

          if ((spr.iSprite == 0) && opaqueBg && (x < (SCREEN_WIDTH - 1))) {
            isHit = true
          }
        }
      }
    }

    return isHit
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
  }
}
