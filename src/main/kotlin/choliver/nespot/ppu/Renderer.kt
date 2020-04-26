package choliver.nespot.ppu

import choliver.nespot.Address
import choliver.nespot.Data
import choliver.nespot.Memory
import choliver.nespot.isBitSet
import choliver.nespot.ppu.Ppu.Companion.BASE_NAMETABLES
import choliver.nespot.ppu.Ppu.Companion.BASE_PATTERNS
import choliver.nespot.ppu.Ppu.Companion.NAMETABLE_SIZE_BYTES
import java.nio.IntBuffer
import kotlin.math.min

// TODO - eliminate all the magic numbers here
// TODO - conditional rendering
// TODO - scrolling
// TODO - only select four sprites
// TODO - priority
// TODO - grayscale
// TODO - emphasize
class Renderer(
  private val memory: Memory,
  private val palette: Memory,
  private val oam: Memory,
  private val videoBuffer: IntBuffer,
  private val colors: List<Int> = COLORS
) {
  data class Context(
    val isLargeSprites: Boolean,
    val bgPatternTable: Int,  // 0 or 1
    val sprPatternTable: Int, // 0 or 1
    val addrStart: Address,
    val fineX: Data,
    val fineY: Data
  )

  private data class Pixel(
    val c: Int,
    val p: Int
  )

  private val pixels = Array(SCREEN_WIDTH) { Pixel(0, 0) }

  fun renderScanlineAndDetectHit(y: Int, ctx: Context): Boolean {
    prepareBackground(ctx)
    val isHit = prepareSpritesAndDetectHit(y, ctx)
    renderToBuffer(y)
    return isHit
  }

  // TODO - this approach does 8x too many memory loads
  // TODO - how do we combine changing both vertical *and* horizontal nametables?
  private fun prepareBackground(ctx: Context) {
    var fineX = ctx.fineX
    var coarseX = (ctx.addrStart and 0x001F)
    val coarseY = (ctx.addrStart and 0x03E0) shr 5
    var iNametable = (ctx.addrStart and 0x0C00) shr 10

    for (x in 0 until SCREEN_WIDTH) {
      val addrNt = BASE_NAMETABLES +
        (iNametable * NAMETABLE_SIZE_BYTES) +
        (coarseY * NUM_TILE_COLUMNS) +
        coarseX

      val addrAttr = BASE_NAMETABLES +
        (iNametable * NAMETABLE_SIZE_BYTES) +
        960 +
        (coarseY / 4) * (NUM_TILE_COLUMNS / 4) +
        (coarseX / 4)

      val iPalette = (memory.load(addrAttr) shr (((coarseY / 2) % 2) * 4 + ((coarseX / 2) % 2) * 2)) and 0x03
      val pattern = getPattern(iTable = ctx.bgPatternTable, iTile = memory.load(addrNt), iRow = ctx.fineY)

      val c = patternPixel(pattern, fineX)
      pixels[x] = Pixel(c, iPalette)

      if (fineX < 7) {
        fineX++
      } else {
        fineX = 0
        if (coarseX < 31) {
          coarseX++
        } else {
          coarseX = 0
          iNametable = iNametable xor 1
        }
      }
    }
  }

  private fun prepareSpritesAndDetectHit(y: Int, ctx: Context): Boolean {
    var isHit = false

    for (iSprite in 0 until NUM_SPRITES) {
      val ySprite = oam.load(iSprite * 4 + 0) + 1   // Offset of one scanline
      val xSprite = oam.load(iSprite * 4 + 3)
      val iPattern = oam.load(iSprite * 4 + 1)
      val attrs = oam.load(iSprite * 4 + 2)
      val iPalette = (attrs and 0x03) + 4
      val isBehind = attrs.isBitSet(5)
      val flipX = attrs.isBitSet(6)
      val flipY = attrs.isBitSet(7)
      val yPixel = y - ySprite

      val pattern  = getSpritePattern(ctx, iPattern, yPixel, flipY)

      if (pattern != null) {
        for (xPixel in 0 until min(TILE_SIZE, SCREEN_WIDTH - xSprite)) {
          val c = patternPixel(
            pattern,
            if (flipX) (7 - xPixel) else xPixel
          )

          val x = xSprite + xPixel
          val spr = Pixel(c, iPalette)
          val bg = pixels[x]
          val opaqueSpr = (c != 0)
          val opaqueBg = (bg.c != 0)

          pixels[x] = if (opaqueSpr && (!isBehind || !opaqueBg)) spr else bg

          // Collision detection
          isHit = isHit || ((iSprite == 0) && opaqueBg && opaqueSpr && (x < (SCREEN_WIDTH - 1)))
        }
      }
    }

    return isHit
  }

  private fun getSpritePattern(ctx: Context, iPattern: Data, iRow: Int, flipY: Boolean) = if (ctx.isLargeSprites) {
    if ((iRow < 0) || (iRow >= (TILE_SIZE * 2))) {
      null
    } else if (iRow < TILE_SIZE) {
      getPattern(
        iTable = iPattern and 0x01,
        iTile = (iPattern and 0xFE) + (if (flipY) 1 else 0),
        iRow = iRow,
        flipY = flipY
      )
    } else {
      getPattern(
        iTable = iPattern and 0x01,
        iTile = (iPattern and 0xFE) + (if (flipY) 0 else 1),
        iRow = iRow - TILE_SIZE,
        flipY = flipY
      )
    }
  } else {
    if (iRow in 0 until TILE_SIZE) {
      getPattern(
        iTable = ctx.sprPatternTable,
        iTile = iPattern,
        iRow = iRow,
        flipY = flipY
      )
    } else null
  }

  private fun renderToBuffer(y: Int) {
    videoBuffer.position(y * SCREEN_WIDTH)
    pixels.forEach {
      val paletteAddr = if (it.c == 0) 0 else (it.p * 4 + it.c) // Background colour is universal
      videoBuffer.put(colors[palette.load(paletteAddr)])
    }
  }

  private fun patternPixel(pattern: Int, xPixel: Int) =
    ((pattern shr (7 - xPixel)) and 1) or (((pattern shr (14 - xPixel)) and 2))

  private fun getPattern(iTable: Int, iTile: Int, iRow: Int, flipY: Boolean = false): Int {
    val addr = (iTable * 4096) + (iTile * 16) + (if (flipY) (7 - iRow) else iRow)
    val p0 = memory.load(BASE_PATTERNS + addr)
    val p1 = memory.load(BASE_PATTERNS + addr + TILE_SIZE)
    return (p1 shl 8) or p0
  }
}
