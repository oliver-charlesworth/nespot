package choliver.nespot.ppu

import choliver.nespot.Data
import choliver.nespot.Memory
import choliver.nespot.MutableForPerfReasons
import choliver.nespot.isBitSet
import choliver.nespot.ppu.Ppu.Companion.BASE_NAMETABLES
import choliver.nespot.ppu.Ppu.Companion.BASE_PATTERNS
import choliver.nespot.ppu.Ppu.Companion.NAMETABLE_SIZE_BYTES
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
  data class Context(
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

  data class Result(
    val sprite0Hit: Boolean,
    val spriteOverflow: Boolean
  )

  private data class Pixel(
    val c: Int,
    val p: Int,
    val src: Int
  )

  private data class SpriteToRender(
    val x: Int,
    val iSprite: Int,
    val pattern: Int,
    val palette: Int,
    val flipX: Boolean,
    val behind: Boolean
  )

  private val pixels = Array(SCREEN_WIDTH) { Pixel(0, 0, 0) }

  fun renderScanline(ctx: Context): Result {
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

    return Result(
      sprite0Hit = isHit,
      spriteOverflow = (ctx.bgEnabled || ctx.sprEnabled) && (sprites.size > MAX_SPRITES_PER_SCANLINE)
    )
  }

  private fun prepareBackground(ctx: Context) {
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
        pixels[x] = Pixel(c = patternPixel(pattern, xFine), p = palette, src = SRC_BG)
        incrementX()
      }
    }
  }

  private fun prepareBlankBackground() {
    for (x in 0 until SCREEN_WIDTH) {
      pixels[x] = Pixel(c = 0, p = 0, src = SRC_BG)
    }
  }

  private fun blankLeftBackgroundTile() {
    for (x in 0 until TILE_SIZE) {
      pixels[x] = Pixel(c = 0, p = 0, src = SRC_BG)
    }
  }

  private fun getSpritesForScanline(ctx: Context): List<SpriteToRender> {
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

  private fun prepareSpritesAndDetectHit(ctx: Context, sprites: List<SpriteToRender>): Boolean {
    var isHit = false

    // Lowest index is highest priority, so render last
    sprites.forEach { spr ->
      for (xPixel in 0 until min(TILE_SIZE, SCREEN_WIDTH - spr.x)) {
        val x = spr.x + xPixel
        val pxSpr = Pixel(
          c = patternPixel(spr.pattern, maybeFlip(xPixel, spr.flipX)),
          p = spr.palette,
          src = spr.iSprite
        )
        val pxCurrent = pixels[x]
        val opaqueSpr = (pxSpr.c != 0)
        val opaqueCurrent = (pxCurrent.c != 0)
        val clipped = !ctx.sprLeftTileEnabled && x < TILE_SIZE

        when {
          clipped -> {}                        // Don't render if clipped
          (pxCurrent.src != SRC_BG) -> {}      // Don't render if already a higher-priority sprite
          !opaqueSpr -> {}                     // Don't render if transparent
          (spr.behind && opaqueCurrent) -> {}  // Don't render if behind an opaque pixel
          else -> pixels[x] = pxSpr
        }

        // Collision detection - no need to check if current is sprite, as only relevant for iSprite == 0
        when {
          (spr.iSprite > 0) -> {}
          clipped -> {}
          !opaqueCurrent -> {}
          !opaqueSpr -> {}
          (x == (SCREEN_WIDTH  - 1)) -> {}
          else -> isHit = true
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

    private const val SRC_BG = -1
  }
}
