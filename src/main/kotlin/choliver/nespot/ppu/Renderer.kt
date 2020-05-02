package choliver.nespot.ppu

import choliver.nespot.Data
import choliver.nespot.Memory
import choliver.nespot.MutableForPerfReasons
import choliver.nespot.isBitSet
import choliver.nespot.ppu.Ppu.Companion.BASE_NAMETABLES
import choliver.nespot.ppu.Ppu.Companion.BASE_PATTERNS
import choliver.nespot.ppu.Ppu.Companion.NAMETABLE_SIZE_BYTES
import choliver.nespot.ppu.model.Coords
import java.nio.IntBuffer
import kotlin.math.min

// TODO - eliminate all the magic numbers here
// TODO - grayscale
// TODO - emphasize
class Renderer(
  private val memory: Memory,
  private val palette: Memory,
  private val oam: Memory,
  private val videoBuffer: IntBuffer,
  private val colors: List<Int> = COLORS
) {

  @MutableForPerfReasons
  data class Input(
    var bgEnabled: Boolean,
    var sprEnabled: Boolean,
    var bgLeftTileEnabled: Boolean,
    var sprLeftTileEnabled: Boolean,
    var largeSprites: Boolean,
    var bgPatternTable: Int,  // 0 or 1
    var sprPatternTable: Int, // 0 or 1
    var coords: Coords,
    var scanline: Int
  )

  data class Output(
    val sprite0Hit: Boolean,
    val spriteOverflow: Boolean
  )

  @MutableForPerfReasons
  private data class Pixel(
    var c: Int = 0,
    var p: Int = 0,
    var opaqueSpr: Boolean = false  // Is an opaque sprite placed here?
  )

  private data class SpriteToRender(
    val x: Int,
    val iSprite: Int,
    val pattern: Int,
    val palette: Int,
    val flipX: Boolean,
    val behind: Boolean
  )

  private val pixels = Array(SCREEN_WIDTH) { Pixel() }

  fun renderScanline(ctx: Input): Output {
    if (ctx.bgEnabled) {
      prepareBackground(ctx)
    } else {
      prepareBlankBackground()
    }

    if (!ctx.bgLeftTileEnabled) {
      blankLeftBackgroundTile()
    }

    val sprites = getSpritesForScanline(ctx)

    val isHit = if (ctx.sprEnabled) {
      prepareSpritesAndDetectHit(ctx, sprites.take(MAX_SPRITES_PER_SCANLINE))
    } else {
      false
    }

    renderToBuffer(ctx.scanline)

    return Output(
      sprite0Hit = isHit,
      spriteOverflow = (ctx.bgEnabled || ctx.sprEnabled) && (sprites.size > MAX_SPRITES_PER_SCANLINE)
    )
  }

  private fun prepareBackground(ctx: Input) {
    var palette = 0
    var pattern: Data = 0x00

    with (ctx.coords) {
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

          palette = (memory.load(addrAttr) shr (((yCoarse / 2) % 2) * 4 + ((xCoarse / 2) % 2) * 2)) and 0x03
          pattern = getPattern(iTable = ctx.bgPatternTable, iTile = memory.load(addrNt), iRow = yFine)
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

  private fun getSpritesForScanline(ctx: Input): List<SpriteToRender> {
    val sprites = mutableListOf<SpriteToRender>()

    for (iSprite in 0 until NUM_SPRITES) {
      val ySprite = oam.load(iSprite * 4 + 0) + 1   // Offset of one scanline
      val iPattern = oam.load(iSprite * 4 + 1)
      val attrs = oam.load(iSprite * 4 + 2)
      val xSprite = oam.load(iSprite * 4 + 3)

      val iRow = ctx.scanline - ySprite
      val flipY = attrs.isBitSet(7)

      val inRange = iRow in 0 until if (ctx.largeSprites) (TILE_SIZE * 2) else TILE_SIZE

      if (inRange) {
        sprites += SpriteToRender(
          x = xSprite,
          iSprite = iSprite,
          pattern = getPattern(
            iTable = when (ctx.largeSprites) {
              true -> iPattern and 0x01
              false -> ctx.sprPatternTable
            },
            iTile = when (ctx.largeSprites) {
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
    }

    return sprites
  }

  private fun prepareSpritesAndDetectHit(ctx: Input, sprites: List<SpriteToRender>): Boolean {
    var isHit = false

    // Lowest index is highest priority, so render last
    sprites.forEach { spr ->
      for (xPixel in 0 until min(TILE_SIZE, SCREEN_WIDTH - spr.x)) {
        val x = spr.x + xPixel
        val c = patternPixel(spr.pattern, maybeFlip(xPixel, spr.flipX))
        val px = pixels[x]

        val opaqueSpr = (c != 0)
        val clipped = !ctx.sprLeftTileEnabled && (x < TILE_SIZE)

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

  private fun renderToBuffer(yScanline: Int) {
    videoBuffer.position(yScanline * SCREEN_WIDTH)
    pixels.forEach {
      val paletteAddr = if (it.c == 0) 0 else (it.p * 4 + it.c) // Background colour is universal
      videoBuffer.put(colors[palette.load(paletteAddr)])
    }
  }

  private fun maybeFlip(v: Int, flip: Boolean) = if (flip) (7 - v) else v

  private fun patternPixel(pattern: Int, xPixel: Int) =
    ((pattern shr (7 - xPixel)) and 1) or (((pattern shr (14 - xPixel)) and 2))

  private fun getPattern(iTable: Int, iTile: Int, iRow: Int): Int {
    val addr = (iTable * 4096) + (iTile * 16) + iRow
    val p0 = memory.load(BASE_PATTERNS + addr)
    val p1 = memory.load(BASE_PATTERNS + addr + TILE_SIZE)
    return (p1 shl 8) or p0
  }

  companion object {
    const val MAX_SPRITES_PER_SCANLINE = 8
  }
}
